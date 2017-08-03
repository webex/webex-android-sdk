package com.cisco.spark.android.locus.model;

import android.net.Uri;

import java.util.UUID;

public class MediaConnection  {
    private String localSdp;
    private String remoteSdp;
    private String type;
    private UUID mediaId;
    private Uri actionsUrl;

    public String getLocalSdp() {
        return localSdp;
    }

    public void setLocalSdp(String localSdp) {
        this.localSdp = localSdp;
    }

    public String getRemoteSdp() {
        return remoteSdp;
    }

    public void setRemoteSdp(String remoteSdp) {
        this.remoteSdp = remoteSdp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public Uri getActionsUrl() {
        return actionsUrl;
    }
}
