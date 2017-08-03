package com.cisco.spark.android.media;

public interface MediaSessionCallbacks {
    void onICEComplete(String callId);
    void onICEFailed(String callId);

    void onFirstPacketRx(String callId, MediaType mediaType);
    void onFirstPacketTx(String callId, MediaType mediaType);

    /*
     * The following two are a bit ambiguous - is it the whole session stopping, or a transient stop? (or hold/resume)
     * If a transient stop should it be followed by 'onFirstPacketRx' when media restarts?
     * Only implemented for Darling, for now
     */
    void onMediaRxStop(String callId, MediaType mediaType);
    void onMediaTxStop(String callId, MediaType mediaType);

    // Following 4 to be implemented at a later date
    void onScrRx(String callId, MediaType mediaType);
    void onScaRx(String callId, MediaType mediaType);
    void onScrTx(String callId, MediaType mediaType);
    void onScaTx(String callId, MediaType mediaType);

    // Following 2 only implemented on WME-based media engine for now
    void onMediaBlocked(String callId, MediaType mediaType, boolean blocked);
    void onShareStopped(String callId);
}
