package com.ciscospark.core;

import android.app.Activity;

import com.cisco.spark.android.notification.Gcm;

class VideoTestGcm implements Gcm {
    @Override
    public String register() {
        return null;
    }

    @Override
    public boolean checkAvailability(Activity activity) {
        return false;
    }

    @Override
    public void clear() {

    }
}
