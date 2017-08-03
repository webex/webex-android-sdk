package com.cisco.spark.android.callcontrol.events;


import com.cisco.spark.android.model.ErrorDetail;

public class CallControlUserIsNotAuthorized {
    private ErrorDetail.CustomErrorCode errorCode;

    public CallControlUserIsNotAuthorized(ErrorDetail.CustomErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }
}
