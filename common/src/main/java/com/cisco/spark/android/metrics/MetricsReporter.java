package com.cisco.spark.android.metrics;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.reachability.NetworkReachabilityChangedEvent;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MetricsReporter implements Component {
    public static final int NO_EVENT = -1;

    private static final int MAX_METRICS_PER_REPORT = 1000;

    protected final Queue<MetricsReportRequest> splunkQueue;
    private ApiClientProvider clientProvider;
    private final LocusDataCache locusDataCache;
    private boolean isNetworkConnected;
    private Timer jobReporter;
    private long timerPeriod;

    private final MetricsEnvironment env;

    private final Object sync = new Object();
    private boolean started;

    public MetricsReporter(ApiClientProvider clientProvider, EventBus bus, MetricsEnvironment env, LocusDataCache locusDataCache) {
        this(clientProvider, bus, env, 60, locusDataCache);
    }

    public MetricsReporter(ApiClientProvider clientProvider, EventBus bus, MetricsEnvironment env, long reportDelay, LocusDataCache locusDataCache) {
        this.clientProvider = clientProvider;
        this.locusDataCache = locusDataCache;
        this.isNetworkConnected = true;
        this.env = env;

        bus.register(this);

        this.timerPeriod = reportDelay;
        splunkQueue = new ConcurrentLinkedQueue<>();
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }

    public SplunkMetricsBuilder newSplunkMetricsBuilder() {
        return new SplunkMetricsBuilder(env);
    }

    public CallMetricsBuilder newCallMetricsBuilder() {
        return new CallMetricsBuilder(env);
    }

    public CallMetricsBuilder newCallMetricsBuilder(MetricsEnvironment environment) {
        return new CallMetricsBuilder(environment);
    }

    public RoomServiceMetricsBuilder newRoomServiceMetricsBuilder() {
        return new RoomServiceMetricsBuilder(env);
    }

    public ConversationMetricsBuilder newConversationMetricsBuilder() {
        return new ConversationMetricsBuilder(env);
    }

    public WhiteboardServiceMetricsBuilder newWhiteboardServiceMetricsBuilder() {
        return new WhiteboardServiceMetricsBuilder(env);
    }

    public EncryptionSplunkMetricsBuilder newEncryptionSplunkMetricsBuilder() {
        return new EncryptionSplunkMetricsBuilder(env);
    }

    public void enqueueMetricsReport(MetricsReportRequest request) {
        if (request.getMetrics().size() == 0) {
            return;
        }

        if (request.getEndpoint() == MetricsReportRequest.Endpoint.SPLUNK) {
            splunkQueue.add(request);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkReachabilityChangedEvent event) {
        isNetworkConnected = event.isConnected();
    }

    protected void onMetricsProcessed(Collection<MetricsReportRequest> requests, boolean wasSuccessful) {
        Ln.v("onMetricsProcessed() was successful = %b", wasSuccessful);
    }

    protected boolean isIdle() {
        synchronized (sync) {
            return splunkQueue.isEmpty();
        }
    }

    public MetricsEnvironment getEnvironment() {
        return env;
    }

    public boolean flushMetrics() {
        /*
         * report<type>Metrics() now return a list of successfully processed MetricsReportRequests.
         * We keep calling this until we either see the flushRequest has been processed, or an error
         * occurs. Try our best to flush both queues.
         */
        boolean success = true;
        synchronized (sync) {
            if (!splunkQueue.isEmpty()) {
                MetricsReportRequest flushRequest = new MetricsReportRequest(MetricsReportRequest.Endpoint.FLUSH);
                splunkQueue.add(flushRequest);
                List<MetricsReportRequest> processedReports = new ArrayList<>();
                while (!processedReports.contains(flushRequest)) {
                    processedReports = reportSplunkMetrics();
                    if (processedReports == null) {
                        Ln.w("flushMetrics Splunk was not successful");
                        success = false;
                        break;
                    }
                }
            }
        }
        return success;
    }

    private List<MetricsReportRequest> initializeReport(Queue<MetricsReportRequest> source, MetricsReportRequest report) {
        // TODO: requests and reports really should be 2 different objects.
        List<MetricsReportRequest> requests = new ArrayList<MetricsReportRequest>();
        MetricsReportRequest request = source.poll();

        while (request != null) {
            report.getMetrics().addAll(request.getMetrics());
            requests.add(request);
            if (report.getMetrics().size() > MAX_METRICS_PER_REPORT) {
                break;
            }
            request = source.poll();
        }
        return requests;
    }

    private List<MetricsReportRequest> reportSplunkMetrics() {
        MetricsReportRequest report = new MetricsReportRequest(MetricsReportRequest.Endpoint.SPLUNK);
        List<MetricsReportRequest> requests = initializeReport(splunkQueue, report);
        try {
            if (!report.getMetrics().isEmpty()) {
                Response<Void> response = clientProvider.getMetricsClient().postGenericMetric(report).execute();

                if (response.isSuccessful()) {
                    onMetricsProcessed(requests, true);
                    return requests;
                } else {
                    return handleErrors(splunkQueue, requests, response, null);
                }
            }
        } catch (IOException exception) {
            return handleErrors(splunkQueue, requests, null, exception);
        } catch (NullPointerException e) { // See Crashlytics 16065
            Ln.i(e);
        }
        return null;
    }

    private List<MetricsReportRequest> handleErrors(Queue<MetricsReportRequest> source, List<MetricsReportRequest> requests, Response<Void> response, IOException e) {
        if (response != null && response.errorBody() != null) {
            int statusCode = response.code();

            if (statusCode >= 500) {
                Ln.d(e, "An error occurred posting metrics. Retrying later");
                source.addAll(requests);
            } else if (statusCode >= 400) {
                onMetricsProcessed(requests, true);
                return requests;
            }
        } else if (e != null) {
            Ln.d(e, "Error talking to servers");
        } else {
            Ln.w("An error occurred posting metrics. Retrying later");
            source.addAll(requests);
        }
        return null;
    }

    private final class ReportMetricsTimerTask extends TimerTask {
        private MetricsReporter metricsReporter;

        public ReportMetricsTimerTask(MetricsReporter metricsReporter) {
            this.metricsReporter = metricsReporter;
        }

        @Override
        public void run() {

            synchronized (sync) {

                if (metricsReporter.isNetworkConnected && !metricsReporter.locusDataCache.isInCall() && !ConversationSyncQueue.isSyncBusy() && metricsReporter.clientProvider.getMetricsClient() != null) {
                    Ln.v("Beginning metrics report run");
                    reportSplunkMetrics();
                    Ln.v("Completed metrics report run");
                } else {
                    Ln.v("Busy, skipping metrics report run");
                }
            }

            metricsReporter.startTimer();
        }
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        started = true;
        startTimer();
    }

    public void startTimer() {
        stopTimer();
        if (started) {
            synchronized (sync) {
                jobReporter = new Timer("Metrics reporter timer (start)");
                jobReporter.schedule(new ReportMetricsTimerTask(this), TimeUnit.SECONDS.toMillis(timerPeriod));
            }
        }
    }

    public void stopTimer() {
        synchronized (sync) {
            if (jobReporter != null) {
                jobReporter.cancel();
                jobReporter = null;
            }
        }
    }

    @Override
    public void stop() {
        started = false;
        stopTimer();
    }
}
