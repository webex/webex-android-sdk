package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.HashMap;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.FETCH_SPACE_URL;

public class FetchSpaceUrlOperation extends Operation implements ConversationOperation {
    private String conversationId;

    @Inject
    transient ApiClientProvider apiClientProvider;

    public FetchSpaceUrlOperation(Injector injector, String conversationId) {
        super(injector);
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.FETCH_SPACE_URL;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        if (newOp.getOperationType() != FETCH_SPACE_URL)
            return false;

        return ((FetchSpaceUrlOperation) newOp).getConversationId().equals(getConversationId());
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @Override
    public boolean needsNetwork() {
        // still need to do work without network to update the UI and prompt for retries
        return getState() == SyncState.EXECUTING;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Uri spaceUrl;
        Uri spaceUrlHidden;

        conversationId = ConversationContentProviderQueries.getOneValue(
                getContentResolver(),
                Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, conversationId),
                ConversationContract.ConversationEntry.CONVERSATION_ID.name(),
                null, null);

        Bundle bundle = ConversationContentProviderQueries.getConversationById(getContentResolver(), conversationId);
        if (bundle == null) {
            Ln.i("bundle is null for conversationId " + conversationId);
            return SyncState.READY;
        } else {
            spaceUrl = UriUtils.parseIfNotNull(bundle.getString(ConversationContract.ConversationEntry.SPACE_URL.name()));
            spaceUrlHidden = UriUtils.parseIfNotNull(bundle.getString(ConversationContract.ConversationEntry.SPACE_URL_HIDDEN.name()));
        }

        // Fetch spaces and write them to conversation for next time if needed
        Batch batch = newBatch();
        if (spaceUrl == null) {
            spaceUrl = resolveSpaceUrl(batch, ConversationContract.ConversationEntry.SPACE_URL);
        }

        if (spaceUrlHidden == null) {
            spaceUrlHidden = resolveSpaceUrl(batch, ConversationContract.ConversationEntry.SPACE_URL_HIDDEN);
        }
        batch.apply();

        return spaceUrlHidden != null && spaceUrl != null
                ? SyncState.SUCCEEDED
                : SyncState.READY;
    }

    private Uri resolveSpaceUrl(Batch batch, ConversationContract.ConversationEntry spaceUrlType) throws IOException {
        Response<HashMap<String, String>> response = null;
        if (spaceUrlType == ConversationContract.ConversationEntry.SPACE_URL_HIDDEN)
            response = apiClientProvider.getConversationClient().putConversationSpaceHidden(conversationId).execute();
        else
            response = apiClientProvider.getConversationClient().putConversationSpace(conversationId).execute();

        if (response.isSuccessful()) {
            HashMap<String, String> spaceUrlMap = response.body();
            return writeSpaceUrl(spaceUrlMap, spaceUrlType, batch);
        } else {
            Ln.w("Failed getting space url: " + LoggingUtils.toString(response));
        }
        return null;
    }

    private Uri writeSpaceUrl(HashMap<String, String> spaceUrlMap, ConversationContract.ConversationEntry col, Batch batch) {
        Uri uri = UriUtils.parseIfNotNull(spaceUrlMap.get("spaceUrl"));
        if (uri != null) {
            batch.add(
                    ContentProviderOperation.newUpdate(Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, conversationId))
                            .withValue(col.name(), uri.toString())
                            .build());
        }
        return uri;
    }
}
