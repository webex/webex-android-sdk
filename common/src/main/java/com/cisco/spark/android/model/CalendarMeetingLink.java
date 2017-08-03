package com.cisco.spark.android.model;

import android.net.Uri;

public class CalendarMeetingLink {

    private Uri href;
    private String rel;

    public Uri getHref() {
        return href;
    }

    public String getRel() {
        return rel;
    }

    public void setHref(Uri href) {
        this.href = href;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }
}
