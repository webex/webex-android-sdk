package com.cisco.spark.android.lyra.model;

import android.net.Uri;

import java.util.List;
import java.util.UUID;

public class LyraSpaceOccupant {
    private Uri url;
    private Identity identity;
    private boolean verified;
    private List<LyraSpaceOccupantDevice> devices;
    private Links links;

    public LyraSpaceOccupant(Uri url, Identity identity, boolean verified, List<LyraSpaceOccupantDevice> devices, Links links) {
        this.url = url;
        this.identity = identity;
        this.verified = verified;
        this.devices = devices;
        this.links = links;
    }

    public Uri getUrl() {
        return url;
    }

    public Identity getIdentity() {
        return identity;
    }

    public boolean isVerified() {
        return verified;
    }

    public List<LyraSpaceOccupantDevice> getDevices() {
        return devices;
    }

    public Links getLinks() {
        return links;
    }

    public UUID getId() {
        return identity.getId();
    }

    public String getDisplayName() {
        return identity.getDisplayName();
    }

}
