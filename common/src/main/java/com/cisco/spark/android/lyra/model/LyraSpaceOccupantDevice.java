package com.cisco.spark.android.lyra.model;

import android.net.Uri;

public class LyraSpaceOccupantDevice {
    private Uri deviceUrl;
    private Links links;

    public LyraSpaceOccupantDevice(Uri deviceUrl, Links links) {
        this.deviceUrl = deviceUrl;
        this.links = links;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public Links getLinks() {
        return links;
    }
}
