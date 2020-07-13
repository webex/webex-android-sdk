/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.os.Build;
import android.util.Pair;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.model.DeviceModel;
import com.ciscowebex.androidsdk.internal.model.RegionModel;
import com.ciscowebex.androidsdk.internal.model.ServiceHostModel;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Objects;
import me.helloworld.utils.collection.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RegisterOperation implements Runnable {

    private Authenticator authenticator;

    private CompletionHandler<Pair<Device, Credentials>> callback;

    public RegisterOperation(Authenticator authenticator, CompletionHandler<Pair<Device, Credentials>> callback) {
        this.authenticator = authenticator;
        this.callback = callback;
    }

    @Override
    public void run() {
        Service.Region.global().get("region").model(RegionModel.class).error(callback).async((Closure<RegionModel>) region -> {
            String countryCode = Objects.defaultIfNull(region.getCountryCode(), "US");
            String regionCode = Objects.defaultIfNull(region.getRegionCode(), "US-WEST");
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceType", Device.ANDROID_DEVICE_TYPE);
            deviceInfo.put("countryCode", countryCode);
            deviceInfo.put("regionCode", regionCode);
            deviceInfo.put("ttl", String.valueOf(TimeUnit.DAYS.toSeconds(180)));
            deviceInfo.put("name", Build.DEVICE);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("localizedModel", Build.MODEL);
            deviceInfo.put("systemName", Build.PRODUCT);
            deviceInfo.put("systemVersion", Build.VERSION.RELEASE);
            deviceInfo.put("capabilities", Maps.makeMap("groupCallSupported", Boolean.TRUE, "sdpSupported", Boolean.TRUE));
            deviceInfo.put("deviceIdentifier", Settings.shared.get(Device.DEVICE_ID, UUID.randomUUID().toString()));

            Credentials.auth(authenticator, userResult -> {
                Credentials credentials = userResult.getData();
                if (credentials == null) {
                    callback.onComplete(ResultImpl.error(userResult.getError()));
                }
                else {
                    String deviceUrl = Settings.shared.get(Device.DEVICE_URL, null);
                    Ln.d("Saved deviceUrl: " + deviceUrl);

                    if (deviceUrl == null) {
                        Ln.d("Creating new device");
                        Service.U2C.global().get("user/catalog").with("format", "hostMap").auth(authenticator).model(ServiceHostModel.class).error(callback).async((Closure<ServiceHostModel>) host -> {
                            String url = host.getServiceUrl(Service.Wdm.name().toLowerCase());
                            Ln.d("WDM Url by U2C: " + url);
                            ServiceReqeust request = url != null ? Service.Wdm.specific(url) : Service.Wdm.global();
                            request.auth(authenticator).header("x-catalog-version2", "true").model(DeviceModel.class).error(callback);
                            request.post(deviceInfo).to("devices").async((Closure<DeviceModel>) model -> callback.onComplete(ResultImpl.success(new Pair<>(new Device(model, region), credentials))));
                        });
                    }
                    else {
                        Ln.d("Updating device");
                        ServiceReqeust request = Service.Wdm.specific(deviceUrl);
                        request.auth(authenticator).header("x-catalog-version2", "true").model(DeviceModel.class).error(callback);
                        request.put(deviceInfo).apply().async((Closure<DeviceModel>) model -> callback.onComplete(ResultImpl.success(new Pair<>(new Device(model, region), credentials))));
                    }
                }
            });
        });
    }
}
