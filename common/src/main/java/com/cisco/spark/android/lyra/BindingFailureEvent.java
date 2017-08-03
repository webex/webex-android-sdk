package com.cisco.spark.android.lyra;


public class BindingFailureEvent {
    public enum BindingFailType {
        NO_CONNECTION,
        OPEN_FAILURE
    }
    public BindingFailType type;

    public BindingFailureEvent(BindingFailType type) {
        this.type = type;
    }
}
