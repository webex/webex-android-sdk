package com.cisco.spark.android.mercury.events;


import com.cisco.spark.android.roap.model.RoapBaseMessage;

public class RoapMessageEvent extends LocusEvent {

    private String correlationId;
    private RoapBaseMessage message;


    public String getCorrelationId() {
        return correlationId;
    }

    public RoapBaseMessage getMessage() {
        return message;
    }

}
