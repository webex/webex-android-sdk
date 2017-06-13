package com.cisco.spark.android.whiteboard;

public class WhiteboardServiceEvent {

    private final boolean isServiceReady;
    private final boolean isMercuryReady;

    public WhiteboardServiceEvent(boolean isServiceReady, boolean isMercuryReady) {
        this.isServiceReady = isServiceReady;
        this.isMercuryReady = isMercuryReady;
    }

    public static WhiteboardServiceEvent changeWhiteboardServiceState(boolean state) {
        return new WhiteboardServiceEvent(state, state);
    }

    public boolean isServiceReady() {
        return isServiceReady;
    }

    public boolean isMercuryReady() {
        return isMercuryReady;
    }
}
