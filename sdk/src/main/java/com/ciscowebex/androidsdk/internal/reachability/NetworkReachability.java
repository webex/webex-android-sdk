package com.ciscowebex.androidsdk.internal.reachability;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.queue.Scheduler;
import com.ciscowebex.androidsdk.utils.NetworkUtils;
import com.github.benoitdion.ln.Ln;
import okhttp3.*;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.TimerTask;

public class NetworkReachability extends BroadcastReceiver {

    public interface NetworkReachabilityObserver {
        void onReachabilityChanged(boolean connected);
        void onConfigurationChanged(boolean isProxyChanged);
    }

    private final NetworkReachabilityObserver observer;
    private final ConnectivityManager connectivityManager;
    private NetworkConnectionStatus currentNetworkConnectionStatus = new NetworkConnectionStatus(false);
    private TimerTask updateNetworkState;

    public NetworkReachability(Context context, NetworkReachabilityObserver observer) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);;
        this.observer = observer;
    }

    public void onReceive(Context context, Intent intent) {
        update();
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(android.net.Proxy.PROXY_CHANGE_ACTION);
        return intentFilter;
    }

    public void start() {
        update();
    }

    public boolean isNetworkConnected() {
        if (currentNetworkConnectionStatus.isConnected()) {
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    public boolean isBehindProxy() {
        return ProxySelector.getDefault().select(URI.create(Service.Wdm.endpoint(null) + "/ping")).get(0) != Proxy.NO_PROXY;
    }

    public void update() {
        if (updateNetworkState != null) {
            updateNetworkState.cancel();
        }
        updateNetworkState = Scheduler.schedule(this::asyncUpdateNetworkConnectivity, 20000, true);
    }

    public NetworkInfo getNetworkInfo() {
        return connectivityManager.getActiveNetworkInfo();
    }

    protected void asyncUpdateNetworkConnectivity() {
        boolean oldIsConnected = currentNetworkConnectionStatus.isConnected();
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if (info == null) {
            currentNetworkConnectionStatus = new NetworkConnectionStatus(false);
            if (oldIsConnected) {
                observer.onReachabilityChanged(false);
            }
            return;
        }

        boolean isConnected = info.isConnected();
        if (isConnected) {
            boolean currentIsBehindProxy = false;
            if (isBehindProxy()) {
                currentIsBehindProxy = true;
                OkHttpClient client = new OkHttpClient().newBuilder().proxyAuthenticator(new ProxyCheckAuthenticator()).build();
                Request request = new Request.Builder().url(Service.Wdm.endpoint(null) + "/").build();
                try {
                    Response response = client.newCall(request).execute();
                    Ln.d("response.code() = " + response.code());
                } catch (IOException ex) {
                    Ln.d("proxyRequiresAuth: " + currentNetworkConnectionStatus.isProxyRequiresAuth());
                }
            }
            int currentNetworkType  = info.getType();
            String currentIPAddress = NetworkUtils.getLocalIpAddress();
            NetworkConnectionStatus oldNetworkConnectionStatus = currentNetworkConnectionStatus;
            currentNetworkConnectionStatus = new NetworkConnectionStatus(isConnected, oldNetworkConnectionStatus.isProxyRequiresAuth(), currentNetworkType, currentIPAddress, currentIsBehindProxy);
            if (oldIsConnected && !currentNetworkConnectionStatus.equals(oldNetworkConnectionStatus)) {
                observer.onConfigurationChanged(currentNetworkConnectionStatus.isProxyChanged(oldNetworkConnectionStatus));
            }
        } else {
            currentNetworkConnectionStatus = new NetworkConnectionStatus(isConnected);
        }

        if (oldIsConnected != isConnected) {
            Ln.i("Different start and end states, sending event");
            observer.onReachabilityChanged(isConnected);
        }
    }

    private class ProxyCheckAuthenticator implements Authenticator {

        @Override
        public Request authenticate(Route route, Response response) {
            currentNetworkConnectionStatus.setProxyRequiresAuth(true);
            List<String> headers = response.headers("Proxy-Authenticate");
            Ln.d("Proxy-Authenticate Headers:\n%s", headers);
            ProxyHelpers.ProxyAutheticationMethod choosenMethod = ProxyHelpers.chooseAuthenticationMethod(headers);
            if (null == choosenMethod) {
                Ln.d("Unknown proxy authetication method.");
            }
            return null;
        }
    }
}

