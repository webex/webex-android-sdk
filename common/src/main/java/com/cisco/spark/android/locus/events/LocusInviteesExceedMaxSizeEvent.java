package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.model.ErrorDetail;

public class LocusInviteesExceedMaxSizeEvent {
    private final ErrorDetail.CustomErrorCode errorCode;
    private final boolean join;

    public LocusInviteesExceedMaxSizeEvent(boolean join, ErrorDetail.CustomErrorCode errorCode) {
        this.errorCode = errorCode;
        this.join = join;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isJoin() {
        return join;
    }
}
