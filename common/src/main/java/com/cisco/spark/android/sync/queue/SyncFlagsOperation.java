package com.cisco.spark.android.sync.queue;


import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.flag.Flag;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.BulkActivitiesRequest;
import com.cisco.spark.android.model.BulkActivitiesResponse;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.MultistatusResponse;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.FlagEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;

public class SyncFlagsOperation extends Operation {

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ContentResolver contentResolver;

    @Inject
    transient ConversationSyncQueue conversationSyncQueue;

    @Inject
    transient DeviceRegistration deviceRegistration;

    public SyncFlagsOperation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    public SyncOperationEntry.OperationType getOperationType() {
        return SyncOperationEntry.OperationType.SYNC_FLAGs;
    }

    @NonNull
    @Override
    protected SyncOperationEntry.SyncState onEnqueue() {
        return SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncOperationEntry.SyncState doWork() throws IOException {
        Response<ItemCollection<Flag>> flagsResponse = apiClientProvider.getFlagClient().getFlags().execute();

        if (flagsResponse.isSuccessful()) {
            ItemCollection<Flag> flags = flagsResponse.body();
            Map<String, Flag> flagsMap = new HashMap<>(flags.size());
            Batch batch = newBatch();
            batch.add(ContentProviderOperation.newDelete(FlagEntry.CONTENT_URI).build());
            for (Flag flag : flags.getItems()) {
                batch.add(ContentProviderOperation.newInsert(FlagEntry.CONTENT_URI)
                        .withValue(FlagEntry.FLAG_STATE.name(), 1)
                        .withValue(FlagEntry.ACTIVITY_ID.name(), flag.getActivityId())
                        .withValue(FlagEntry.FLAG_ID.name(), flag.getId())
                        .withValue(FlagEntry.FLAG_UPDATE_TIME.name(), flag.getDateUpdated().getTime())
                        .build());
                flagsMap.put(flag.getActivityId(), flag);
            }
            batch.apply();

            List<Uri> missingActivityUrls = new ArrayList<>();
            Cursor cursor = contentResolver.query(FlagEntry.CONTENT_URI, new String[]{FlagEntry.ACTIVITY_ID.name()}, "ACTIVITY_ID NOT IN (SELECT ACTIVITY_ID FROM ActivityEntry)", null, null, null);
            try {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        Uri activityUrl = deviceRegistration.getConversationServiceUrl().buildUpon().appendPath("activities").appendPath(cursor.getString(0)).build();
                        missingActivityUrls.add(activityUrl);
                    }
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            if (!missingActivityUrls.isEmpty()) {
                Response<BulkActivitiesResponse> bulkActivitiesResponse = apiClientProvider.getConversationClient().getActivities(new BulkActivitiesRequest(missingActivityUrls)).execute();
                if (bulkActivitiesResponse.isSuccessful()) {
                    List<Activity> activitiesToSync = new ArrayList<>();
                    List<MultistatusResponse> activities = bulkActivitiesResponse.body().getResponses();

                    for (MultistatusResponse response : activities) {
                        if (response.getStatus() >= 400 && response.getStatus() < 500) {
                            try {
                                apiClientProvider.getFlagClient().delete(flagsMap.get(response.getHref()).getId());
                            } catch (Exception ex) {
                                Ln.e(false, ex);
                            }
                        } else {
                            if (response.getData() != null) {
                                activitiesToSync.add(response.getData().getActivity());
                            }
                        }
                    }

                    if (activitiesToSync.size() > 0) {
                        new BulkActivitySyncTask(injector, activitiesToSync).execute();
                    }
                } else {
                    Ln.w("Failed bulk activities request: " + LoggingUtils.toString(bulkActivitiesResponse));
                }
            }
        } else if (flagsResponse.code() == 400) {
            return SyncOperationEntry.SyncState.FAULTED;
        } else if (flagsResponse.code() == 429) {
            int retrySeconds = 20;

            if (flagsResponse.headers().get("Retry-After") != null) {
                retrySeconds = Integer.valueOf(flagsResponse.headers().get("Retry-After"));
            }

            getRetryPolicy().setRetryDelay(retrySeconds, TimeUnit.SECONDS);
            return SyncOperationEntry.SyncState.READY;
        }

        return SyncOperationEntry.SyncState.SUCCEEDED;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation instanceof SyncFlagsOperation;
    }

    @Override
    protected SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }
}
