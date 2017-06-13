package com.cisco.spark.android.locus.model;

import android.net.Uri;

import java.util.List;
import java.util.Map;

public class LocusParticipantDevice {
    private Uri url;
    private List<MediaConnection> mediaConnections;
    private String deviceType;
    private Map<String, String> featureToggles;
    private LocusParticipant.Intent intent;
    private Uri keepAliveUrl;
    private int keepAliveSecs;
    private LocusParticipant.State state;
    private String correlationId;

    public Uri getUrl() {
        return url;
    }

    public List<MediaConnection> getMediaConnections() {
        return mediaConnections;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Map<String, String> getFeatureToggles() {
        return featureToggles;
    }

    public LocusParticipant.Intent getIntent() {
        return intent;
    }

    public Uri getKeepAliveUrl() {
        return keepAliveUrl;
    }

    public int getKeepAliveSecs() {
        return keepAliveSecs;
    }

    public LocusParticipant.State getState() {
        return state;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public static class Builder {

        private LocusParticipantDevice device;

        public Builder() {
            device = new LocusParticipantDevice();
        }

        public Builder setUrl(Uri url) {
            device.url = url;
            return this;
        }

        public Builder setIntent(LocusParticipant.Intent intent) {
            device.intent = intent;
            return this;
        }

        public Builder setState(LocusParticipant.State state) {
            device.state = state;
            return this;
        }

        public Builder setKeepAlive(Uri url, int secs) {
            device.keepAliveUrl = url;
            device.keepAliveSecs = secs;
            return this;
        }

        public Builder setDeviceType(String deviceType) {
            device.deviceType = deviceType;
            return this;
        }

        public Builder setFeatureToggles(Map<String, String> featureToggles) {
            device.featureToggles = featureToggles;
            return this;
        }

        public Builder setMediaConnections(List<MediaConnection> mediaConnections) {
            device.mediaConnections = mediaConnections;
            return this;
        }

        public Builder setCorrelationId(String correlationId) {
            device.correlationId = correlationId;
            return this;
        }

        public LocusParticipantDevice build() {
            return device;
        }
    }
}
