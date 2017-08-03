package com.cisco.spark.android.locus.requests;


import com.cisco.spark.android.features.CoreFeatures;

public class CallLocusRequest extends JoinLocusRequest {
    private LocusInvitee invitee;

    public CallLocusRequest(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

    public LocusInvitee getInvitee() {
        return this.invitee;
    }

    public void setInvitee(final LocusInvitee invitee) {
        this.invitee = invitee;
    }
}
