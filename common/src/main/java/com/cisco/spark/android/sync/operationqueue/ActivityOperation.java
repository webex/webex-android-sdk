package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Place;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.util.LocationManager;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.TestUtils;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.SYNC_OPERATION_ID;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.DELETE_ACTIVITY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.REMOVE_PARTICIPANT;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.UPDATE_ENCRYPTION_KEY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;


/**
 * Handy base class for operations that send activities, since much of the logic is the same across
 * different activity types.
 */
public abstract class ActivityOperation extends Operation implements ConversationOperation {

    @Inject
    transient protected ApiTokenProvider apiTokenProvider;

    @Inject
    transient protected ApiClientProvider apiClientProvider;

    @Inject
    transient protected LocationManager locationManager;

    @Inject
    transient protected MetricsReporter metricsReporter;

    @Inject
    transient protected EncryptionDurationMetricManager encryptionDurationMetricManager;

    @Inject
    transient protected ConversationSyncQueue conversationSyncQueue;

    @Inject
    transient protected Gson gson;

    @Inject
    transient protected KeyManager keyManager;

    @Inject
    transient protected OperationQueue operationQueue;

    @Inject
    transient protected Context context;

    @Inject
    transient protected ActorRecordProvider actorRecordProvider;

    /**
     * The activity being sent
     */
    protected Activity activity;

    /**
     * The conversation id of the activity's target
     */
    protected String conversationId;

    /**
     * The Uri of the activity's encryption key, if any
     */
    protected Uri keyUri;

    /**
     * The activity returned from the server
     */
    private Activity result;

    ActivityOperation(Injector injector, String conversationId) {
        super(injector);
        this.conversationId = conversationId;
    }

    /**
     * Default implementation of onEnqueue for activity operations. <p/> This implementation takes
     * care of building the activity by calling the extending class' {@link
     * ActivityOperation#configureActivity() configureActivity} method, logging, and writing it to
     * the database synchronously.
     *
     * @return {@link com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState#READY
     * READY}
     */
    @NonNull
    @Override
    protected SyncState onEnqueue() {
        configureActivity();

        if (getState().isTerminal()) {
            return getState();
        }

        // Instantiate a task here and do the DB work synchronously on this thread.
        conversationSyncQueue.getNewActivityTask(activity).execute();

        return SyncState.READY;
    }

