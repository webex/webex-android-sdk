package com.cisco.spark.android.room;

/**
 * This is fired when we get a token after not being paired
 */
public class FirstPairingEvent {
    private String roomName;

    public FirstPairingEvent(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }
}
