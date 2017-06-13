package com.cisco.spark.android.mercury.events;

import android.net.Uri;

import com.cisco.spark.android.mercury.MercuryData;

import java.util.UUID;

public class RoomDeviceEnteredEvent extends MercuryData {
    private String userName;
    private Uri deviceUrl;
    private UUID deviceCorrelationId;


    public String getUserName() {
        return userName;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public UUID getDeviceCorrelationId() {
        return deviceCorrelationId;
    }
}
