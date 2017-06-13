package com.cisco.spark.android.presence.operation;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.presence.PresenceStatusList;
import com.cisco.spark.android.presence.PresenceStatusRequest;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

public class FetchPresenceStatusOperation extends Operation {
    private static final int MAX_SUBJECTS = 10;
    private PresenceStatusRequest request;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient EventBus bus;

    public FetchPresenceStatusOperation(Injector injector, String subjectUuid) {
        this(injector, Arrays.asList(subjectUuid));
    }

    public FetchPresenceStatusOperation(Injector injector, List<String> subjects) {
        super(injector);
        request = new PresenceStatusRequest(subjects);
    }


    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.FETCH_USER_PRESENCE;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withInitialDelay(200);
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Response<PresenceStatusList> response = apiClientProvider.getPresenceClient().getUserCompositions(request).execute();

        if (response.isSuccessful()) {
            bus.post(response.body());
            return SyncState.SUCCEEDED;
        }
        return SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.FETCH_USER_PRESENCE) {
            return false;
        }

        FetchPresenceStatusOperation presenceStatusOperation = (FetchPresenceStatusOperation) newOperation;

        if (getState().isPreExecute()) {
            for (String subject : presenceStatusOperation.request.getSubjects()) {
                this.request.removeSubject(subject);
                this.request.addSubject(subject);

                if (this.request.subjectCount() > MAX_SUBJECTS) {
                    this.request.removeFirstSubject();
                }
            }
        }

        return getState().isPreExecute();
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        // The current one is still executing, put the new one in line behind this one and let it
        // accumulate work while we wrap this one up
        if (newOperation.getOperationType() == getOperationType()) {
            newOperation.setDependsOn(this);
        }
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    private Date getExpireTime(int ttlInSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, ttlInSeconds);
        return calendar.getTime();
    }
}
