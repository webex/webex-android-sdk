package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;

public class RoomSetRoomControlUserEvent extends MercuryData {
    private class Message {
        public String action;
        public String password;
    }

    private Message message;

    public String getAction() {
        return (message == null) ? null : message.action;
    }

    public String getPassword() {
        return (message == null) ? null : message.password;
    }
}
