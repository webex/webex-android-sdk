package com.cisco.spark.android.callcontrol.events;


import com.cisco.spark.android.model.ErrorDetail;

public class CallControlMeetingNotStartedEvent {
    private ErrorDetail.CustomErrorCode errorCode;

    public CallControlMeetingNotStartedEvent(ErrorDetail.CustomErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }
}
