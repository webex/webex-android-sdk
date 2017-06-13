package com.cisco.spark.android.util;


import com.cisco.spark.android.media.statistics.MediaStats;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class MediaStatsReporter {
    private long statsCounter;
    private MediaStats mediaStats;

    @Inject
    public MediaStatsReporter() {
        resetData();
    }

    public void resetData() {
        statsCounter = 0;
        mediaStats = new MediaStats();
    }

    public void incrementStatsCounter() {
        statsCounter++;
    }

    public long getStatsCounter() {
        return statsCounter;
    }

    public void setMediaStats(MediaStats mediaStats) {
        this.mediaStats = mediaStats;
    }

    public MediaStats getMediaStats() {
        return mediaStats;
    }
}
