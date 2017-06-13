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

    protected final Queue<MetricsReportRequest> circonusQueue;
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
        circonusQueue = new ConcurrentLinkedQueue<>();
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

    public ContentMetricsBuilder newContentMetricsBuilder() {
        return new ContentMetricsBuilder(env);
    }

    public RoomServiceMetricsBuilder newRoomServiceMetricsBuilder() {
        return new RoomServiceMetricsBuilder(env);
    }

    public ConversationMetricsBuilder newConversationMetricsBuilder() {
        return new ConversationMetricsBuilder(env);
    }

    public EncryptionSplunkMetricsBuilder newEncryptionSplunkMetricsBuilder() {
        return new EncryptionSplunkMetricsBuilder(env);
    }

    public void enqueueMetricsReport(MetricsReportRequest request) {
        if (request.getMetrics().size() == 0) {
            return;
        }

        if (request.getEndpoint() == MetricsReportRequest.Endpoint.CIRCONUS) {
            circonusQueue.add(request);
        } else if (request.getEndpoint() == MetricsReportRequest.Endpoint.SPLUNK) {
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
            return circonusQueue.isEmpty() && splunkQueue.isEmpty();
        }
    }

    public MetricsEnvironment getEnvironment() {
        return env;
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
                    reportCirconusMetrics();
                    reportSplunkMetrics();
                    Ln.v("Completed metrics report run");
                } else {
                    Ln.v("Busy, skipping metrics report run");
                }
            }

            metricsReporter.startTimer();
        }

        private void reportSplunkMetrics() {
            MetricsReportRequest report = new MetricsReportRequest(MetricsReportRequest.Endpoint.SPLUNK);
            List<MetricsReportRequest> requests = initializeReport(metricsReporter.splunkQueue, report);
            try {
                if (!report.getMetrics().isEmpty()) {
                    Response<Void> response = metricsReporter.clientProvider.getMetricsClient().postGenericMetric(report).execute();

                    if (response.isSuccessful()) {
                        metricsReporter.onMetricsProcessed(requests, true);
                    } else {
                        handleErrors(metricsReporter.splunkQueue, requests, response, null);
                    }
                }
            } catch (IOException exception) {
                handleErrors(metricsReporter.splunkQueue, requests, null, exception);
            } catch (NullPointerException e) { // See Crashlytics 16065
                Ln.i(e);
            }
        }

        private void reportCirconusMetrics() {
            MetricsReportRequest report = new MetricsReportRequest(MetricsReportRequest.Endpoint.CIRCONUS);
            List<MetricsReportRequest> requests = initializeReport(metricsReporter.circonusQueue, report);
            try {
                if (!report.getMetrics().isEmpty()) {
                    Response<Void> response = metricsReporter.clientProvider.getConversationClient().recordMetrics(report).execute();
                    if (response.isSuccessful()) {
                        metricsReporter.onMetricsProcessed(requests, true);
                    }
                }
            } catch (NullPointerException e) {  // See Crashlytics 16065
                Ln.i(e);
            } catch (IOException e) {
                Ln.i(e);
            }
        }

        private void handleErrors(Queue<MetricsReportRequest> source, List<MetricsReportRequest> requests, Response<Void> response, IOException e) {
            if (response != null && response.errorBody() != null) {
                int statusCode = response.code();

                if (statusCode >= 500) {
                    Ln.d(e, "An error occurred posting metrics. Retrying later");
                    source.addAll(requests);
                } else if (statusCode >= 400) {
                    metricsReporter.onMetricsProcessed(requests, true);
                }
            } else if (e != null) {
                Ln.d(e, "Error talking to servers");
            } else {
                Ln.w(e, "An error occurred posting metrics. Retrying later");
                source.addAll(requests);
            }
        }

        private List<MetricsReportRequest> initializeReport(Queue<MetricsReportRequest> source, MetricsReportRequest report) {
            // TODO: requests and reports really should be 2 different objects.
            List<MetricsReportRequest> requests = new ArrayList<MetricsReportRequest>();
            MetricsReportRequest request = source.poll();

            while (request != null) {
                report.getMetrics().addAll(request.getMetrics());
                requests.add(request);
                if (report.getMetrics().size() > MAX_METRICS_PER_REPORT)
                    break;
                request = source.poll();
            }
            return requests;
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
