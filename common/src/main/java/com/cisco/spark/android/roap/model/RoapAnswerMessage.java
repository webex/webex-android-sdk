package com.cisco.spark.android.roap.model;

import java.util.List;

public class RoapAnswerMessage extends RoapBaseMessage {

    private List<String> sdps;

    public RoapAnswerMessage(final Integer seq, final List<String> sdps) {
        this.messageType = RoapBaseMessage.ANSWER;
        this.seq = seq;
        this.sdps = sdps;
    }

    public List<String> getSdps() {
        return sdps;
    }
}
