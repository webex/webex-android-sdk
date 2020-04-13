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

package com.ciscowebex.androidsdk.internal;

import android.net.Uri;
import com.ciscowebex.androidsdk.internal.model.DeviceModel;
import com.ciscowebex.androidsdk.internal.model.RegionModel;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.collection.Maps;

import java.util.HashMap;
import java.util.Map;

public class Device {

    public static final String DEVICE_URL = "DEVICE_URL";

    public static final String SPARKBOARD_DEVICE_TYPE = "SPARK_BOARD";
    public static final String SPARKBOARD_MEDIA_ENGINE_TYPE = "ACANO_MEDIA_ENGINE";
    public static final String ANDROID_DEVICE_TYPE = "ANDROID";
    public static final String ANDROID_MEDIA_ENGINE_TYPE = "WME";
    public static final String UC_DEVICE_TYPE = "UC";
    public static final String TP_DEVICE_TYPE = "TP_ENDPOINT";
    public static final String PROVISIONAL_DEVICE_TYPE = "PROVISIONAL";
    public static final String SIP_DEVICE_TYPE = "SIP";
    public static final String WEBEX_DEVICE_TYPE = "WEBEX";
    public static final String WEBEX_SHARE_TYPE = "SPARK_SHARE";
    public static final String WEB_DEVICE_TYPE = "WEB";

    private static final String MERCURY_REGISTRATION_QUERIES = "?mercuryRegistrationStatus=true";

    private String deviceType;

    private DeviceModel deviceModel;

    private RegionModel regionModel;

    private String webSocketUrl;

    public Device(DeviceModel device, RegionModel region) {
        this.deviceType = ANDROID_DEVICE_TYPE;
        this.deviceModel = device;
        this.regionModel = region;
        this.webSocketUrl = deviceModel.getWebSocketUrl();
        if (!this.webSocketUrl.endsWith(MERCURY_REGISTRATION_QUERIES)) {
            try {
                this.webSocketUrl = Uri.parse(webSocketUrl + MERCURY_REGISTRATION_QUERIES).toString();
            } catch (Throwable t) {
                Ln.w(t);
            }
        }
    }

    public String getDeviceUrl() {
        return deviceModel.getDeviceUrl();
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getRegionCode() {
        return regionModel.getRegionCode();
    }

    public String getCountryCode() {
        return regionModel.getCountryCode();
    }

    public String getDeviceSettings() {
        return deviceModel.getDeviceSettings();
    }

    public String getServiceUrl(String key) {
        return deviceModel.getServiceUrl(key);
    }

    public Map<String, Object> toJsonMap(String overwriteType) {
        Map<String, Object> json = new HashMap<>();
        json.put("url", getDeviceUrl());
        json.put("deviceType", overwriteType == null ? getDeviceType() : overwriteType);
        json.put("regionCode", getRegionCode());
        json.put("countryCode", getCountryCode());
        json.put("capabilities", Maps.makeMap("groupCallSupported", true, "sdpSupported", true));
        return json;
    }
}
