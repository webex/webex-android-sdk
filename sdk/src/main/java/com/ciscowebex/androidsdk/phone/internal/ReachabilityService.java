package com.ciscowebex.androidsdk.phone.internal;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.model.CalliopeClusterModel;
import com.ciscowebex.androidsdk.internal.model.MediaEngineReachabilityModel;
import com.ciscowebex.androidsdk.internal.model.ReachabilityModel;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.utils.NetworkUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.Checker;

import java.util.List;
import java.util.Map;

public class ReachabilityService {

    private PhoneImpl phone;
    private String ipAddress;
    private long lastUpdate;
    private MediaEngineReachabilityModel feedback;

    public ReachabilityService(PhoneImpl phone) {
        this.phone = phone;
    }

    public MediaEngineReachabilityModel getFeedback() {
        return feedback;
    }

    public void fetch() {
        boolean isIpAddressChanged = isIpAddressChanged();
        boolean isMaxAgeReached = isDataExpired();
        if (isIpAddressChanged || isMaxAgeReached) {
            Ln.d("Fetch scheduled, isAddressChanged = %s, isMaxAgeReached = %s", isIpAddressChanged, isMaxAgeReached);
            performReachabilityCheck(data -> {
                if (data != null) {
                    feedback = data;
                    ipAddress = NetworkUtils.getLocalIpAddress();
                    lastUpdate = System.currentTimeMillis();
                    Ln.d("Fetch done: " + feedback);
                }
            });
        }
        else {
            Ln.d("Fetch skipped, isAddressChanged = %s, isMaxAgeReached = %s", isIpAddressChanged, isMaxAgeReached);
        }
    }

    public void clear() {
        ipAddress = null;
        phone.getEngine().clearReachability();
    }

    private boolean isIpAddressChanged() {
        String currentIpAddress = NetworkUtils.getLocalIpAddress();
        return !Checker.isEqual(ipAddress, currentIpAddress);
    }

    private boolean isDataExpired() {
        return System.currentTimeMillis() - lastUpdate > 7200000;
    }

    private void performReachabilityCheck(Closure<MediaEngineReachabilityModel> callback) {
        if (phone.getDevice() == null) {
            Ln.e("Failure: Not register!");
            return;
        }
        Service.CalliopeDiscorey.get("clusters")
                .auth(phone.getAuthenticator()).device(phone.getDevice()).model(CalliopeClusterModel.class)
                .error((CompletionHandler<CalliopeClusterModel>) error -> Ln.e("Failure: " + error))
                .async((Closure<CalliopeClusterModel>) model -> {
                    Map<String, Map<String, List<String>>> group = model.getClusterInfo();
                    if (group != null) {
                        phone.getEngine().performReachabilityCheck(group, callback);
                    }
                });
    }

}
