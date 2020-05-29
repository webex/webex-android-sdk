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
        Service.Region.get("region").model(RegionModel.class).error(callback).async((Closure<RegionModel>) region -> {
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

            String deviceUrl = Settings.shared.get(Device.DEVICE_URL, null);
            Ln.d("Saved deviceUrl: " + deviceUrl);
            ServiceReqeust request;
            if (deviceUrl == null) {
                Ln.d("Creating new device");
                deviceInfo.put("deviceIdentifier", UUID.randomUUID().toString());
                request = Service.Wdm.post(deviceInfo);
                request.to("devices");
            }
            else {
                Ln.d("Updating device");
                String deviceIdentifier = Settings.shared.get(Device.DEVICE_ID, null);
                if (deviceIdentifier != null) {
                    deviceInfo.put("deviceIdentifier", deviceIdentifier);
                }
                request = Service.Wdm.put(deviceInfo);
                request.url(deviceUrl);
            }
            request.auth(authenticator).model(DeviceModel.class).error(callback).async((Closure<DeviceModel>) model -> Credentials.auth(authenticator, userResult -> {
                Credentials credentials = userResult.getData();
                if (credentials == null) {
                    callback.onComplete(ResultImpl.error(userResult.getError()));
                }
                else {
                    callback.onComplete(ResultImpl.success(new Pair<>(new Device(model, region), credentials)));
                }
            }));
        });
    }
}
