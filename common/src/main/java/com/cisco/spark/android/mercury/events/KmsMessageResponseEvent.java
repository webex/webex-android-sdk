package com.cisco.spark.android.mercury.events;


import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.model.KmsRequestResponseComplete;

public class KmsMessageResponseEvent extends MercuryData {

    private int statusCode;
    private KmsRequestResponseComplete encryption;

    public int getStatusCode() {
        return statusCode;
    }

    public KmsRequestResponseComplete getEncryptionKmsMessage() {
        return encryption;
    }
}
