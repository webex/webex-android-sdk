package com.cisco.spark.android.presence;

import android.net.Uri;

import com.cisco.spark.android.mercury.MercuryData;

import java.util.Date;

public class PresenceStatusResponse extends MercuryData {
    private Uri url; // May Be null.
    private String subject;
    private PresenceStatus status;
    private Date statusTime;
    private Date lastActive;
    private int expiresTTL;

    public PresenceStatusResponse() {
    }

    public PresenceStatusResponse(String subject, PresenceStatus status, int expiresTTL) {
        this.subject = subject;
        this.status = status;
        this.expiresTTL = expiresTTL;
    }

    public Uri getUrl() {
        return url;
    }

    public String getSubject() {
        return subject;
    }

    public PresenceStatus getStatus() {
        return status;
    }

    public Date getStatusTime() {
        return statusTime;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public int getExpiresTTL() {
        return expiresTTL;
    }

    public Date getExpirationDate() {
        return PresenceUtils.getExpireTime(getExpiresTTL());
    }
}
