package com.cisco.spark.android.mercury.events;

public class RoomControlMessage {
    public enum Type {
        volumeUp, volumeDown, mute, unmute
    }

    private Type type;

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
