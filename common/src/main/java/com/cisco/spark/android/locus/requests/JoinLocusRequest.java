package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.MediaConnection;

import java.util.ArrayList;
import java.util.List;

public class JoinLocusRequest extends DeltaRequest {
    private Uri deviceUrl;
    private List<MediaConnection> localMedias;
    private String usingResource;
    private boolean moveMediaToResource;
    private boolean supportsNativeLobby;
    // Boolean object so that we can provide with null value and avoid serialization when necessary
    private Boolean moderator = null;
    private String pin = null;
    private String correlationId;


    public JoinLocusRequest(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public List<MediaConnection> getLocalMedias() {
        return localMedias;
    }

    public void setLocalMedias(List<MediaConnection> localMedias) {
        this.localMedias = localMedias;
    }

    public void setLocalMedias(MediaConnection[] localMedias) {
        this.localMedias = new ArrayList<MediaConnection>(localMedias.length);
        for (MediaConnection mediaInfo : localMedias) {
            this.localMedias.add(mediaInfo);
        }
    }

    public String getUsingResource() {
        return usingResource;
    }

    public void setUsingResource(String usingResource) {
        this.usingResource = usingResource;
    }

    public boolean isMoveMediaToResource() {
        return moveMediaToResource;
    }

    public void setMoveMediaToResource(boolean moveMediaToResource) {
        this.moveMediaToResource = moveMediaToResource;
    }

    public boolean isSupportsNativeLobby() {
        return supportsNativeLobby;
    }

    public void setSupportsNativeLobby(boolean supportsNativeLobby) {
        this.supportsNativeLobby = supportsNativeLobby;
    }

    public boolean isModerator() {
        return moderator;
    }

    public void setModerator(Boolean moderator) {
        this.moderator = moderator;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