    @NonNull
    @Override
    @CallSuper
    public SyncState onPrepare() {
        SyncState ret = super.onPrepare();

        if (ret != SyncState.READY)
            return ret;

        // for the 'Leave' case. The conversation has been legitimately removed from the database
        if (getOperationType() == REMOVE_PARTICIPANT) {
            return SyncState.READY;
        }

        Bundle convValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), getConversationId());

        //TODO to keep testing fair this should only happen for _headless_ test users because they have no database.
        if (convValues == null && TestUtils.isInstrumentation()) {
            return SyncState.READY;
        }

        if (convValues == null) {
            Ln.i("Conversation " + getConversationId() + " does not exist locally yet");
            return SyncState.PREPARING;
        }

        String newConversationId = convValues.getString(ConversationContract.ConversationEntry.CONVERSATION_ID.name());

        // This happens if the activity's target was a provisional conversation. Get the 'real' conversation id from the db.
        if (!TextUtils.isEmpty(newConversationId) && !TextUtils.equals(conversationId, newConversationId)) {
            Ln.d("updating conversation id for activity " + activity + ": " + conversationId + " => " + newConversationId);
            conversationId = newConversationId;
            activity.setTarget(new Conversation(conversationId));
        }

        if (activity.getType().isEncryptable()) {
            keyUri = UriUtils.parseIfNotNull(convValues.getString(DEFAULT_ENCRYPTION_KEY_URL.name()));
            boolean isProvisional = TextUtils.equals(newConversationId, convValues.getString(SYNC_OPERATION_ID.name()));

            if (keyUri == null && !isProvisional && getOperationType() != UPDATE_ENCRYPTION_KEY) {
                Ln.i("Not ready, conversation has a blank key uri.");
                Operation updateKeyOperation = operationQueue.updateConversationKey(conversationId);
                setDependsOn(updateKeyOperation);
                ret = SyncState.PREPARING;
            } else if (keyUri != null) {
                activity.setEncryptionKeyUrl(keyUri);

                // If we have a keyUri, make sure we have the key in the cache
                KeyObject keyObject = keyManager.getBoundKey(keyUri);
                if (keyObject == null && keyUri != null) {
                    setDependsOn(operationQueue.requestKey(keyUri));
                    ret = SyncState.PREPARING;
                }
            }
        }

        return ret;
    }

    /**
     * Default implementation of doWork for activity operations.
     *
     * This implementation takes care of making sure the operation is in a valid state and updates
     * the conversation id from the database if this operation's conversation id is provisional. The
     * activity's key uri will be set based on the conversation id.
     *
     * @return One of {@link com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState
     * SyncState}
     */
    @NonNull
    @Override
    @CallSuper
    protected SyncState doWork() throws IOException {
        Bundle convValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), getConversationId());

        if (convValues != null) {
            conversationId = convValues.getString(ConversationContract.ConversationEntry.CONVERSATION_ID.name());
        }

        if (activity.getType().isEncryptable()) {
            keyUri = (convValues == null) ? null : UriUtils.parseIfNotNull(convValues.getString(DEFAULT_ENCRYPTION_KEY_URL.name()));

            // This can happen with headless test users who have no DB
            if (keyUri == null) {
                Response<Conversation> response = apiClientProvider.getConversationClient().getConversation(getConversationId()).execute();
                if (response.isSuccessful()) {
                    keyUri = response.body().getDefaultActivityEncryptionKeyUrl();
                } else {
                    Ln.w("Failed getting conversation from service: " + LoggingUtils.toString(response));
                }
            }

            if (keyUri == null) {
                throw new NotReadyException("Unable to encrypt because we have no key uri");
            }

            if (keyManager.getBoundKey(keyUri) == null) {
                return SyncState.READY;
            }
        }

        return getState();
    }

    @Override
    @CallSuper
    protected void onStateChanged(SyncState oldState) {
        if (getState() == SyncState.FAULTED) {
            // The LOCAL activity will already have been added to the activity stream. In this case
            // we need to back it out so the local representation is accurate.
            rollbackActivityEntry();
        }

        // Update the activity record in the DB. Normally we're not on the main thread but there
        // are edge cases so we handle those with the async task to avoid hitting the db on main.
        if (Looper.getMainLooper() != Looper.myLooper())
            updateActivityEntry(getState());
        else {
            new SafeAsyncTask<Void>() {
                @Override
                public Void call() throws Exception {
                    updateActivityEntry(getState());
                    return null;
                }
            }.execute();
        }
    }

    /**
     * postActivity/postContent <p/> NOTE: The activity needs to be sent with a null publish date.
     * If its not sent with a null date, the server will use the time from the device. <p/> See
     * Also: https://sqbu-github.cisco.com/WebExSquared/cloud-apps/issues/124
     */
    protected Response<Activity> postActivity(Activity activity) throws IOException {
        activity.setPublished(null);
        Response<Activity> response = apiClientProvider.getConversationClient().postActivity(activity).execute();
        if (response.isSuccessful()) {
            result = response.body();
        } else {
            Ln.w("Failed posting activity: " + LoggingUtils.toString(response));
        }
        return response;
    }

    protected Response<Activity> postContent(Activity activity, boolean transcode) throws IOException {
        activity.setPublished(null);
        Response<Activity> response = apiClientProvider.getConversationClient().postContent(activity, transcode).execute();
        if (response.isSuccessful()) {
            result = response.body();
        } else {
            Ln.w("Failed posting activity: " + LoggingUtils.toString(response));
        }
        return response;
    }

    /**
     * Abstract method, called as part of {@link ActivityOperation#onEnqueue() onEnqueue} for
     * Activity operations. The implementation should call {@link com.cisco.spark.android.sync.operationqueue.ActivityOperation#configureActivity(String)
     * configureActivity(String verb)} to set up a boilerplate activity with a type, an ID, an
     * Actor, publish time, source, and target.  In most cases the implementing class just has to
     * set the activity's Object.
     */
    protected abstract void configureActivity();

    /**
     * Build a boilerplate Activity with the provided verb. The ActivityObject is not set. See
     * {@link ActivityOperation#configureActivity()}
     *
     * @param verb The activity's verb
     */
    protected void configureActivity(String verb) {
        // Create the activity for display with a temporary ID
        activity = new Activity(verb);
        activity.setId(getOperationId());
        activity.setClientTempId(getOperationId());
        activity.setActor(new Person(apiTokenProvider.getAuthenticatedUser()));
        activity.setSource(ActivityEntry.Source.LOCAL);

        Conversation conversation = new Conversation(conversationId);

        // Set team tags if it is a team conversation
        String primaryTeamConvId = ConversationContentProviderQueries.getTeamPrimaryConversationId(context.getContentResolver(), conversationId);
        if (!TextUtils.isEmpty(primaryTeamConvId)) {
            if (primaryTeamConvId.equals(conversationId)) {
                conversation.setIsTeam(true);
            } else {
                conversation.setIsOpen(true);
            }
        }

        activity.setTarget(conversation);

        // Make sure the published time is the last one to avoid sorting issues.
        // This will be changed to the correct time when it comes back from the server
        long lastActivityPublishedTime = ConversationContentProviderQueries.getLastActivityPublished(getContentResolver(), conversationId);
        activity.setPublished(new Date(Math.max(System.currentTimeMillis(), lastActivityPublishedTime + 100)));
    }

    /**
     * Most activity operations do their work in doWork(); for them the default impl here is fine.
     * Activity operations that do work after doWork returns should override.
     *
     * @return One of {@link com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState
     * SyncState}
     */
    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    /**
     * This base class includes a conversationId member for convenience.
     *
     * @return The conversation ID, which may be provisional if the conversation is not created yet.
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Add location data to the activity if the location setting is enabled.
     */
    protected void addLocationData() {
        if (locationManager != null && locationManager.isEnabled()) {
            Place location = new Place(locationManager.getCoarseLocationName(), locationManager.getCoarseLocationISO6709Position());
            activity.setLocation(location);
        }
    }

    /**
     * Normally when an Activity fails we want to remove it from the stream
     */
    protected void rollbackActivityEntry() {
        // Message and Sticker activities aren't deleted on fail because the user has an opportunity to retry
        if (!isSafeToRemove())
            return;

        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                if (isSafeToRemove()) {
                    Batch batch = newBatch();
                    batch.add(ContentProviderOperation.newDelete(ActivityEntry.CONTENT_URI)
                            .withSelection(ActivityEntry.SYNC_OPERATION_ID + "=?",
                                    new String[]{getOperationId()})
                            .build());
                    batch.apply();
                }
                return null;
            }
        }.execute();
    }

    // If any activities have this operation's id, update its state for the ui.
    protected void updateActivityEntry(SyncState state) {
        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                .withValue(ActivityEntry.SYNC_STATE.name(), state.ordinal())
                .withSelection(ActivityEntry.SYNC_OPERATION_ID + "=?", new String[]{String.valueOf(getOperationId())})
                .build());
        batch.apply();
    }

    protected ActorRecord.ActorKey validateActorKey(ActorRecord.ActorKey actorKey) {
        String userUuid = actorKey.getUuid();

        if (Strings.isEmailAddress(userUuid)) {
            ActorRecord actorRecord = actorRecordProvider.get(userUuid);
            if (actorRecord != null && actorRecord.getKey() != null && !TextUtils.isEmpty(actorRecord.getKey().getUuid()))
                userUuid = actorRecord.getKey().getUuid();
        }

        if (Strings.isEmailAddress(userUuid)) {
            try {
                Response<User> resp = apiClientProvider.getUserClient().getUserID(apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), userUuid).execute();
                if (resp.isSuccessful() && resp.body() != null)
                    userUuid = resp.body().getUuid();
            } catch (IOException e) {
                Ln.w(e, "Failure validating actorKey");
            }
        }

        return new ActorRecord.ActorKey(userUuid);
    }

    public Activity getResult() {
        return result;
    }

    @CallSuper
    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        // If a delete activity operation comes in for the activity we are working on...
        if (newOperation.getOperationType() == DELETE_ACTIVITY && getOperationType() != DELETE_ACTIVITY) {
            DeleteActivityOperation deleteOp = (DeleteActivityOperation) newOperation;
            if (TextUtils.equals(getOperationId(), deleteOp.getActivityToDelete())) {
                if (getState().isPreExecute()) {
                    // We haven't sent the activity. Just cancel both operations
                    cancel();
                    deleteOp.cancel();
                } else {
                    // Make sure the delete doesn't run until after we finish
                    deleteOp.setDependsOn(this);
                }
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + (getConversationId() != null ? " room:" + getConversationId() : "");
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(5, TimeUnit.MINUTES)
                .withRetryDelay(1, TimeUnit.SECONDS);
    }
}
