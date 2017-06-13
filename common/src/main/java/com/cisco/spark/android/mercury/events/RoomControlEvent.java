package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;

import java.util.UUID;

public class RoomControlEvent extends MercuryData {
    private RoomControlMessage controlMessage;
    private UUID userId;
    private String userName;

    public RoomControlMessage getControlMessage() {
        return controlMessage;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}
