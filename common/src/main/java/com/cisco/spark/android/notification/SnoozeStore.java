package com.cisco.spark.android.notification;

public interface SnoozeStore {
    long getSnoozeUntil();
    void setSnoozeUntil(long time);
}
