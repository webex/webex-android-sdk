package com.cisco.spark.android.room;

import com.cisco.spark.android.lyra.model.Link;

public interface LyraAnnounceCallback {

    /**
     * The token was accepted
     * @param link add to space link
     * @param secondsToNextTokenEmit when to expect next token change
     * @param maxTokenValidityInSeconds when the current token expires (usually longer than next token)
     */
    void onSuccess(Link link, String spaceId, long secondsToNextTokenEmit, long maxTokenValidityInSeconds, String proof);

    void onError(String errorCode);

}
