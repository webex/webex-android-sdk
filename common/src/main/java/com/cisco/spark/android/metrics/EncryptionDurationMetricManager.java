package com.cisco.spark.android.metrics;

import android.net.Uri;
import android.text.TextUtils;
import android.util.LruCache;

import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.events.KmsKeyEvent;
import com.cisco.spark.android.mercury.events.KeyPushEvent;
import com.cisco.spark.android.metrics.value.EncryptionMetrics;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.processing.ActivityProcessor;
import com.cisco.spark.android.processing.ActivityProcessorCommand;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * This class is really hairy logically because the metrics requirements for encryption were complex
 * when it was written. It's overkill at this point.
 *
 * - Track duration for key fetches
 * - Detect when a "Decrypting..." placeholder is displayed more than n ms
 * - Keep a running tally of successfully decrypted messages & titles
 * - Keep a running tally of successfully displayed messages & titles
 * - Report to the metrics service periodically
 */
public class EncryptionDurationMetricManager implements ActivityProcessor {

    public static final long MAX_DURATION = TimeUnit.SECONDS.toMillis(10);
    public static final long KEY_REQUEST_TIMEOUT = MAX_DURATION + 1;
    public static final long MIN_UX_ERROR_DURATION_MS = 80; // waits shorter than this are success
    private static final int FETCHED_KEYS_QUEUE_SIZE = 100;

    private final MetricsReporter metricsReporter;
    private long sharedKeySetupStartTime;
    private NaturalLog ln;

    // Tracks decryption start time (by initial display time). When a metric has been reported for
    // this activity we set the value to 0.
    private final LruCache<String, ActivityTracking> trackedActivities = new LruCache<>(500);

    // a list of outstanding key requests, and successful fetches
    private ConcurrentHashMap<Uri, KeyRequestObject> keyRequestById = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Uri> fetchedKeys = new ConcurrentLinkedQueue<>();

    private EncryptionSplunkMetricsBuilder metricsBuilder;
    private EncryptionMetrics.UnboundKeyFetchMetric unboundKeyFetchMetric;

    public EncryptionDurationMetricManager(EventBus bus, MetricsReporter metricsReporter, ActivityListener activityListener, Ln.Context lnContext) {
        this.metricsReporter = metricsReporter;
        this.ln = Ln.get(lnContext, "cryptMetric");
        activityListener.register(this);
        bus.register(this);
    }

    public void enqueueReport() {
        synchronized (trackedActivities) {
            if (metricsBuilder != null)
                metricsReporter.enqueueMetricsReport(getMetricsBuilder().build());
            metricsBuilder = null;
        }
    }

    private EncryptionSplunkMetricsBuilder getMetricsBuilder() {
        synchronized (trackedActivities) {
            if (metricsBuilder == null) {
                metricsBuilder = metricsReporter.newEncryptionSplunkMetricsBuilder();
            }
            return metricsBuilder;
        }
    }

    /**
     * Called from bind() when an activity is displayed (encrypted or not)
     *
     * @param activityId
     * @param keyUri
     * @param isEncrypted True if the activity still needs to be decrypted
     */
    public void onActivityDisplayed(String activityId, Uri keyUri, boolean isEncrypted) {
        onActivityOrTitleDisplayed(activityId, keyUri, isEncrypted, false);
    }

    private void onActivityOrTitleDisplayed(String activityOrConvId, Uri keyUri, boolean isEncrypted, boolean isTitle) {
        if (activityOrConvId == null)
            return;

        synchronized (trackedActivities) {
            ActivityTracking activityTracking = trackedActivities.get(activityOrConvId);

            if (activityTracking == null) {
                activityTracking = new ActivityTracking(isTitle);
                trackedActivities.put(activityOrConvId, activityTracking);
            }

            if (activityTracking.isDisplayedDecrypted)
                return;

            if (isEncrypted) {
                KeyRequestObject keyRequestObject = keyRequestById.get(keyUri);
                if (keyRequestObject == null) {
                    if (fetchedKeys.contains(keyUri)) {
                        ln.v("Key has already been fetched--wait for it to appear in the cache.");
                        return;
                    }
                    keyRequestObject = new KeyRequestObject(0, System.currentTimeMillis());
                    keyRequestById.put(keyUri, keyRequestObject);
                }

                if (!keyRequestObject.activityIds.contains(activityOrConvId)) {
                    ln.v("Activity awaiting key " + activityOrConvId);
                    keyRequestObject.activityIds.add(activityOrConvId);
                }
            } else {
                ln.v("Displayed " + (isTitle ? "title " : "activity ") + activityOrConvId);
                EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus status = activityTracking.onDisplayedDecrypted();
                if (status != null) {
                    onDelayedDecryption(activityOrConvId, keyUri, null, "cache miss", status);
                }
                onKeyReceived(keyUri);
            }
        }
    }

    public void onTitleDisplayed(String conversationId, Uri keyUri, boolean isEncrypted) {
        if (TextUtils.isEmpty(conversationId) || keyUri == null)
            return;

        onActivityOrTitleDisplayed(conversationId, keyUri, isEncrypted, true);
    }

