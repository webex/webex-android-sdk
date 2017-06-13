package com.cisco.spark.android.metrics;

import com.cisco.spark.android.util.Clock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TimingProvider {
    private final Clock clock;
    private final MetricsReporter metricsReporter;
    private Map<String, Timing> timings = new ConcurrentHashMap<String, Timing>();

    @Inject
    public TimingProvider(Clock clock, MetricsReporter metricsReporter) {
        this.clock = clock;
        this.metricsReporter = metricsReporter;
    }

    /**
     * Create and retrieve timing by name.
     * If a timing with the given name already exists return it otherwise create a new one.
     */
    public Timing get(String name) {
        if (!timings.containsKey(name)) {
            timings.put(name, new Timing(clock, metricsReporter, name));
        }
        return timings.get(name);
    }

    public boolean contains(String name) {
        return timings.containsKey(name);
    }

    public Timing remove(String name) {
        return timings.remove(name);
    }
}
