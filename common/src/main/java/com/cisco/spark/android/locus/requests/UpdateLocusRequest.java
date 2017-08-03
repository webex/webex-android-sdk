package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.features.CoreFeatures;

import java.util.List;

public class UpdateLocusRequest extends DeltaRequest {

    private List<LocusInvitee> invitees;
    private boolean alertIfActive;
    private String kmsMessage;

    public UpdateLocusRequest(CoreFeatures coreFeatures, List<LocusInvitee> invitees, boolean alertIfActive) {
        super(coreFeatures);
        this.invitees = invitees;
        this.alertIfActive = alertIfActive;
    }

    public List<LocusInvitee> getInvitees() {
        return this.invitees;
    }

    public boolean isAlertIfActive() {
        return  alertIfActive;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

    public void setKmsMessage(String kmsMessage) {
        this.kmsMessage = kmsMessage;
    }
}