    // Called from bind() to display either "decrypting" or "failed" placeholder text
    public boolean isFailed(String activityId) {
        synchronized (trackedActivities) {
            ActivityTracking activityTracking = trackedActivities.get(activityId);
            if (activityTracking == null || activityTracking.isDecrypted)
                return false;

            return (activityTracking.getDuration() >= MAX_DURATION);
        }
    }

    private void onDelayedDecryption(String activityId, Uri keyUri, String conversationId, String reason, EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus waitStatus) {
        if (activityId == null)
            return;

        ln.i("Activity Decryption delayed " + activityId);

        synchronized (trackedActivities) {
            ActivityTracking activityTracking = trackedActivities.get(activityId);

            if (activityTracking != null) {
                EncryptionMetrics.UxDecryptionWaitMetric metric = new EncryptionMetrics.UxDecryptionWaitMetric(
                        keyUri,
                        conversationId,
                        waitStatus,
                        reason,
                        activityTracking.isTitle,
                        false);
                getMetricsBuilder().reportValue(metric);
            }
        }
    }

    public void onKeyRequested(Uri keyUri, long startTime) {
        synchronized (trackedActivities) {
            int retryNum = 0;
            KeyRequestObject keyRequestObject;
            if (keyRequestById.containsKey(keyUri)) {
                keyRequestObject = keyRequestById.get(keyUri);
                keyRequestObject.startTime = startTime;
            } else {
                if (!fetchedKeys.contains(keyUri)) {
                    keyRequestObject = new KeyRequestObject(retryNum, startTime);
                    keyRequestById.put(keyUri, keyRequestObject);
                }
            }
        }
    }

    private void onKeyReceived(Uri keyUri) {
        if (keyUri == null)
            return;

        synchronized (trackedActivities) {
            KeyRequestObject keyRequestObject = keyRequestById.remove(keyUri);
            if (fetchedKeys.size() > FETCHED_KEYS_QUEUE_SIZE)
                fetchedKeys.poll();
            fetchedKeys.add(keyUri);
            if (keyRequestObject == null) {
                return;
            }

            EncryptionMetrics.KeyFetchMetric metric = new EncryptionMetrics.KeyFetchMetric(
                    keyUri,
                    getKeyFetchRequestDuration(keyRequestObject),
                    false);
            getMetricsBuilder().reportValue(metric);

            if (keyRequestObject.activityIds != null) {
                for (String activityId : keyRequestObject.activityIds) {
                    ActivityTracking t = trackedActivities.get(activityId);
                    if (t != null) {
                        EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus ws = t.onDecrypted();
                        if (ws != null) {
                            onDelayedDecryption(activityId, keyUri, null, "cache miss", ws);
                        }
                    }
                }
            }
        }
    }

    public void onKeyFetchFailed(Uri keyUri) {
        if (keyUri == null)
            return;
        ln.i("Key Fetch Failed " + keyUri);

        synchronized (trackedActivities) {
            KeyRequestObject request = keyRequestById.get(keyUri);

            // Keys are added to the map when the request starts and removed when they finish.
            // Timed-out keys remain in the map to avoid sending multiple metrics for the same key.
            if (request != null && !request.isFailed) {
                request.isFailed = true;
                getMetricsBuilder().reportValue(
                        new EncryptionMetrics.KeyFetchMetric(keyUri, MAX_DURATION,
                                false));

                for (String activityId : request.activityIds) {
                    onDelayedDecryption(activityId, keyUri, null, EncryptionMetrics.ERROR_TIMEOUT,
                            EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus.failed);
                }
            }
        }
    }

    private long getKeyFetchRequestDuration(KeyRequestObject keyRequestObject) {
        long keyFetchRequestDuration = System.currentTimeMillis() - keyRequestObject.getStartTime();
        return (keyFetchRequestDuration <= MAX_DURATION) ? keyFetchRequestDuration : KEY_REQUEST_TIMEOUT;
    }

    /**
     * Called by the ActivityListener when a new activity is decrypted. It guarantees no duplicates.
     */
    @Override
    public ActivityProcessorCommand processActivity(Activity activity) {
        ln.d("EncryptionDurationMetricManager: processActivity " + activity.getId());
        if (activity == null || activity.getEncryptionKeyUrl() == null || !activity.getType().isEncryptable())
            return null;

        String activityId = activity.getId();

        ln.d("EncryptionDurationMetricManager: got activity id " + activityId);

        synchronized (trackedActivities) {
            ln.d("EncryptionDurationMetricManager: synchronized block " + activityId);

            if (activityId == null)
                return null;

            ActivityTracking activityTracking = trackedActivities.get(activityId);

            ln.d("EncryptionDurationMetricManager: getting tracked activity " + activityId);

            if (activityTracking == null) {
                activityTracking = new ActivityTracking(false);
                trackedActivities.put(activityId, activityTracking);
            }

            ln.d("EncryptionDurationMetricManager: got tracked activity " + activityId);

            if (activityTracking.isDecrypted) {
                ln.d("Activity already decrypted " + activityId);
                return null;
            }

            EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus status = activityTracking.onDecrypted();

            ln.v("message decrypted " + activityId + " in " +
                    activityTracking.getDuration() + "ms " + activity.getObject().getDisplayName());

            if (status != null) {
                onDelayedDecryption(
                        activityId,
                        activity.getEncryptionKeyUrl(),
                        activity.getConversationId(),
                        "cache miss",
                        status
                );
            }
        }
        return null;
    }

