package com.cisco.spark.android.mercury.events;


public class KmsResponseComplete {
    private String message;
    private String destination;
    private boolean success;

    public String getKmsMessage() {
        return message;
    }

    public String getDestination() {
        return destination;
    }

    public boolean isSuccess() {
        return success;
    }
}
