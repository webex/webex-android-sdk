package com.cisco.spark.android.locus.requests;

import java.util.List;

public class UpdateLocusRequest {

    private List<LocusInvitee> invitees;
    private boolean alertIfActive;
    private String kmsMessage;

    public UpdateLocusRequest(List<LocusInvitee> invitees, boolean alertIfActive) {
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
