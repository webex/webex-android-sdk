package com.cisco.spark.android.roap.model;

import java.util.List;

public class RoapOfferResponseMessage extends RoapBaseMessage {
    private List<String> sdps;

    private Long tieBreaker;

    private List<String> headers;

    public RoapOfferResponseMessage() {}

    public RoapOfferResponseMessage(final Integer seq,
                            final List<String> sdps,
                            final Long tieBreaker) {
        this.seq = seq;
        this.sdps = sdps;
        this.tieBreaker = tieBreaker;
    }

    public RoapOfferResponseMessage(final Integer seq,
                            final List<String> sdps,
                            final Long tieBreaker,
                            final List<String> headers) {
        this.seq = seq;
        this.sdps = sdps;
        this.tieBreaker = tieBreaker;
        this.headers = headers;
    }

    public List<String> getSdps() {
        return sdps;
    }

    public Long getTieBreaker() {
        return tieBreaker;
    }

    public List<String> getHeaders() {
        return headers;
    }

}

