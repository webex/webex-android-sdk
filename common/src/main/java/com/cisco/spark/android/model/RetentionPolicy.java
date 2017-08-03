package com.cisco.spark.android.model;

public class RetentionPolicy {
    private String retentionUrl;

    private int retentionDays = -1;

    private long lastRetentionSyncTimestamp = 0L;

    private int status = 0;

    private String errorMessage;

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public long getLastRetentionSyncTimestamp() {
        return lastRetentionSyncTimestamp;
    }

    public void setLastRetentionSyncTimestamp(long lastRetentionSyncTimestamp) {
        this.lastRetentionSyncTimestamp = lastRetentionSyncTimestamp;
    }

    public String getRetentionUrl() {
        return retentionUrl;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "retentionUrl = " + retentionUrl
                + " retentionDays = " + retentionDays
                + " lastRetentionSyncTimestamp = " + lastRetentionSyncTimestamp
                + " status = " + status
                + " errorMessage = " + errorMessage;
    }
}
