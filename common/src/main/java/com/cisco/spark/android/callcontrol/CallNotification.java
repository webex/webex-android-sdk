package com.cisco.spark.android.callcontrol;

import com.cisco.spark.android.locus.model.LocusKey;

public interface CallNotification {
    void notify(LocusKey locusKey, NotificationActions notificationActions);
    void dismiss(LocusKey locusKey);
    int getTimeout();
}
