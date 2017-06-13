package com.cisco.spark.android.model;

public class DeskFeedbackRequest {

    private String feedbackId, languageCode;

    public DeskFeedbackRequest() {}

    public DeskFeedbackRequest(String feedbackId, String languageCode) {
        this.feedbackId = feedbackId;
        this.languageCode = languageCode;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public String getLanguageCode() {
        return languageCode;
    }
}
