package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent.JoinType;
import com.cisco.spark.android.locus.model.LocusKey;

/**
 * Error joining locus
 */
public class ErrorJoiningLocusEvent {

    public static final String Error = "Failure";
    private String errorMessage;
    private int errorCode;
    private String usingResource;
    private LocusKey locusKey;

    @JoinType
    private int joinType;

    public ErrorJoiningLocusEvent(String errorMessage, int errorCode, String usingResource, LocusKey locusKey, @JoinType int joinType) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.usingResource = usingResource;
        this.locusKey = locusKey;
        this.joinType = joinType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getUsingResource() {
        return usingResource;
    }

    public LocusKey getLocusKey() {
        return  locusKey;
    }

    public int getJoinType() {
        return joinType;
    }
}
