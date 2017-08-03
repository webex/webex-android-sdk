package com.cisco.spark.android.reachability;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;

@Singleton
public class NetworkReachability {
    private final EventBus bus;
    private ConnectivityManager connectivityManager;
    private boolean isConnected;

    @Inject
    public NetworkReachability(Context context, EventBus bus) {
        this.bus = bus;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        isConnected = isNetworkConnected();
    }

    public boolean isNetworkConnected() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public void update() {
        boolean oldIsConnected = isConnected;
        isConnected = isNetworkConnected();
        if (oldIsConnected != isConnected) {
            bus.post(new NetworkReachabilityChangedEvent(isConnected));
        }
    }

    public NetworkInfo getNetworkInfo() {
        return connectivityManager.getActiveNetworkInfo();
    }
}
