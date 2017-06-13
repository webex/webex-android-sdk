package com.cisco.spark.android.room;

public interface AnnounceCallback {

    /**
     * The token was accepted
     * @param secondsToNextTokenEmit when to expect next token change
     * @param maxTokenValidityInSeconds when the current token expires (usually longer than next token)
     */
    void onSuccess(int secondsToNextTokenEmit, int maxTokenValidityInSeconds);

    void onTimeout();

    void onError();

}
