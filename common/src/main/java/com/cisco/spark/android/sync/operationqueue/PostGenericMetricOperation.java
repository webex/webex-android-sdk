package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.model.GenericMetricsRequest;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncStateFailureReason;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.TestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class PostGenericMetricOperation extends Operation {
    private static final int MAX_METRICS = 100;

    private GenericMetricsRequest metricsRequest;
    private boolean preLogin;
    private boolean isDiagnosticEvent;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient LocusDataCache locusDataCache;

    public PostGenericMetricOperation(Injector injector, GenericMetric metric) {
        this(injector, metric, false);
    }

    public PostGenericMetricOperation(Injector injector, GenericMetric metric, boolean preLogin) {
        super(injector);

        this.metricsRequest = new GenericMetricsRequest();
        this.metricsRequest.addMetric(metric);
        this.isDiagnosticEvent = metric.isDiagnosticEvent();

        this.preLogin = preLogin;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        if (TestUtils.isInstrumentation()) {
            return RetryPolicy.newLimitAttemptsPolicy()
                    .withInitialDelay(TimeUnit.SECONDS.toMillis(1));
        } else if (preLogin) {
            return RetryPolicy.newLimitAttemptsPolicy()
                    .withInitialDelay(TimeUnit.SECONDS.toMillis(5));
        } else {
            return RetryPolicy.newLimitAttemptsPolicy()
                    .withInitialDelay(TimeUnit.MINUTES.toMillis(1));
        }
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.POST_GENERIC_METRIC;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != getOperationType()) {
            return false;
        }

        if (!preLogin) {
            PostGenericMetricOperation metricOperation = (PostGenericMetricOperation) newOperation;
            if (this.isDiagnosticEvent == metricOperation.isDiagnosticEvent) {
                if (getState().isPreExecute()) {
                    this.metricsRequest.combine(metricOperation.getMetricsRequest());
                }

                return getState().isPreExecute();
            }
        }

        return false;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        // The current one is still executing, put the new one in line behind this one and let it
        // accumulate work while we wrap this one up
        if (newOperation.getOperationType() == getOperationType() && (getState() != SyncState.EXECUTING || getState() != SyncState.FAULTED)) {
            newOperation.setDependsOn(this);
        }
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        if (locusDataCache.isInCall() && !this.isDiagnosticEvent) {
            return SyncState.PREPARING; // Wait until the user ends the call or the sync finishes.
        }
        return super.onPrepare();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Response<Void> response = null;

        if (metricsRequest.metricsSize() <= MAX_METRICS) {
            response = postMetrics(metricsRequest);
        } else {
            LinkedBlockingQueue<GenericMetric> allMetrics = new LinkedBlockingQueue<>(metricsRequest.getMetrics());
            while (allMetrics.size() > 0) {
                ArrayList<GenericMetric> metrics = new ArrayList<>();
                allMetrics.drainTo(metrics, 100);
                GenericMetricsRequest subRequest = new GenericMetricsRequest();
                subRequest.addAllMetrics(metrics);
                response = postMetrics(subRequest);

                if (!response.isSuccessful()) {
                    break;
                }
            }
        }

        if (response.isSuccessful()) {
            return SyncState.SUCCEEDED;
        }

        if (response.code() >= 500) {
            return SyncState.READY; // Server issue retry again.
        } else {
            setFailureReason(SyncStateFailureReason.INVALID_RESPONSE);
            setErrorMessage("MetricsClient returned: " + response.message());
            return SyncState.FAULTED;
        }
    }

    protected Response postMetrics(GenericMetricsRequest request) throws IOException {
        // Inform metrics about to be sent that they are to be sent now - may need to know 'sent time'
        for (GenericMetric metric : request.getMetrics()) {
            metric.onInstantBeforeSend();
        }

        if (preLogin) {
            return apiClientProvider.getMetricsPreloginClient().postClientMetric(request).execute();
        } else {
            return apiClientProvider.getMetricsClient().postClientMetric(request).execute();
        }
    }

    public GenericMetricsRequest getMetricsRequest() {
        return metricsRequest;
    }

    @Override
    protected String getOperationInfo() {
        String info =  super.getOperationInfo();

        return info + " " + metricsRequest.getMetrics();
    }

    @Override
    public boolean requiresAuth() {
        return !preLogin;
    }
}
