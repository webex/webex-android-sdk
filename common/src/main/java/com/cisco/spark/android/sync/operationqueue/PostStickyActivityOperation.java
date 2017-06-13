package com.cisco.spark.android.sync.operationqueue;


import android.content.Context;
import android.support.annotation.NonNull;

import com.cisco.spark.android.R;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ImageURIObject;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.stickies.Sticky;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.Toaster;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncStateFailureReason;


/**
 * PostStickyActivityOperation
 * <p/>
 * This operation posts an ImageURI from a selected Sticker.
 */
public class PostStickyActivityOperation extends ActivityOperation {

    @Inject
    transient protected EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient Gson gson;

    @Inject
    transient Context context;

    private Sticky sticky;
    private ImageURIObject imageURIObject;

    private String successLogStatement = "Sent sticker to Conversation %s";
    private String failureLogStatement = "Failed to send sticker to Conversation %s";

    public PostStickyActivityOperation(Injector injector, String conversationId, Sticky sticky) {
        super(injector, conversationId);

        this.sticky = sticky;
        this.imageURIObject = new ImageURIObject(sticky.getLocation().toString());
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.post);

        activity.setObject(imageURIObject);

        addLocationData();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        Activity encryptedActivity = conversationProcessor.copyAndEncryptActivity(activity, keyUri);
        Response<Activity> response = postActivity(encryptedActivity);

        if (response.isSuccessful()) {
            return SyncState.SUCCEEDED;
        }

        return SyncState.READY;
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);
        if (getState() == SyncState.FAULTED && getFailureReason() != SyncStateFailureReason.DEPENDENCY) {
            Toaster.showLong(context, R.string.error_sending_sticker);
        }
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return OperationType.STICKIES;
    }

    @Override
    public boolean needsNetwork() {
        // still need to do work without network to update the UI and prompt for retries
        return getState() == SyncState.EXECUTING;
    }

    // Keep faulted operations around, the user can manually retry
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
        // same policy as Comment
        return RetryPolicy.newLimitAttemptsPolicy(2)
                .withRetryDelay(5, TimeUnit.SECONDS);
    }
}
