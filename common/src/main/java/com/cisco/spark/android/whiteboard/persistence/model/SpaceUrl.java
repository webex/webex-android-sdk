package com.cisco.spark.android.whiteboard.persistence.model;


import android.net.Uri;

public class SpaceUrl {

    private Uri spaceUrl;

    public SpaceUrl(Uri spaceUrl) {
        this.spaceUrl = spaceUrl;
    }

    public Uri getSpaceUrl() {
        return spaceUrl;
    }

    public void setSpaceUrl(Uri spaceUrl) {
        this.spaceUrl = spaceUrl;
    }

}
