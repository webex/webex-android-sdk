package com.cisco.spark.android.metrics;

import android.util.LruCache;
import android.util.Pair;

class MetricsThrottler {
    private static final int LRU_CACHE_SIZE = 100;

    private static final LruCache<Pair<String, Integer>, Boolean> recentlyReported = new LruCache<>(LRU_CACHE_SIZE);
    static boolean shouldThrottle(String tag, Object value) {

        Pair<String, Integer> pair = new Pair<>(tag, value.hashCode());
        boolean inCache = recentlyReported.get(pair) != null;
        if (!inCache) {
            recentlyReported.put(pair, Boolean.TRUE);
        }

        return inCache;
    }
}
