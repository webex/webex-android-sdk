package com.cisco.spark.android.locus.events;


import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent.JoinType;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.model.ErrorDetail;

public class InvalidLocusEvent {

    public static final String Error = "InvalidLocus";

    private ErrorDetail.CustomErrorCode errorCode;
    private String invitee;
    private String usingResource;
    private LocusKey locusKey;
    private String errorMessage;

    @JoinType
    private int joinType;

    public InvalidLocusEvent(ErrorDetail.CustomErrorCode errorCode, String invitee, String usingResource, LocusKey locusKey,
                             String errorMessage, @JoinType int joinType) {
        this.errorCode = errorCode;
        this.invitee = invitee;
        this.usingResource = usingResource;
        this.locusKey = locusKey;
        this.errorMessage = errorMessage;
        this.joinType = joinType;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }

    public String getInvitee() {
        return invitee;
    }

    public String getUsingResource() {
        return usingResource;
    }

    public LocusKey getLocusKey() {
        return  locusKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @JoinType
    public int getJoinType() {
        return joinType;
    }
}
