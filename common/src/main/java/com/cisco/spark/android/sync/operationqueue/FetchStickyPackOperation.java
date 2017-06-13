package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.stickies.Sticky;
import com.cisco.spark.android.stickies.StickyPack;
import com.cisco.spark.android.stickies.StickyPad;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class FetchStickyPackOperation extends Operation {

    private final String logTag = "Stickies: %s";

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ContentManager contentManager;

    @Inject
    transient Settings settings;

    public FetchStickyPackOperation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.FETCH_STICKY_PACK;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        SyncState state = SyncState.SUCCEEDED;

        StickyPack currentStickyPack = apiClientProvider.getStickiesClient().getPackForUser();
        if (currentStickyPack == null || currentStickyPack.getPads() == null) {
            // Note: The Stickies service *should* never return a NULL StickyPack or a pack with a NULL StickyPad list. If no Stickies are available, the list should be empty.
            Ln.e(String.format(logTag, "Received a NULL StickyPack or a StickyPack with a NULL StickyPad list."));
            state = SyncState.READY;
        } else {
            updateDatabase(currentStickyPack);
            fetchStickyImages(currentStickyPack);
            settings.setHasLoadedStickyPack(true);
        }

        return state;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == OperationType.FETCH_STICKY_PACK;
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.MINUTES)
                .withExponentialBackoff();
    }

    private void updateDatabase(StickyPack currentStickyPack) {
        Batch batch = super.newBatch();

        // Delete everything from the DB and write new data. Currently there is not much data in a StickyPack.
        deleteAllStickyPads(batch);

        // Now add new sticky pads.
        if (currentStickyPack.getPads().size() == 0) {
            Ln.i(String.format(logTag, "Received empty StickyPad. User has no stickers available to them."));
        } else {
            for (StickyPad pad : currentStickyPack.getPads()) {
                addStickyPad(pad, batch);
            }
        }

        if (batch.size() == 0) {
            Ln.d(String.format(logTag, "No changes to existing stickers found."));
        } else {
            batch.apply();
        }
    }

    private void deleteAllStickyPads(Batch batch) {
        Ln.d(String.format(logTag, "Deleting all existing StickyPads."));
        batch.add(ConversationContentProviderOperation.deleteAllStickyPads());
    }

    private void addStickyPad(StickyPad pad, Batch batch) {
        Ln.d(String.format(logTag, "Adding StickyPad " + pad.toString()));
        for (Sticky sticky : pad.getStickies()) {
            batch.add(ConversationContentProviderOperation.insertSticky(pad.getId().toString(), pad.getDescription(), sticky.getId(), sticky.getLocation().toString(), sticky.getDescription()));
        }
    }

    private void fetchStickyImages(StickyPack currentStickyPack) {
        for (StickyPad pad : currentStickyPack.getPads()) {
            for (Sticky sticky : pad.getStickies()) {
                Uri stickyUri = Uri.parse(sticky.getLocation().toString());
                String filename = stickyUri.getLastPathSegment();
                contentManager.getCacheRecord(
                        ConversationContract.ContentDataCacheEntry.Cache.IMAGEURI,
                        stickyUri,
                        null,
                        filename,
                        null);
            }
        }
    }
}
