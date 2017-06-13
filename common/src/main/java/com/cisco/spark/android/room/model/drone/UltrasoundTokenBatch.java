package com.cisco.spark.android.room.model.drone;

import java.util.List;

public class UltrasoundTokenBatch {

    private List<String> tokens;
    private int tokenIntervalInSeconds;

    public int getTokenIntervalInSeconds() {
        return tokenIntervalInSeconds;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public UltrasoundTokenBatch(List<String> tokens, int tokenIntervalInSeconds) {
        this.tokens = tokens;
        this.tokenIntervalInSeconds = tokenIntervalInSeconds;
    }
}
