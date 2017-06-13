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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class PostGenericMetricOperation extends Operation {
    private static final int MAX_METRICS = 100;
    private GenericMetricsRequest metricsRequest;
    private boolean prelogin;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient LocusDataCache locusDataCache;

    public PostGenericMetricOperation(Injector injector, GenericMetric metric) {
        super(injector);
        this.prelogin = false;
        this.metricsRequest = new GenericMetricsRequest();

        this.metricsRequest.addMetric(metric);
    }

    public PostGenericMetricOperation(Injector injector, GenericMetric metric, boolean prelogin) {
        this(injector, metric);
        this.prelogin = prelogin;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        if (TestUtils.isInstrumentation()) {
            return RetryPolicy.newLimitAttemptsPolicy()
                    .withInitialDelay(TimeUnit.SECONDS.toMillis(1));
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

        PostGenericMetricOperation metricOperation = (PostGenericMetricOperation) newOperation;

        if (getState().isPreExecute()) {
            this.metricsRequest.combine(metricOperation.getMetricsRequest());
        }

        return getState().isPreExecute();
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
        if (locusDataCache.isInCall()) {
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
            while (metricsRequest.metricsSize() > 0) {
                List<GenericMetric> metrics = metricsRequest.getMetrics().subList(0, MAX_METRICS - 1);
                GenericMetricsRequest subRequest = new GenericMetricsRequest();
                subRequest.addAllMetrics(metrics);

                response = postMetrics(subRequest);

                if (!response.isSuccessful()) {
                    break;
                }

                metrics.clear(); // This removes them from the main list as well.
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
        return apiClientProvider.getMetricsClient().postClientMetric(request).execute();
    }

    public GenericMetricsRequest getMetricsRequest() {
        return this.metricsRequest;
    }

    @Override
    protected String getOperationInfo() {
        String info =  super.getOperationInfo();

        return info + " " + metricsRequest.getMetrics();
    }
}
