package com.cisco.spark.android.meetings;

import com.cisco.spark.android.util.Strings;

public class WhistlerReservationResponse {

    // Fields set by gson
    private String reservationUrl;
    private String resourceUrl;
    private WhistlerResponseMetaData responseMetaData;


    public WhistlerResponseMetaData getResponseMetaData() {
        return responseMetaData;
    }

    public String getReservationUrl() {
        return reservationUrl;
    }

    public String getReservationId() {
        return Strings.getLastBitFromUrl(reservationUrl);
    }

    public String getResourceId() {
        return Strings.getLastBitFromUrl(resourceUrl);
    }
}
