package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;

public class MigrateRequest extends DeltaRequest {
    private LocusInvitee invitee;
    private boolean isMigrateSelf;
    private Uri deviceUrl;

    public MigrateRequest(CoreFeatures coreFeatures, String invitee, boolean isMigrateSelf, Uri deviceUrl) {
        super(coreFeatures);
        LocusInvitee li = new LocusInvitee(coreFeatures);
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
