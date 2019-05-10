package com.ciscowebex.androidsdk.conversation;

import java.util.Date;

public class ActivityObject {

    protected String id;
    protected String objectType;
    protected String url;
    protected String displayName;
    protected Date published;

    public ActivityObject() {}

    public ActivityObject(String objectType, String id) {
        this.objectType = objectType;
        this.id = id;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }


}
