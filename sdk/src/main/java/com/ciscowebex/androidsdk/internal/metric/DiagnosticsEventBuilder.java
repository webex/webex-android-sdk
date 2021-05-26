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

package com.ciscowebex.androidsdk.internal.metric;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import com.cisco.wx2.diagnostic_events.*;
import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.model.GenericMetricModel;
import com.ciscowebex.androidsdk.internal.model.LocusKeyModel;
import com.ciscowebex.androidsdk.phone.internal.CallImpl;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.utils.NetworkUtils;
import com.ciscowebex.androidsdk.utils.TrackingIdGenerator;
import com.ciscowebex.androidsdk.utils.UserAgent;
import com.github.benoitdion.ln.Ln;
import org.joda.time.Instant;

import java.util.UUID;

public class DiagnosticsEventBuilder {

    private static final long TIME_ZERO = 0;
    private SparkIdentifiers.Builder identBuilder;

    protected final PhoneImpl phone;

    /*
     * Clients MUST always populate correlationId, userId, deviceId, and trackingId.
     * Clients SHOULD populate as many identifiers as possible when sending events.
     * This includes locusSessionId, locusId, locusStartTime, and any others available to the client.
     */
    protected DiagnosticsEventBuilder(PhoneImpl phone) {
        this.phone = phone;
        this.identBuilder = SparkIdentifiers.builder();

        Credentials credentials = phone.getCredentials();
        if (credentials != null) {
            String userId = credentials.getUserId();
            if (userId != null) {
                try {
                    identBuilder.userId(UUID.fromString(userId));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (credentials.getOrgId() != null) {
                identBuilder.orgId(credentials.getOrgId());
            }
        }
        if (phone.getDevice() != null && phone.getDevice().getDeviceIdentifier() != null) {
            identBuilder.deviceId(phone.getDevice().getDeviceIdentifier());
        } else {
            identBuilder.deviceId("-"); // required field. This is far from ideal. Should find something that always has a correct deviceID
        }
    }

    protected void addCallInfo(CallImpl call) {
        if (call != null) {
            identBuilder.correlationId(call.getCorrelationId());
            identBuilder.locusUrl(call.getUrl());
            identBuilder.locusId(UUID.fromString(call.getModel().getKey().getLocusId()));
            long locusStartTime = call.getConnectedTime();
            if (locusStartTime > TIME_ZERO) {
                identBuilder.locusStartTime(new Instant(locusStartTime));
            }
        }
    }

    protected void addCallInfo(String correlationId, LocusKeyModel locusKey) {
        if (correlationId != null) {
            identBuilder.correlationId(correlationId);
        }
        if (locusKey != null) {
            try {
                if (locusKey.getUrl() != null) {
                    identBuilder.locusUrl(locusKey.getUrl());
                }
                identBuilder.locusId(UUID.fromString(locusKey.getLocusId()));
            } catch (Throwable e) {
                // LocusKey::getLocusId is almost always in UUID form, but in some tests it's not
                // so just catch this and carry on - we just won't set a locusId in the metric
            }
            long locusStartTime = System.currentTimeMillis();
            if (locusStartTime > TIME_ZERO) {
                identBuilder.locusStartTime(new Instant(locusStartTime));
            }
        }
    }

    private Origin.NetworkType getNetworkType() {
        ConnectivityManager connectivityManager = ((ConnectivityManager) phone.getContext().getSystemService(Context.CONNECTIVITY_SERVICE));
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                switch (networkInfo.getType()) {
                    case ConnectivityManager.TYPE_ETHERNET:
                        return Origin.NetworkType.ETHERNET;
                    case ConnectivityManager.TYPE_WIFI:
                        return Origin.NetworkType.WIFI;
                    case ConnectivityManager.TYPE_MOBILE:
                    case ConnectivityManager.TYPE_MOBILE_DUN:
                        return Origin.NetworkType.CELLULAR;
                    default:
                        return Origin.NetworkType.UNKNOWN;
                }
            }
        }
        return Origin.NetworkType.UNKNOWN;
    }

