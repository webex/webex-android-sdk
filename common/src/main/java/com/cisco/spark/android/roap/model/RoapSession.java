package com.cisco.spark.android.roap.model;


import com.github.benoitdion.ln.Ln;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoapSession {

    public enum SessionState {
        IDLE,
        OFFER_SENT,
        OFFER_RECEIVED,
        OFFER_REQUEST_SENT,
        OFFER_REQUEST_RECEIVED,
        ANSWER_SENT,
        ANSWER_RECEIVED,
        OFFER_RESPONSE_SENT,
        OFFER_RESPONSE_RECEIVED
    }

    private Integer seq;

    private SessionState state;

    private RoapSessionCallbacks roapSessionCallbacks;

    private boolean autoSendOK;

    public RoapSession(final Integer seq,
                       SessionState state,
                       RoapSessionCallbacks roapSessionCallbacks) {
        this.seq = seq;
        this.state = state;
        this.roapSessionCallbacks = roapSessionCallbacks;
        this.autoSendOK = false;
    }

    public RoapSession(final RoapSessionCallbacks roapSessionCallbacks) {
        this(0, SessionState.IDLE, roapSessionCallbacks);
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public SessionState getState() {
        return state;
    }

    public boolean autoSendOK() {
        return autoSendOK;
    }

    public void setAutoSendOK(boolean autoSendOK) {
        this.autoSendOK = autoSendOK;
    }

    //public functions

    public void sendRoapMesage(long tieBreaker)  {
        if (SessionState.IDLE.equals(state)) {
            sendOfferRequest(tieBreaker);
        } else {
            Ln.e(false, state.toString() + ": cannot send offer request");
        }
    }

    public void sendRoapMessage(String sdp, long tieBreaker) {
        sendRoapMessage(Arrays.asList(sdp), tieBreaker);
    }

    public void sendOK()  {
        if (SessionState.ANSWER_RECEIVED.equals(state)) {
            roapSessionCallbacks.sendRoapMessage(new RoapOkMessage(seq));
            state = SessionState.IDLE;
        } else {
            Ln.e(false, "in wrong state to send ok");
        }
    }

    public void sendError(RoapErrorType errorType, String errorCause) {
        roapSessionCallbacks.sendRoapMessage(new RoapErrorMessage(errorType, errorCause, seq));
        state = SessionState.IDLE;
    }

    public List<String> processRoapMessage(final RoapBaseMessage msg)  {
        try {
            if (msg instanceof RoapOfferMessage) {
                return processRoapMessage((RoapOfferMessage) msg);
            } else if (msg instanceof RoapAnswerMessage) {
                return processRoapMessage((RoapAnswerMessage) msg);
            } else if (msg instanceof RoapOkMessage) {
                return processRoapMessage((RoapOkMessage) msg);
            } else if (msg instanceof RoapOfferRequestMessage) {
                return processRoapMessage((RoapOfferRequestMessage) msg);
            } else if (msg instanceof RoapErrorMessage) {
                return processRoapMessage((RoapErrorMessage) msg);
            }
        } catch (Exception e) {
            sendError(RoapErrorType.FAILED, e.getMessage());
        }
        return null;
    }

    // private send methods

    //one day we may expose this to the public
    private void sendRoapMessage(final List<String> sdps, long tieBreaker)  {
        if (!isValidSdps(sdps)) {
            Ln.e("sdps cannot be null or empty");
        }

        if (state.equals(SessionState.IDLE) || state.equals(SessionState.OFFER_SENT)) {
            sendOffer(sdps, tieBreaker);
        } else if (state.equals(SessionState.OFFER_RECEIVED) || state.equals(SessionState.ANSWER_SENT)) {
            sendAnswer(sdps);
        } else {
            Ln.e(state.toString() + ": cannot send offer request");
        }
    }

    private void sendOffer(final List<String> sdps, long tieBreaker) {
        //allows for retransmissions
        boolean shouldIncrement = (state == SessionState.IDLE);
        roapSessionCallbacks.sendRoapMessage(new RoapOfferMessage(seq(shouldIncrement), sdps, tieBreaker));
        state = SessionState.OFFER_SENT;
    }

    private void sendAnswer(final List<String> sdps) {
        roapSessionCallbacks.sendRoapMessage(new RoapAnswerMessage(seq, sdps));
        state = SessionState.ANSWER_SENT;
    }

    private void sendOfferRequest(long tieBreaker) {
        //allows for retransmissions
        boolean shouldIncrement = (state == SessionState.IDLE);
        roapSessionCallbacks.sendRoapMessage(new RoapOfferRequestMessage(seq(shouldIncrement), tieBreaker));
        state = SessionState.OFFER_REQUEST_SENT;
    }

    private void sendErrorAndThrow(RoapErrorType errorType, String msg) throws RoapExternalSessionException {
        Ln.d("RoapSession.sendErrorAndThrow, msg = " + msg + ", type = " + errorType);
        sendError(errorType, msg);
        throw new RoapExternalSessionException(errorType, msg);
    }

    // private process methods

    private List<String>  processRoapMessage(final RoapOfferMessage msg) throws RoapExternalSessionException {
        if (state.equals(SessionState.OFFER_REQUEST_SENT)) {
            validateSeq(msg.getSeq());
        } else if (state.equals(SessionState.IDLE)) {
            validateSeq(msg.getSeq(), seq + 1);
        } else {
            if (state.equals(SessionState.OFFER_SENT)) {
                sendErrorAndThrow(RoapErrorType.DOUBLE_CONFLICT, "offer was already sent");
            } else {
                Ln.d("RoapSession.processRoapMessage(RoapOfferMessage), current state = " + state + ", seq = " + msg.seq);
            }
            return null;
        }

        validateSdpsField(msg.getSdps());

        state = SessionState.OFFER_RECEIVED;
        seq = msg.seq;
        roapSessionCallbacks.receivedRoapOffer(msg.getSdps());
        return msg.getSdps();
    }

    private List<String> processRoapMessage(final RoapAnswerMessage msg)
            throws RoapExternalSessionException, RoapInternalSessionException {
        if (!state.equals(SessionState.OFFER_SENT)) {
            sendErrorAndThrow(RoapErrorType.INVALID_STATE, "answer message not expected");
        }

        validateSeq(msg.getSeq());
        validateSdpsField(msg.getSdps());

        state = SessionState.ANSWER_RECEIVED;

        if (autoSendOK) {
            sendOK(); //state changed to IDLE
        }

        roapSessionCallbacks.receivedRoapAnswer(msg.getSdps());
        return msg.getSdps();
    }

    private List<String> processRoapMessage(final RoapOkMessage msg) throws RoapExternalSessionException {
        if (!state.equals(SessionState.ANSWER_SENT)) {
            sendErrorAndThrow(RoapErrorType.INVALID_STATE, "ok message not expected");
        }

        validateSeq(msg.getSeq());

        state = SessionState.IDLE;

        return Collections.emptyList();
    }

    private List<String> processRoapMessage(final RoapOfferRequestMessage msg) throws RoapExternalSessionException {
        sendErrorAndThrow(RoapErrorType.REFUSED, "remote offer request not supported");
        return Collections.emptyList();
    }

    private List<String> processRoapMessage(final RoapErrorMessage e) throws RoapExternalSessionException {
        state = SessionState.IDLE;
        throw new RoapExternalSessionException(e.getErrorType(), e.getErrorCause());
    }

    //private helpers


    private Integer seq(boolean increment) {
        return increment ? ++seq : seq;
    }

    private static boolean isValidSdps(final List<String> sdps) {
        return (sdps != null && sdps.size() > 0);
    }

    private void validateSdpsField(final List<String> sdps) throws RoapExternalSessionException {
        if (!isValidSdps(sdps)) {
            sendErrorAndThrow(RoapErrorType.NOMATCH, "sdps field is missing or empty");
        }
    }

    private void validateSeq(final Integer actual) throws RoapExternalSessionException {
        validateSeq(actual, seq);
    }

    private void validateSeq(final Integer actual, final Integer exp) throws RoapExternalSessionException {
        if (!actual.equals(exp)) {
            String msg = String.format("invalid seq actual %d expected %d", actual, seq);
            sendErrorAndThrow(RoapErrorType.OUT_OF_ORDER, msg);
        }
    }
}
