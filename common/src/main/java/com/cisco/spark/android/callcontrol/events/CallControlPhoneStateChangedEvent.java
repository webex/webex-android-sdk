package com.cisco.spark.android.callcontrol.events;

public class CallControlPhoneStateChangedEvent {

    private final int state;

    public CallControlPhoneStateChangedEvent(final int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
