package com.ciscospark.core;


import com.cisco.spark.android.callcontrol.CallNotification;
import com.cisco.spark.android.callcontrol.NotificationActions;
import com.cisco.spark.android.locus.model.LocusKey;

class VideoTestCallNotification implements CallNotification {

    @Override
    public void notify(LocusKey locusKey, NotificationActions notificationActions) {

    }

    @Override
    public void dismiss(LocusKey locusKey) {

    }

    @Override
    public int getTimeout() {
        return 30;
    }
}
