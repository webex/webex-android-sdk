package com.cisco.spark.android.locus.events;


import com.cisco.spark.android.model.ErrorDetail;

public class InvalidLocusEvent {
    private ErrorDetail.CustomErrorCode errorCode;
    private String invitee;

    public InvalidLocusEvent(ErrorDetail.CustomErrorCode errorCode, String invitee) {
        this.errorCode = errorCode;
        this.invitee = invitee;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }

    public String getInvitee() {
        return invitee;
    }
}
