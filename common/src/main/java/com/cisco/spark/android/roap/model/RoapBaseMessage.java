package com.cisco.spark.android.roap.model;


public class RoapBaseMessage {
    public static final String OFFER = "OFFER";
    public static final String ANSWER = "ANSWER";
    public static final String OK = "OK";
    public static final String OFFER_REQUEST = "OFFER_REQUEST";
    public static final String OFFER_RESPONSE = "OFFER_RESPONSE";
    public static final String ERROR = "ERROR";
    public static final String SHUTDOWN = "SHUTDOWN";

    public static final Long TIEBREAKER_MAX = Long.valueOf("FFFFFFFF", 16);

    public static final Long TIEBREAKER_MIN = 0L;

    private static final String VERSION = "2";
    private String version;
    protected Integer seq;
    protected String messageType;



    public RoapBaseMessage() {
        this.version = VERSION;
    }

    public String getVersion() {
        return version;
    }

    public Integer getSeq() {
        return seq;
    }

    public String getMessageType() {
        return messageType;
    }
}
