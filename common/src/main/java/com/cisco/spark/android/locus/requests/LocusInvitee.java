package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.features.CoreFeatures;

public class LocusInvitee extends DeltaRequest {
    private String invitee;

    public LocusInvitee(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

    public String getInvitee() {
        return invitee;
    }

    public void setInvitee(String invitee) {
        this.invitee = invitee;
    }
}
