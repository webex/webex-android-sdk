package com.cisco.spark.android.callcontrol.events;


import com.cisco.spark.android.model.ErrorDetail;

public class CallControlInvalidLocusEvent {
    private final ErrorDetail.CustomErrorCode errorCode;
    private final String invitee;

    public CallControlInvalidLocusEvent(ErrorDetail.CustomErrorCode errorCode, String invitee) {
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
