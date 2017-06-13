package com.cisco.spark.android.room;

public class RequestTpLogsRequest {

    private final String locusId;
    private final String callStart;
    private final String feedbackId;

    public RequestTpLogsRequest(String feedbackId) {
        this.feedbackId = feedbackId;
        this.locusId = null;
        this.callStart = null;
    }

    public RequestTpLogsRequest(String locusId, String callStart) {
        this.locusId = locusId;
        this.callStart = callStart;
        this.feedbackId = null;
    }

}
