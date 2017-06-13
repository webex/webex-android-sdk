package com.cisco.spark.android.locus.events;

public class UnhandledEvent {
    private String message;
    private Object payload;

    public UnhandledEvent(String message, Object payload) {
        this.message = message;
        this.payload = payload;
    }

    public String getMessage() {
        return message;
    }

    public Object getPayload() {
        return payload;
    }
}
