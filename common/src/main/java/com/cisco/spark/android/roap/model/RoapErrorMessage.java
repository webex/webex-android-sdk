package com.cisco.spark.android.roap.model;

public class RoapErrorMessage extends RoapBaseMessage {

    @Deprecated
    private String offererSessionId;
    @Deprecated
    private String answererSessionId;
    private RoapErrorType errorType;
    private String errorCause;

    public RoapErrorMessage() {}

    /**
     * This is an overloaded constructor which creates an object of RoapErrorMessage with a mandatory ROAP Sequence number
     */
    @Deprecated
    public RoapErrorMessage(final String offererSessionId, final String answererSessionId,
                            final RoapErrorType errorType, final String errorCause, final int seq) {
        this.messageType = RoapBaseMessage.ERROR;
        this.offererSessionId = offererSessionId;
        this.answererSessionId = answererSessionId;
        this.errorType = errorType;
        this.errorCause = errorCause;
        this.seq = seq;
    }

    public RoapErrorMessage(final RoapErrorType errorType, final String errorCause, final int seq) {
        this.errorType = errorType;
        this.errorCause = errorCause;
        this.seq = seq;
    }

    @Deprecated
    public String getOffererSessionId() {
        return offererSessionId;
    }

    @Deprecated
    public String getAnswererSessionId() {
        return answererSessionId;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public RoapErrorType getErrorType() {
        return errorType;
    }
}
