package com.cisco.spark.android.locus.events;


public class LocusProcessorEvent {
    public enum Type {
        NEW_EVENT,
        PROCESSED_EVENT
    }

    private Type type;
    private String info;

    public LocusProcessorEvent(Type type, String info) {
        this.type = type;
        this.info = info;
    }

    public Type getType() {
        return type;
    }

    public String getInfo() {
        return info;
    }

    public String toString() {
        return "type: " + type.toString() + (info == null ? "" : ", info: " + info);
    }
}
