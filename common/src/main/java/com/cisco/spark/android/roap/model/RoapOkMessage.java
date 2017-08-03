package com.cisco.spark.android.roap.model;

public class RoapOkMessage extends RoapBaseMessage {

    public RoapOkMessage(final Integer seq) {
        this.messageType = OK;
        this.seq = seq;
    }
}
