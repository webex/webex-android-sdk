package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;

import java.util.UUID;

public class RoomRequestLogsEvent extends MercuryData {
    private String feedbackId;
    private String email;
    private UUID locusId;
    private String callStart;

    public String getFeedbackId() {
        return feedbackId;
    }

    public UUID getLocusId() {
        return locusId;
    }

    public String getEmail() {
        return email;
    }

    public String getCallStart() {
        return callStart;
    }
}
