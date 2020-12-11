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

package com.ciscowebex.androidsdk.internal.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocusParticipantDeviceModel {

    private String url;
    private List<MediaConnectionModel> mediaConnections;
    private String deviceType;
    private Map<String, String> featureToggles;
    private IntentModel intent;
    private String keepAliveUrl;
    private int keepAliveSecs;
    private LocusParticipantModel.State state;
    private String correlationId;
    private boolean serverTranscoded;
    private boolean serverComposed;
    private LocusParticipantModel.ProvisionalDeviceType provisionalType;
    private String attendeeId;
    private String finalUrl;
    private String provisionalUrl;
    private Set<LocusParticipantModel.RequestedMedia> requestedMedia;

    public String getUrl() {
        return url;
    }

    public List<MediaConnectionModel> getMediaConnections() {
        return mediaConnections;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Map<String, String> getFeatureToggles() {
        return featureToggles;
    }

    public IntentModel getIntent() {
        return intent;
    }

    public void setIntent(IntentModel intent) {
        this.intent = intent;
    }

    public String getKeepAliveUrl() {
        return keepAliveUrl;
    }

    public int getKeepAliveSecs() {
        return keepAliveSecs;
    }

    public LocusParticipantModel.State getState() {
        return state;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getProvisionalUrl() {
        return provisionalUrl;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public String getAttendeeId() {
        return attendeeId;
    }

    public LocusParticipantModel.ProvisionalDeviceType getProvisionalType() {
        return provisionalType;
    }

    public boolean isServerTranscoded() {
        return serverTranscoded;
    }

    public boolean isServerComposed() {
        return serverComposed;
    }

    public Set<LocusParticipantModel.RequestedMedia> getRequestedMedia() {
        return requestedMedia;
    }
}
