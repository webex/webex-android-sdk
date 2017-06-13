package com.cisco.spark.android.locus.requests;


import java.util.UUID;

public class CallLocusRequest extends JoinLocusRequest {
    private LocusInvitee invitee;
    private String correlationId;

    public CallLocusRequest() {
        correlationId = UUID.randomUUID().toString();
    }

    public LocusInvitee getInvitee() {
        return this.invitee;
    }

    public void setInvitee(final LocusInvitee invitee) {
        this.invitee = invitee;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
