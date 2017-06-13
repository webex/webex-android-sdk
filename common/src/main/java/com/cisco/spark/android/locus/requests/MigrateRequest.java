package com.cisco.spark.android.locus.requests;

import android.net.Uri;

public class MigrateRequest {
    private LocusInvitee invitee;
    private boolean isMigrateSelf;
    private Uri deviceUrl;

    public MigrateRequest(String invitee, boolean isMigrateSelf, Uri deviceUrl) {
        LocusInvitee li = new LocusInvitee();
        li.setInvitee(invitee);

        this.invitee = li;
        this.isMigrateSelf = isMigrateSelf;
        this.deviceUrl = deviceUrl;
    }

    public String getInvitee() {
        return invitee.getInvitee();
    }

    public boolean geiIsMigrateSelf() {
        return isMigrateSelf;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }
}
