package com.cisco.spark.android.lyra.model;


public class LyraSpaceSessions {

    private final LyraSpaceSessionSupported supported;
    private final boolean willAcceptNewSession;
    private final LyraSpaceSession primary;

    public LyraSpaceSessions(LyraSpaceSessionSupported supported,
                             boolean willAcceptNewSession,
                             LyraSpaceSession primary) {
        this.supported = supported;
        this.willAcceptNewSession = willAcceptNewSession;
        this.primary = primary;
    }

    public LyraSpaceSessionSupported getSupported() {
        return supported;
    }

    public boolean willAcceptNewSession() {
        return willAcceptNewSession;
    }

    public LyraSpaceSession getPrimary() {
        return primary;
    }
}

