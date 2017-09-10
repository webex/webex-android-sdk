package com.ciscospark.androidsdk.webhook;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class Webhook {

    @SerializedName("id")
    private String _id;

    @SerializedName("name")
    private String _name;

    @SerializedName("targetUrl")
    private String _targetUrl;

    @SerializedName("resource")
    private String _resource;

    @SerializedName("event")
    private String _event;

    @SerializedName("filter")
    private String _filter;

    @SerializedName("secret")
    private String _secret;

    @SerializedName("created")
    private Date _created;

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public String getTargetUrl() {
        return _targetUrl;
    }

    public String getResource() {
        return _resource;
    }

    public String getEvent() {
        return _event;
    }

    public String getFilter() {
        return _filter;
    }

    public String getSecret() {
        return _secret;
    }

    public Date getCreated() {
        return _created;
    }
}
