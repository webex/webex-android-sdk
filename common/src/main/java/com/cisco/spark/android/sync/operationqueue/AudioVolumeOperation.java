package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.lyra.VolumeRequest;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

public class AudioVolumeOperation extends Operation {
    private String roomId;
    private int volumeLevel;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient CallMetricsReporter callMetricsReporter;

    public AudioVolumeOperation(Injector injector, String roomId, int volumeLevel) {
        super(injector);
        this.roomId = roomId;
        this.volumeLevel = volumeLevel;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.AUDIO_VOLUME;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {

        long startTime = System.currentTimeMillis();
        Response<Void> response = apiClientProvider.getLyraClient().setVolume(roomId, new VolumeRequest(volumeLevel)).execute();
        long durationTime = System.currentTimeMillis() - startTime;

        if (response.isSuccessful()) {
            callMetricsReporter.reportVolumeActionMetrics(durationTime, roomId);
            return SyncState.SUCCEEDED;
        }

        return SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);
        if (newOperation.getOperationType() == OperationType.AUDIO_VOLUME) {
            Ln.d("Canceling because a newer volume operation was just posted. " + this);
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
