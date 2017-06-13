package com.ciscospark.core;


import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.util.LocationManager;

class VideoTestLocationManager implements LocationManager {
    @Override
    public String getCoarseLocationName() {
        return null;
    }

    @Override
    public String getCoarseLocationISO6709Position() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setApplicationController(ApplicationController applicationController) {

    }

    @Override
    public void clearLocationCache() {

    }

    @Override
    public boolean shouldStart() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}