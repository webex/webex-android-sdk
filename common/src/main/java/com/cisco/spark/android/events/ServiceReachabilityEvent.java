package com.cisco.spark.android.events;

public class ServiceReachabilityEvent {
    private boolean reachabilityState;

    public ServiceReachabilityEvent(boolean state) {
        reachabilityState = state;
    }

    public boolean allServicesHealthy() {
        return reachabilityState;
    }
}
