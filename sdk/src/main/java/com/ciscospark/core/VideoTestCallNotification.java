package com.ciscospark.core;


import android.util.Log;

import com.cisco.spark.android.callcontrol.CallNotification;
import com.cisco.spark.android.callcontrol.NotificationActions;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;

class VideoTestCallNotification implements CallNotification {
    private LocusDataCache calls;

    public VideoTestCallNotification(LocusDataCache calls) {
        this.calls = calls;
    }

    @Override
    public void notify(LocusKey locusKey, NotificationActions notificationActions) {

    }

    @Override
    public void dismiss(LocusKey locusKey) {
        final LocusData call = calls.getLocusData(locusKey);
        call.setIsToasting(false);
    }

    @Override
    public int getTimeout() {
        return 30;
    }
}
