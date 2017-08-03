package com.cisco.spark.android.metrics;

import com.cisco.spark.android.util.Clock;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to gather timing splits and dump the data to an analytics tracker and to the logs.
 */
public class Timing {
    private final Clock clock;
    private final MetricsReporter metricsReporter;
    private final String tag;

    private class Split {
        Split(String l) {
            timestamp = clock.now();
            if (l != null)
                label = l.toLowerCase(Locale.US).replace(' ', '_').replace('.', '_');
        }

        String label;
        long timestamp;
    }

    private final ArrayList<Split> splitLabels = new ArrayList<Split>();

    public Timing(Clock clock, MetricsReporter metricsReporter, String tag) {
        this.clock = clock;
        this.metricsReporter = metricsReporter;
        this.tag = tag;
        start();
    }

    /**
     * Clear the timing.
     */
    public void start() {
        synchronized (splitLabels) {
            splitLabels.clear();
            addSplit(null);
        }
    }

    /**
     * Add a split for the current time, labeled with label.
     *
     * @param label A string to be display with the split time.
     */
    public void addSplit(String label) {
        synchronized (splitLabels) {
            splitLabels.add(new Split(label));
        }
    }

    /**
     * Dump the timings and send metrics
     */
    public void endAndPublish() {
        endAndPublish(0, Long.MAX_VALUE, null);
    }

    /**
     * Dump the timings and send metrics
     */
    public void endAndPublish(Map<String, Object> additionalValues) {
        endAndPublish(0, Long.MAX_VALUE, additionalValues);
    }

    /**
     * Dump the timings and send metrics
     */
    public void endAndPublish(long min, long max) {
        endAndPublish(min, max, null);
    }

    /**
     * Dump the timings and send metrics
     */
    public void endAndPublish(long min, long max, Map<String, Object> additionalValues) {
        ArrayList<Split> toPublish = new ArrayList<Split>();
        synchronized (splitLabels) {
            toPublish.addAll(splitLabels);
            splitLabels.clear();
        }

        SplunkMetricsBuilder builder = metricsReporter.newSplunkMetricsBuilder();

        long first = 0;
        long previous = 0;
        for (Split split : toPublish) {
            if (first == 0) {
                first = split.timestamp;
                previous = split.timestamp;
                continue;
            }
            builder.reportTiming(tag + "_" + split.label, (int) (split.timestamp - previous));
            previous = split.timestamp;
        }
        if (toPublish.size() > 1)
            builder.reportTiming(tag + "_end", (int) (clock.now() - previous));

        long duration = (clock.now() - first);
        builder.reportTiming(tag, (int) duration);

        if (additionalValues != null) {
            for (Map.Entry<String, Object> entry : additionalValues.entrySet()) {
                builder.reportValue(entry.getKey(), entry.getValue());
            }
        }

        if (duration >= min && duration <= max)
            metricsReporter.enqueueMetricsReport(builder.build(tag));
    }

    public void end() {
        addSplit(null);
    }

    public long getTotalDuration() {
        Split first, last;
        synchronized (splitLabels) {
            first = splitLabels.get(0);
            last = splitLabels.get(splitLabels.size() - 1);
        }

        return last.timestamp - first.timestamp;
    }

    public long finish() {
        Split first;
        synchronized (splitLabels) {
            first = splitLabels.get(0);
        }

        long duration = (clock.now() - first.timestamp);

        return duration;
    }

    public long getLatestDurationToReport() {
        ArrayList<Split> toPublish = new ArrayList<Split>();
        synchronized (splitLabels) {
            toPublish.addAll(splitLabels);
            splitLabels.clear();
        }
        if (toPublish.size() == 0)
            return 0;
        Split split = toPublish.get(toPublish.size() - 1);
        long duration = clock.now() - split.timestamp;
        return duration;
    }
}
