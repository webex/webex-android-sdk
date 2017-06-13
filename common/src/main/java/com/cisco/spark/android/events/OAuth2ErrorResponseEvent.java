package com.cisco.spark.android.events;

import android.net.Uri;

import com.cisco.spark.android.util.UriUtils;

import retrofit2.Response;

public class OAuth2ErrorResponseEvent {
    int code;
    Uri uri;

    public OAuth2ErrorResponseEvent(Response response) {
        code = response.code();
        uri = UriUtils.parseIfNotNull(response.raw().request().url().toString());
    }

    public OAuth2ErrorResponseEvent(okhttp3.Response response) {
        code = response.code();
        uri = UriUtils.parseIfNotNull(response.request().url().toString());
    }

    public int getCode() {
        return code;
    }

    public Uri getUri() {
        return uri;
    }
}
