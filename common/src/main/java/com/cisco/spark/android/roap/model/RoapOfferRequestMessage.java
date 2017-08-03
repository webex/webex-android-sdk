package com.cisco.spark.android.roap.model;

import java.util.List;

public class RoapOfferRequestMessage extends RoapBaseMessage {
    @Deprecated
    private String offererSessionId;

    @Deprecated
    private String answererSessionId;

    private Long tieBreaker;

    private List<String> headers;

    public RoapOfferRequestMessage() {}

    @Deprecated
    public RoapOfferRequestMessage(final String offererSessionId,
                                   final String answererSessionId,
                                   final Integer seq,
                                   final Long tieBreaker) {
        this.offererSessionId = offererSessionId;
        this.answererSessionId = answererSessionId;
        this.seq = seq;
        this.tieBreaker = tieBreaker;
    }

    public RoapOfferRequestMessage(final Integer seq,
                                   final Long tieBreaker) {
        this.seq = seq;
        this.tieBreaker = tieBreaker;
    }

    @Deprecated
    public RoapOfferRequestMessage(final String offererSessionId,
                                   final String answererSessionId,
                                   final Integer seq,
                                   final Long tieBreaker,
                                   final List<String> headers) {
        this.offererSessionId = offererSessionId;
        this.answererSessionId = answererSessionId;
        this.seq = seq;
        this.tieBreaker = tieBreaker;
        this.headers = headers;
    }

    public RoapOfferRequestMessage(final Integer seq,
                                   final Long tieBreaker,
                                   final List<String> headers) {
        this.seq = seq;
        this.tieBreaker = tieBreaker;
        this.headers = headers;
    }

    @Deprecated
    public String getOffererSessionId() {
        return offererSessionId;
    }

    @Deprecated
    public String getAnswererSessionId() {
        return answererSessionId;
    }

    public Long getTieBreaker() {
        return tieBreaker;
    }

    public List<String> getHeaders() {
        return headers;
    }

}
