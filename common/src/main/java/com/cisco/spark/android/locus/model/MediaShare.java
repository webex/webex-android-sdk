package com.cisco.spark.android.locus.model;


import android.net.Uri;

/**
 * A MediaShare is a named collection of shared resources whose use can be
 * coordinated via Locus floor control.
 */
public class MediaShare  {

    public static final String SHARE_CONTENT_TYPE = "content";
    public static final String SHARE_WHITEBOARD_TYPE = "whiteboard";

    private String name;
    private Uri url;
    private String resourceUrl;
    private Floor floor;

    public MediaShare(Uri url, Floor floor) {
        this.url = url;
        this.floor = floor;
    }

    private MediaShare(String name, Uri url, String resourceUrl, Floor floor) {
        this.name = name;
        this.url = url;
        this.resourceUrl = resourceUrl;
        this.floor = floor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }

    public Floor getFloor() {
        return floor;
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public boolean isValidType() {
        return isWhiteboard() || isContent();
    }

    public boolean isWhiteboard() {
        return SHARE_WHITEBOARD_TYPE.equals(name);
    }

    public boolean isContent() {
        return SHARE_CONTENT_TYPE.equals(name);
    }

    public boolean isMediaShareGranted() {
        boolean result = false;
        if (getFloor() != null && Floor.GRANTED.equals(getFloor().getDisposition())) {
            result = true;
        }
        return result;

    }

    public void setResourceUrl(Uri resourceUrl) {
        this.resourceUrl = resourceUrl.toString();
    }

    public static class Builder {

        private Uri url;
        private Floor floor;
        private String name;
        private String resourceUrl;

        public Builder setUrl(Uri url) {
            this.url = url;
            return this;
        }

        public Builder setFloor(Floor floor) {
            this.floor = floor;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setResourceUrl(String resourceUrl) {
            this.resourceUrl = resourceUrl;
            return this;
        }

        public MediaShare build() {
            return new MediaShare(name, url, resourceUrl, floor);
        }
    }
}
