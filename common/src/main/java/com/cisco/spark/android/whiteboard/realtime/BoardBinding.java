package com.cisco.spark.android.whiteboard.realtime;

public class BoardBinding {

    private String[] bindings;
    private String deliveryStrategy;
    private String deviceType;

    public BoardBinding(String... bindings) {
        this.bindings = bindings;
        this.deliveryStrategy = "REPLICATED";
        this.deviceType = "UNKNOWN";
    }
}
