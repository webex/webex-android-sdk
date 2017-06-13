package com.cisco.spark.android.locus.model;

import android.net.Uri;

import com.google.gson.JsonObject;

public class LocusLink {
    private String rel;
    private Uri href;
    private String method;
    private JsonObject body;


    public String getRel() {
        return rel;
    }

    public Uri getHref() {
        return href;
    }

    public String getMethod() {
        return method;
    }

    public JsonObject getBody() {
        return body;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public void setHref(Uri href) {
        this.href = href;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setBody(JsonObject body) {
        this.body = body;
    }
}
