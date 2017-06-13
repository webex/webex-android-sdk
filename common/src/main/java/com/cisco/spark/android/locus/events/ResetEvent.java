package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.mercury.MercuryClient;

public class ResetEvent {
    private MercuryClient.WebSocketStatusCodes code;

    public ResetEvent(MercuryClient.WebSocketStatusCodes code) {
        this.code = code;
    }

    public MercuryClient.WebSocketStatusCodes getCode() {
        return code;
    }
}
