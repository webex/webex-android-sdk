package com.cisco.spark.android.reachability;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import com.cisco.spark.android.core.SquaredBroadcastReceiver;

import javax.inject.Inject;

public class ConnectivityChangeReceiver extends SquaredBroadcastReceiver {
    @Inject
    NetworkReachability networkReachability;

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!isInitialized()) {
            return;
        }

        networkReachability.update();
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return intentFilter;
    }
}
