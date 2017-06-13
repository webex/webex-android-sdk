package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;

public class RoomSetUpgradeChannelEvent extends MercuryData {
    private String message;

    public String getChannel() {
        return message;
    }
}
