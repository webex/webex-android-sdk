package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationRecord;
import com.cisco.spark.android.sync.TitleBuilder;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.FETCH_UNJOINED_TEAMS;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class FetchUnjoinedTeamRoomsOperation extends Operation {

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient Provider<Batch> batchProvider;

    @Inject
    transient TitleBuilder titleBuilder;

    @Inject
    transient Gson gson;

    @Inject
    transient KeyManager keyManager;

    @Inject
    transient OperationQueue operationQueue;

    protected final HashSet<Uri> keysToFetch = new HashSet<>();

    public FetchUnjoinedTeamRoomsOperation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return FETCH_UNJOINED_TEAMS;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        Response<ItemCollection<Team>> response = apiClientProvider.getConversationClient().getTeamsWithUnjoinedTeamConversationsSince(1).execute();
        if (!response.isSuccessful()) {
            Ln.w("Failed getting unjoined team rooms: " + LoggingUtils.toString(response));
            return SyncState.READY;
        }

        List<Team> teams = response.body().getItems();

        Batch batch = batchProvider.get();

        for (Team team : teams) {
            List<Conversation> unjoinedConversations = team.getConversations().getItems();
            for (Conversation conversation : unjoinedConversations) {
                ConversationRecord cr = ConversationRecord.buildFromConversation(gson, conversation, apiTokenProvider.getAuthenticatedUser(), titleBuilder);
                if (cr.getTitleKeyUrl() != null) {
                    KeyObject key = keyManager.getBoundKey(cr.getTitleKeyUrl());
                    if (key != null && !TextUtils.isEmpty(key.getKey())) {
                        try {
                            cr.decryptTitleAndSummary(new KeyObject(cr.getTitleKeyUrl(), key.getKey(), key.getKeyId()));
                            cr.setAreTitleAndSummaryEncrypted(false);
                        } catch (IOException | ParseException e) {
                            Ln.d(e, "Failed decrypting title and/or summary");
                        }
                    } else {
                        //TODO do we need keysToFetch anymore?
                        keysToFetch.add(cr.getTitleKeyUrl());
                        cr.setAreTitleAndSummaryEncrypted(true);
                    }
                }

                if (cr.getAvatarEncryptionKeyUrl() != null) {
                    KeyObject key = keyManager.getBoundKey(cr.getAvatarEncryptionKeyUrl());
                    if (key != null && !TextUtils.isEmpty(key.getKey())) {
                        try {
                            cr.decryptAvatarScr(new KeyObject(cr.getAvatarEncryptionKeyUrl(), key.getKey(), key.getKeyId()));
                            cr.setIsAvatarEncrypted(false);
                        } catch (IOException | ParseException | JOSEException e) {
                            Ln.d(e, "Failed decrypting conversation avatar");
                        }
                    } else {
                        keysToFetch.add(cr.getAvatarEncryptionKeyUrl());
                        cr.setIsAvatarEncrypted(true);
                    }
                }

                if (cr.getDefaultEncryptionKeyUrl() != null && keyManager.getBoundKey(cr.getDefaultEncryptionKeyUrl()) == null) {
                    keysToFetch.add(cr.getDefaultEncryptionKeyUrl());
                }

                // Make sure the conversation is there. Fail duplicate insert silently.
                if (!cr.isBuiltFromCursor())
                    batch.add(cr.getInsertOperation().build());
                cr.addUpdateOperations(batch, true);
            }
        }

        if (!keysToFetch.isEmpty())
            operationQueue.requestKeys(keysToFetch);

        batch.apply();

        return SyncState.SUCCEEDED;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        //isOperationRedundant handles dupes, so we don't have to worry about this
    }

    @Override
    protected ConversationContract.SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withExponentialBackoff();
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == FETCH_UNJOINED_TEAMS;
    }
}
