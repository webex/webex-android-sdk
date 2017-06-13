package com.cisco.spark.android.room;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseAltoMetricFlow implements AltoMetricsFlow {

    protected Map<AltoFlowEvent, Long> timestamps = new HashMap<>();

    protected AltoFlowEvent lastEvent;

    protected void clear() {
        this.lastEvent = null;
        timestamps.clear();
    }

    protected int millisBetween(AltoFlowEvent firstEvent, AltoFlowEvent secondEvent) {
        Long t1 = timestamps.get(firstEvent);
        Long t2 = timestamps.get(secondEvent);
        if (t1 == null || t2 == null)
            return 0;

        return (int) (t2 - t1);
    }
}
