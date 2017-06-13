package com.cisco.spark.android.meetings;

public class WhistlerReservation {
    private WhistlerResourceType resourceType;
    private String reservedBy;
    private WhistlerRequestMetaData requestMetaData;

    public WhistlerReservation(WhistlerResourceType resourceType, String reservedBy, WhistlerRequestMetaData requestMetaData) {
        this.resourceType = resourceType;
        this.reservedBy = reservedBy;
        this.requestMetaData = requestMetaData;
    }

    public WhistlerResourceType getResourceType() {
        return resourceType;
    }

    public String getReservedBy() {
        return reservedBy;
    }

    public WhistlerRequestMetaData getRequestMetaData() {
        return requestMetaData;
    }
}


