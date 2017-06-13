package com.cisco.spark.android.locus.model;

import java.util.Date;

/**
 *  A Floor for a MediaShare is a "temporary permission to access or manipulate" a MediaShare. From a ReST perspective,
 *  a Floor is a sub-resource of a MediaShare.  The Benificiary of a Floor is the Locus Participant that currently has
 *  permission to control the MediaShare.  A Requester of a Floor is a Locus Participant that wishes to obtain the floor
 *  for itself or for another Participant (the beneficiary).
 */
public class Floor {

    public final static String ACCEPTED = "ACCEPTED";
    public final static String GRANTED = "GRANTED";
    public final static String RELEASED = "RELEASED";

    public Floor(String disposition) {
        this.disposition = disposition;
    }

    private String disposition;

    private Date requested;

    private Date granted;

    private Date released;

    private LocusParticipant requester;

    private LocusParticipant beneficiary;

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public Date getRequested() {
        return requested;
    }

    public void setRequested(Date requested) {
        this.requested = requested;
    }

    public Date getGranted() {
        return granted;
    }

    public void setGranted(Date granted) {
        this.granted = granted;
    }

    public Date getReleased() {
        return released;
    }

    public void setReleased(Date released) {
        this.released = released;
    }

    public LocusParticipant getRequester() {
        return requester;
    }

    public void setRequester(LocusParticipant requester) {
        this.requester = requester;
    }

    public LocusParticipant getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(LocusParticipant beneficiary) {
        this.beneficiary = beneficiary;
    }
}
