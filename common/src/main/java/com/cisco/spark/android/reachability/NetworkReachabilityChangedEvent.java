package com.cisco.spark.android.reachability;

public class NetworkReachabilityChangedEvent {
    private final boolean isNetworkConnected;

    public NetworkReachabilityChangedEvent(boolean isNetworkConnected) {
        this.isNetworkConnected = isNetworkConnected;
    }

    public boolean isConnected() {
        return isNetworkConnected;
    }
}