    public void onEvent(KeyPushEvent event) {
        if (event.getKeys() == null) {
            ln.i("Key event : " + event.getStatusCode() + " " + event.getFailureReason());
            onError(event.getStatusCode(), event.getFailureReason());
            return;
        }

        for (KeyObject key : event.getKeys()) {
            onKeyReceived(key.getKeyUrl());
        }
    }

    public void onEvent(KmsKeyEvent event) {
        if (event.getKeys() == null) {
            ln.i("KmsKeyEvent has no keys");
            return;
        }

        for (KeyObject key : event.getKeys()) {
            onKeyReceived(key.getKeyUrl());
        }
    }

    public void onEvent(LogoutEvent e) {
        synchronized (trackedActivities) {
            trackedActivities.evictAll();
            keyRequestById.clear();
            sharedKeySetupStartTime = 0;
            unboundKeyFetchMetric = null;
        }
    }

    public void onError(int status, String reason) {
        synchronized (trackedActivities) {
            getMetricsBuilder().reportValue(new EncryptionMetrics.Error(status, reason));
        }
    }

    public void onBeginSharedKeyNegotiation() {
        synchronized (trackedActivities) {
            if (sharedKeySetupStartTime > 0) {
                getMetricsBuilder().reportValue(new EncryptionMetrics.Error(400, EncryptionMetrics.ERROR_SHARED_KEY));
            }
            sharedKeySetupStartTime = System.currentTimeMillis();
        }
    }

    public void onFinishSharedKeyNegotiation() {
        synchronized (trackedActivities) {
            EncryptionMetrics.SharedKeySetupTime metric =
                    new EncryptionMetrics.SharedKeySetupTime(System.currentTimeMillis() - sharedKeySetupStartTime);
            sharedKeySetupStartTime = 0;
            getMetricsBuilder().reportValue(metric);
        }
    }

    public void onBeginFetchUnboundKeys(long keysToFetch) {
        synchronized (trackedActivities) {
            if (unboundKeyFetchMetric != null) {
                getMetricsBuilder().reportValue(new EncryptionMetrics.Error(400, EncryptionMetrics.ERROR_UNBOUND_KEY_FETCH_TIMEOUT));
            }
            unboundKeyFetchMetric = new EncryptionMetrics.UnboundKeyFetchMetric(keysToFetch);
        }
    }

    public void onFinishFetchUnboundKeys() {
        synchronized (trackedActivities) {
            if (unboundKeyFetchMetric != null) {
                unboundKeyFetchMetric.markFinishTime();
                getMetricsBuilder().reportValue(unboundKeyFetchMetric);
                unboundKeyFetchMetric = null;
            }
        }
    }

    private class KeyRequestObject {
        private int retryCount;
        private long startTime;
        private int status = 200;
        private List<String> activityIds = new ArrayList<>();
        public boolean isFailed;

        public KeyRequestObject(int retryCount, long startTime) {
            this.retryCount = retryCount;
            this.startTime = startTime;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public long getStartTime() {
            return startTime;
        }

        public int getStatus() {
            return status;
        }
    }

    private class ActivityTracking {
        private boolean isDisplayedDecrypted;
        private boolean isDecrypted;
        private long displayRequestTime = System.currentTimeMillis();
        private boolean isTitle;

        private ActivityTracking(boolean isTitle) {
            this.isTitle = isTitle;
        }

        private EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus onDisplayedDecrypted() {
            synchronized (trackedActivities) {
                if (isDisplayedDecrypted)
                    return null;

                EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus ret = onDecrypted();

                isDisplayedDecrypted = true;
                return ret;
            }
        }

        private EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus onDecrypted() {
            synchronized (trackedActivities) {
                if (isDecrypted)
                    return null;

                isDecrypted = true;
                EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus ret = null;
                long duration = getDuration();
                if (duration > MAX_DURATION) {
                    ret = EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus.failed;
                } else if (duration > getMinUxErrorDurationMs()) {
                    ret = EncryptionMetrics.UxDecryptionWaitMetric.WaitStatus.delayed;
                }
                return ret;
            }
        }

        private long getDuration() {
            return System.currentTimeMillis() - displayRequestTime;
        }
    }

    public long getMinUxErrorDurationMs() {
        return MIN_UX_ERROR_DURATION_MS;
    }

    // test helpers
    public boolean isKeyRequestByIdEmpty() {
        if (!keyRequestById.isEmpty()) {
            ln.w("Outstanding Key Requests: " + Strings.join(" ", keyRequestById.keySet().toArray(new Uri[]{})));
            return false;
        }
        return true;
    }

    public void setMinUxErrorDurationMs(long ms) {
        throw new IllegalAccessError("Not Implemented");
    }
}