    private Origin.BuildType getBuildType() {
        return Origin.BuildType.PROD;
    }

    private OriginTime buildOriginTime() {
        try {
            return OriginTime.builder().triggered(Instant.now())
                    .sent(Instant.now()) // this needs to be set to pass validation, but will be updated later when actually sent
                    .build();
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid origin time property for event");
            Ln.e(e.getValidationError().getErrors().toString());
            return null;
        }
    }

    private Origin buildOrigin() {
        try {
            return Origin.builder()
                    .name(Origin.Name.ENDPOINT)
                    .userAgent(UserAgent.value)
                    .buildType(getBuildType())
                    .networkType(getNetworkType())
                    .localIP(NetworkUtils.getLocalIpAddress())
                    .usingProxy(NetworkUtils.isBehindProxy())
                    .mediaEngineSoftwareVersion(getStandardMediaEngineVersion(phone.getEngine().getVersion()))
                    .clientInfo(buildClientInfo())
                    .build();
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid origin property for event");
            Ln.e(e.getValidationError().getErrors().toString());
            return null;
        }
    }

    private ClientInfo buildClientInfo() {
        try {
            return ClientInfo.builder()
                    .clientType(ClientType.TEAMS_CLIENT)
                    .os(ClientInfo.Os.ANDROID)
                    .osVersion(Build.VERSION.RELEASE)
                    .localIP(NetworkUtils.getLocalIpAddress())
                    //.subClientType(SubClientType.WEB_APP)
                    .build();
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid clientInfo property for event");
            Ln.e(e.getValidationError().getErrors().toString());
            return null;
        }
    }

    protected String getStandardMediaEngineVersion(String rawVersion) {
        String value = rawVersion;
        if (value != null) {
            if (value.length() > 8 && value.substring(0, 6).matches("[0-9]+")) { // WME Format: "AABBCCDD (MediaSession)"
                String major = value.substring(0, 2);
                String minor = value.substring(2, 4);
                String patch = value.substring(4, 6);
                if (!TextUtils.isEmpty(major) && !TextUtils.isEmpty(minor) && !TextUtils.isEmpty(patch)) {
                    value = Integer.valueOf(major).toString() + "." + Integer.valueOf(minor).toString() + "." + Integer.valueOf(patch).toString();
                }
                return value;
            } else if (value.startsWith("AME")) { // Sparkboard Format: AMEx.y
                return value.substring(3);
            }
        }
        return "1.0.0";
    }

    protected GenericMetricModel build(ClientEvent.Builder clientEventBuilder) {
        try {
            identBuilder.trackingId(TrackingIdGenerator.shared.nextTrackingId());
            ClientEvent clientEvent = clientEventBuilder.identifiers(identBuilder.build()).build();
            Event event = Event.builder()
                    .eventId(UUID.randomUUID())
                    .version(1)
                    .originTime(buildOriginTime())
                    .origin(buildOrigin())
                    .event(clientEvent)
                    .build();
            return GenericMetricModel.buildDiagnosticMetric(event);
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid client event diagnostic metric");
            Ln.e(e.getValidationError().getErrors().toString());
            return null;
        }
    }

    protected GenericMetricModel build(MediaQualityEvent.Builder mediaEventBuilder) {
        try {
            identBuilder.trackingId(TrackingIdGenerator.shared.nextTrackingId());
            MediaQualityEvent mediaEvent = mediaEventBuilder.identifiers(identBuilder.build()).build();
            Event event = Event.builder()
                    .eventId(UUID.randomUUID())
                    .version(1)
                    .originTime(buildOriginTime())
                    .origin(buildOrigin())
                    .event(mediaEvent)
                    .build();
            return GenericMetricModel.buildDiagnosticMetric(event);
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid media quality event diagnostic metric");
            Ln.e(e.getValidationError().getErrors().toString());
            return null;
        }
    }
}
