package com.cisco.spark.android.model;

/**
 * Shared DTO among a number of endpoints whose responses have
 * the format of a single "url" field.
 */
public class UrlResponse {

    private String url;

    public UrlResponse() {

    }

    public UrlResponse(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
