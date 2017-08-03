package com.cisco.spark.android.locus.model;

public class SuggestedMedia {
    private String mediaType;
    private String mediaContent;
    private String direction;

    public String getMediaType() {
        return mediaType;
    }

    public String getMediaContent() {
        return mediaContent;
    }

    public String getDirection() {
        return direction;
    }

    //used for testing
    public SuggestedMedia(String mediaType, String mediaContent, String direction) {
        this.mediaType = mediaType;
        this.mediaContent = mediaContent;
        this.direction = direction;
    }
}
