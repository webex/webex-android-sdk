package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.model.ErrorDetail;

public class LocusInviteesExceedMaxSizeEvent {
    public static final String Error = "HttpForbidden";
    private final ErrorDetail.CustomErrorCode errorCode;
    private final boolean join;
    private String usingResource;
    private LocusKey locusKey;
    private String errorMessage;

    public LocusInviteesExceedMaxSizeEvent(boolean join, ErrorDetail.CustomErrorCode errorCode, String usingResource, LocusKey locusKey, String errorMessage) {
        this.errorCode = errorCode;
        this.join = join;
        this.usingResource = usingResource;
        this.locusKey = locusKey;
        this.errorMessage = errorMessage;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isJoin() {
        return join;
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
}
