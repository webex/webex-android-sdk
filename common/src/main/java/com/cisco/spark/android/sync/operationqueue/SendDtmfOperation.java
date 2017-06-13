package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;


public class SendDtmfOperation extends Operation {
    @Inject
    transient CallControlService callControlService;

    static private int correlationId = 0;

    private String tones;
    private int sequence;

    public SendDtmfOperation(Injector injector, String tones) {
        super(injector);
        this.tones = tones;
        this.sequence = correlationId++;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.SEND_DTMF;
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

        Response response = callControlService.sendDtmf(sequence, tones);

        if (response != null) {
            if (!response.isSuccessful()) {
                Ln.e(false, "Failed sendDtmf.  Response: " + response.code());
                state = SyncState.FAULTED;
            }
        } else {
            Ln.e(false, "No response from sendDtmf");
            state = SyncState.FAULTED;
        }


        return state;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.SEND_DTMF)
            return;

        SendDtmfOperation newSendDtmfOperation = (SendDtmfOperation) newOperation;

        if (newSendDtmfOperation.sequence == sequence - 1) {
            setDependsOn(newOperation);
        } else if (newSendDtmfOperation.sequence == sequence + 1) {
            newOperation.setDependsOn(this);
        }
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.SECONDS)
                .withMaxAttempts(3)
                .withRetryDelay(0);
    }

}
