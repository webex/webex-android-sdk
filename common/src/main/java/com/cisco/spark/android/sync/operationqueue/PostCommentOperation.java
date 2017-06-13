package com.cisco.spark.android.sync.operationqueue;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cisco.spark.android.R;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.TimingProvider;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.model.MetricNames;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Comment;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.presence.PostMessagePresenceEvent;
import com.cisco.spark.android.presence.PresenceStatus;
import com.cisco.spark.android.presence.PresenceStatusList;
import com.cisco.spark.android.presence.PresenceStatusRequest;
import com.cisco.spark.android.presence.PresenceUtils;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.Toaster;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation that represents a text message (not content) being posted
 */
public class PostCommentOperation extends ActivityOperation {

    @Inject
    transient protected EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient protected Gson gson;

    @Inject
    transient Context context;

    @Inject
    transient DeviceRegistration deviceRegistration;

    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;

    @Inject
    transient TimingProvider timingProvider;

    @Inject
    transient EventBus bus;

    protected Comment comment;
    protected ActorRecord oneOneParticipant;
    protected String selfPresence;
    protected String otherPresence;
    transient private GenericMetric metric;

    public PostCommentOperation(Injector injector, String conversationId, Comment comment) {
        super(injector, conversationId);
        this.comment = comment;
        this.metric = new GenericMetric(MetricNames.CLIENT_POST_COMMENT);
    }

    @Override
    public void initialize(Injector injector) {
        super.initialize(injector);
        this.metric = new GenericMetric(MetricNames.CLIENT_POST_COMMENT);
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.post);

        activity.setObject(comment);

        addLocationData();
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        SyncState ret = super.onEnqueue();

        Bundle convValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), getConversationId());
        long selfPresenceIndex = ConversationContentProviderQueries.getOneLongValue(getContentResolver(), ConversationContract.ActorEntry.CONTENT_URI, ConversationContract.ActorEntry.PRESENCE_STATUS.name(), ConversationContract.ActorEntry.ACTOR_UUID + "=?", new String[] { authenticatedUserProvider.getAuthenticatedUser().getUserId() });
        selfPresence = PresenceStatus.values()[(int) selfPresenceIndex].toString();
        otherPresence = null;

        if (convValues != null) {
            String lookup = convValues.getString(ConversationContract.ConversationEntry.ONE_ON_ONE_PARTICIPANT.name(), null);

            if (lookup != null) {
                oneOneParticipant = actorRecordProvider.get(lookup);
                if (oneOneParticipant != null)
                    otherPresence = (oneOneParticipant.getPresenceStatus() != null) ? oneOneParticipant.getPresenceStatus().toString() : "unknown";
            }
        } else {
            oneOneParticipant = null;
        }

        return ret;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        Activity encryptedActivity = conversationProcessor.copyAndEncryptActivity(activity, keyUri);
        Response<Activity> response = postActivity(encryptedActivity);

        if (response.isSuccessful()) {
            if (PresenceUtils.shouldDisplayPresence(deviceRegistration)) {
                metric.addTag(ClientMetricTag.METRIC_TAG_SELF_PRESENCE, selfPresence);

                if (otherPresence != null) {
                    metric.addTag(ClientMetricTag.METRIC_TAG_REMOTE_PRESENCE, otherPresence);
                }
            }

            return SyncState.SUCCEEDED;
        }

        return SyncState.READY;
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);

        if (getState() == SyncState.FAULTED) {
            timingProvider.remove(activity.getId());
            switch (getFailureReason()) {
                case DEPENDENCY:
                    break;
                case CANCELED:
                    Toaster.showLong(context, R.string.error_message_canceled);
                    break;
                default:
                    Toaster.showLong(context, R.string.error_sending_message);
            }
        } else if (getState() == SyncState.SUCCEEDED && (comment.getMentions() != null && comment.getMentions().size() > 0)) {
            if (PresenceUtils.shouldDisplayPresence(deviceRegistration)) {
                List<String> subjectsUuids = new ArrayList<>();

                for (Person person : comment.getMentions().getItems()) {
                    subjectsUuids.add(person.getUuid());
                }

                try {
                    Response<PresenceStatusList> response = apiClientProvider.getPresenceClient().getUserCompositions(new PresenceStatusRequest(subjectsUuids)).execute();

                    if (response.isSuccessful()) {
                        bus.post(new PostMessagePresenceEvent(response.body()));
                    }
                } catch (IOException ex) {
                    Ln.w(ex, "Error fetching Mentions Presence");
                }
            }
        } else if (getState() == SyncState.SUCCEEDED && oneOneParticipant != null) {
            if (PresenceUtils.shouldDisplayPresence(deviceRegistration)) {
                Ln.d("Fetching 1:1 Presence");

                try {
                    Response<PresenceStatusList> response = apiClientProvider
                            .getPresenceClient()
                            .getUserCompositions(new PresenceStatusRequest(Arrays.asList(oneOneParticipant.getUuidOrEmail()))).execute();

                    if (response.isSuccessful()) {
                        bus.post(new PostMessagePresenceEvent(response.body()));
                    }
                } catch (IOException ex) {
                    Ln.w(ex, "Error fetching Mentions Presence");
                }
            }
        }

        if (getState().isTerminal()) {
            metric.addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, getState() == SyncState.FAULTED ? false : true);

            if (timingProvider.contains(activity.getId())) {
                if (getState() == SyncState.SUCCEEDED) {
                    long perceivedSendDuration = timingProvider.remove(activity.getId()).finish();
                    metric.addField(ClientMetricField.METRIC_FIELD_PERCIEVED_DURATION, perceivedSendDuration);
                }
            }

            operationQueue.postGenericMetric(metric);
        }
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.MESSAGE;
    }

    @Override
    public boolean needsNetwork() {
        // still need to do work without network to update the UI and prompt for retries
        return getState() == SyncState.EXECUTING;
    }

    @Override
    public boolean isSafeToRemove() {
        if (!super.isSafeToRemove())
            return false;

        // Keep faulted operations around unless they are canceled, the user can manually retry
        return (isCanceled() || isSucceeded());
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(2)
                .withRetryDelay(5, TimeUnit.SECONDS);
    }
}
