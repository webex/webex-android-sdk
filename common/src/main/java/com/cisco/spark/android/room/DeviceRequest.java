package com.cisco.spark.android.room;

import android.net.Uri;

public class DeviceRequest {

    private final Uri deviceUrl;

    public DeviceRequest(final Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

}
