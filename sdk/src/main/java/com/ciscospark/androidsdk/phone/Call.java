package com.ciscospark.androidsdk.phone;

import java.util.List;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import com.ciscospark.androidsdk.CompletionHandler;

/**
 * Created by zhiyuliu on 04/09/2017.
 */

public interface Call {

    public enum Direction {
        INCOMING, OUTGOING
    }

    public enum CallStatus {
        INITIATED, RINGING, CONNECTED, DISCONNECTED
    }

    Phone.FacingMode getFacingMode();

    void setFacingMode(Phone.FacingMode facingMode);

    Direction getDirection();

    CallStatus getStatus();

    void setObserver(CallObserver observer);

    CallObserver getObserver();

    List<CallMembership> getMemberships();

    CallMembership getFrom();

    CallMembership getTo();

    Rect getLocalVideoViewSize();

    Rect getRemoteVideoViewSize();

    boolean isRemoteSendingVideo();

    boolean isRemoteSendingAudio();

    boolean isSendingVideo();

    void setSendingVideo(boolean sending);

    boolean isSendingAudio();

    void setSendingAudio(boolean sending);

    boolean isReceivingVideo();

    void setReceivingVideo(boolean receiving);

    boolean isReceivingAudio();

    void setReceivingAudio(boolean receiving);

    void acknowledge(@NonNull CompletionHandler<Void> callback);

    void answer(@NonNull CallOption option, @NonNull CompletionHandler<Void> callback);

    void reject(@NonNull CompletionHandler<Void> callback);

    void hangup(@NonNull CompletionHandler<Void> callback);

    boolean isSendingDTMFEnabled();

    void sendDTMF(String dtmf, @NonNull CompletionHandler<Void> callback);
}
