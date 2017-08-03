package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.model.ErrorDetail;

public class LocusUserIsNotAuthorized {
    public static final String Error = "LocusUserIsNotAuthorized";
    private final ErrorDetail.CustomErrorCode errorCode;
    private final boolean isJoined;
    private String usingResource;
    private LocusKey locusKey;
    private String errorMessage;

    public LocusUserIsNotAuthorized(boolean isJoined, ErrorDetail.CustomErrorCode errorCode, String usingResource, LocusKey locusKey, String errorMessage) {
        this.errorCode = errorCode;
        this.isJoined = isJoined;
        this.usingResource = usingResource;
        this.locusKey = locusKey;
        this.errorMessage = errorMessage;
    }

    public ErrorDetail.CustomErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isJoin() {

        return isJoined;
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
