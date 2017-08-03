package com.cisco.spark.android.roap.model;

import java.util.List;

public class RoapOfferMessage extends RoapBaseMessage {

    private List<String> sdps;

    private Long tieBreaker;

    public RoapOfferMessage(final Integer seq, final List<String> sdps, final Long tieBreaker) {
        this.messageType = OFFER;
        this.seq = seq;
        this.sdps = sdps;
        this.tieBreaker = tieBreaker;
    }


    public List<String> getSdps() {
        return sdps;
    }

    public Long getTieBreaker() {
        return tieBreaker;
    }
}
