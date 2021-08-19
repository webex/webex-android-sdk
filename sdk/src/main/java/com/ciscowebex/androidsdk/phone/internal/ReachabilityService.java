/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.phone.internal;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.model.CalliopeClusterModel;
import com.ciscowebex.androidsdk.internal.model.MediaEngineReachabilityModel;
import com.ciscowebex.androidsdk.utils.NetworkUtils;
import com.github.benoitdion.ln.Ln;
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
        Service.CalliopeDiscovery.homed(phone.getDevice()).get("clusters")
                .auth(phone.getAuthenticator()).model(CalliopeClusterModel.class)
                .error((CompletionHandler<CalliopeClusterModel>) error -> Ln.e("Failure: " + error))
                .async((Closure<CalliopeClusterModel>) model -> {
                    Map<String, Map<String, List<String>>> group = model.getClusterInfo();
                    if (group != null) {
                        phone.getEngine().performReachabilityCheck(group, callback);
                    }
                });
    }

}
