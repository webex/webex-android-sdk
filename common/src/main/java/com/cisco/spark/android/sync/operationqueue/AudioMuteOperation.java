package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

public class AudioMuteOperation extends Operation {

    private String roomId;
    private boolean isMuted;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient CallMetricsReporter callMetricsReporter;

    public AudioMuteOperation(Injector injector, String roomId, boolean isMuted) {
        super(injector);
        this.roomId = roomId;
        this.isMuted = isMuted;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.AUDIO_MUTE;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {

        Response<Void> response;

        long startTime = System.currentTimeMillis();

        if (isMuted) {
            response = apiClientProvider.getLyraClient().mute(roomId).execute();
        } else {
            response = apiClientProvider.getLyraClient().unMute(roomId).execute();
        }

        long durationTime = System.currentTimeMillis() - startTime;

        if (response.isSuccessful()) {
            if (isMuted) {
                callMetricsReporter.reportMuteActionMetrics(durationTime, roomId);
            } else {
                callMetricsReporter.reportUnMuteActionMetrics(durationTime, roomId);
            }
            return SyncState.SUCCEEDED;
        }

        return SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);
        if (newOperation.getOperationType() == OperationType.AUDIO_MUTE) {
            Ln.d("Canceling because a newer mute operation was just posted. " + this);
            cancel();
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withInitialDelay(200);
    }
}
