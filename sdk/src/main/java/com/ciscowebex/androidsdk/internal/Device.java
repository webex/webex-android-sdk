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
import com.ciscowebex.androidsdk.internal.model.ServicesClusterModel;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.collection.Maps;

import java.util.HashMap;
import java.util.Map;

public class Device {

    public enum Type {

        ANDROID_CLIENT("ANDROID"),
        ANDROID_MEDIA_ENGINE("WME"),
        ANDROID_SDK("TEAMS_SDK_ANDROID"),
        WEB_CLIENT("WEB"),
        SIP("SIP"),
        WEBEX("WEBEX"),
        WEBEX_SHARE("SPARK_SHARE");

        private String typeName;

        Type(String name) {
            this.typeName = name;
        }

        public String getTypeName() {
            return typeName;
        }

    }

    public static final String DEVICE_URL = "DEVICE_URL";
    public static final String DEVICE_ID = "DEVICE_ID";
    private static final String MERCURY_REGISTRATION_QUERIES = "?mercuryRegistrationStatus=true";

    private String deviceType;

    private DeviceModel deviceModel;

    private RegionModel regionModel;

    private Map<String, String> clusterUrls;

    private String webSocketUrl;

    public Device(DeviceModel device, RegionModel region, ServicesClusterModel cluster) {
        Ln.d("Device: " + Json.get().toJson(device));
        this.deviceType = Type.ANDROID_SDK.getTypeName();
        this.deviceModel = device;
        this.regionModel = region;
        this.clusterUrls = cluster.getClusterUrls();
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

    public String getDeviceIdentifier() {
        return deviceModel.getDeviceIdentifier();
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

    // urn:TEAM:us-west-2_r:identityLookup, https://conv-r.wbx2.com/conversation/api/v1
    public String getServiceClusterUrl(String serviceClusterId) {
        return clusterUrls.get(serviceClusterId);
    }

    public String getIdentityServiceClusterUrl(String urn) {
        String ret = getServiceClusterUrl(urn + ":identityLookup");
        if (ret == null) {
            ret = Service.Conv.baseUrl(this);
        }
        return ret;
    }

    public String getClusterId(String url) {
        String id = null;
        for (Map.Entry<String, String> entry : clusterUrls.entrySet()) {
            if (url.startsWith(entry.getValue())) {
                return entry.getKey().substring(0, entry.getKey().lastIndexOf(":"));
            }
        }
        return WebexId.DEFAULT_CLUSTER_ID;
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

    public void store() {
        Settings.shared.store(Device.DEVICE_URL, getDeviceUrl());
        Settings.shared.store(Device.DEVICE_ID, getDeviceIdentifier());
    }

    public void clear() {
        Settings.shared.clear(Device.DEVICE_URL);
        Settings.shared.clear(Device.DEVICE_ID);
    }
}
