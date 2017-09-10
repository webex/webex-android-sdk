package com.ciscospark.androidsdk.phone;

/**
 * Created by zhiyuliu on 04/09/2017.
 */

public interface CallMembership {

    enum State {
        UNKNOWN,
        IDLE,
        NOTIFIED,
        JOINED,
        LEFT,
        DECLINED
    }

    boolean isInitiator();

    String getPersonId();

    State getState();

    String getEmail();

    String getSipUrl();

    String getPhoneNumber();

    boolean isSendingVideo();

    boolean isSendingAudio();
}
