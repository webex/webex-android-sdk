package com.cisco.spark.android.flag;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.wdm.DeviceRegistration;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

public final class FlagOperation extends Operation {
    private final String activityId;
    private final boolean flag;
    private String flagId;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient DeviceRegistration deviceRegistration;

    @Inject
    transient ContentResolver contentResolver;

    @Inject
    transient EventBus bus;

    private MetricsData metricsData;

    public static class MetricsData {
        private String conversationId;
        private boolean self;
        private int messageAge;

        public MetricsData(String conversationId, boolean self, int messageAge) {
            this.conversationId = conversationId;
            this.self = self;
            this.messageAge = messageAge;
        }
    }

    public FlagOperation(Injector injector, String flagId) {
        super(injector);
        this.flagId = flagId;
        this.activityId = null;
        this.flag = false;
    }

    public FlagOperation(Injector injector, String activityId, boolean flag, MetricsData metricsData) {
        super(injector);
        this.activityId = activityId;
        this.metricsData = metricsData;
        this.flag = flag;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.FLAG;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        if (flag) {
            addFlagToDatabase(null, new Date());
        } else {
            if (flagId == null) {
                flagId = ConversationContentProviderQueries.getOneValue(contentResolver, ConversationContract.FlagEntry.FLAG_ID,
                        ConversationContract.FlagEntry.ACTIVITY_ID + "=?", new String[]{activityId});
            }

            deleteFlagFromDatabase();
        }
        return SyncState.READY;
    }

    private void deleteFlagFromDatabase() {
        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newDelete(ConversationContract.FlagEntry.CONTENT_URI)
                .withSelection(ConversationContract.FlagEntry.ACTIVITY_ID + "=?", new String[]{activityId})
                .build());
        batch.apply();
    }

    private void addFlagToDatabase(String flagId, Date updateTime) {
        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newInsert(ConversationContract.FlagEntry.CONTENT_URI)
                .withValue(ConversationContract.FlagEntry.FLAG_STATE.name(), flag ? 1 : 0)
                .withValue(ConversationContract.FlagEntry.ACTIVITY_ID.name(), activityId)
                .withValue(ConversationContract.FlagEntry.FLAG_ID.name(), flagId)
                .withValue(ConversationContract.FlagEntry.FLAG_UPDATE_TIME.name(), updateTime.getTime())
                .build());
        batch.apply();
    }

    private void updateFlagDatabaseEntry(String flagId, Date updateTime) {
        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newUpdate(ConversationContract.FlagEntry.CONTENT_URI)
                .withValue(ConversationContract.FlagEntry.FLAG_STATE.name(), flag ? 1 : 0)
                .withValue(ConversationContract.FlagEntry.ACTIVITY_ID.name(), activityId)
                .withValue(ConversationContract.FlagEntry.FLAG_ID.name(), flagId)
                .withValue(ConversationContract.FlagEntry.FLAG_UPDATE_TIME.name(), updateTime.getTime())
                .withSelection(ConversationContract.FlagEntry.ACTIVITY_ID + "=?", new String[]{activityId})
                .build());
        batch.apply();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Uri activityUrl = deviceRegistration.getConversationServiceUrl().buildUpon().appendPath("activities").appendPath(activityId).build();
        if (flag) {
            Response<Flag> response = apiClientProvider.getFlagClient().flag(Flag.flagRequest(activityUrl)).execute();

            if (response.isSuccessful()) {
                updateFlagDatabaseEntry(response.body().getId(), response.body().getDateUpdated());
            } else if (response.code() == 400) {
                deleteFlagFromDatabase();
                bus.post(new TooManyFlagsEvent());
                return SyncState.SUCCEEDED;
            } else if (response.code() == 429) {
                getRetryPolicy().setRetryDelay(20, TimeUnit.SECONDS);
                return SyncState.READY;
            }
        } else {
            Response<Void> response = apiClientProvider.getFlagClient().delete(flagId).execute();

            if (response.code() == 429) {
                int retrySeconds = 20;

                if (response.headers().get("Retry-After") != null) {
                    retrySeconds = Integer.valueOf(response.headers().get("Retry-After"));
                }

                getRetryPolicy().setRetryDelay(retrySeconds, TimeUnit.SECONDS);
                return SyncState.READY;
            }
        }
        return SyncState.SUCCEEDED;
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        if (getState().isTerminal() && !oldState.isTerminal()) {
            if (getState() == SyncState.FAULTED) {
                bus.post((flag) ? new FailedToFlagItem() : new FailedToRemoveFlag());
                deleteFlagFromDatabase();
            }
        }
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (newOperation instanceof FlagOperation) {
            FlagOperation newFlagOperation = (FlagOperation) newOperation;
            // a new operation on the same activity supersedes this one.
            if (activityId.equals(newFlagOperation.activityId))
                cancel();
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }

    public class TooManyFlagsEvent {
    }

    public class FailedToFlagItem {
    }

    public class FailedToRemoveFlag {

    }
}
