package com.cisco.spark.android.media;


import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.webex.wme.TraceServerSink;

public class MockMediaEngine extends MediaEngineAdapter {

    private final Ln.Context lnContext;
    private final NaturalLog ln;

    private boolean initialized;

    private MediaSession activeMediaSession;

    public MockMediaEngine(Ln.Context lnContext) {
        this.lnContext = lnContext;
        this.ln = Ln.get(lnContext, "MockMediaEngine");
    }

    @Override
    public void setTraceServerSink(TraceServerSink serverSink) {
        ln.i("setTraceServerSink sink: %s", serverSink);
    }

    @Override
    public void startTraceServer(String clusters) {
        ln.i("setTraceServer clusters: %s", clusters);
    }

    @Override
    public void initialize() {
        initialized = true;
    }

    @Override
    public void uninitialize() {
        ln.i("uninitialize engine");
        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String getVersion() {
        return "00000000";
    }

    @Override
    public MediaSession createMediaSession(String callId) {
        ln.i("createMediaSession");

        MediaSession mediaSession = new MockMediaSession(lnContext);
        activeMediaSession = mediaSession;

        return mediaSession;
    }

    @Override
    public void setActiveMediaSession(MediaSession mediaSession) {
        ln.i("setActiveMediaSession mediaSession = %s", mediaSession);
        activeMediaSession = mediaSession;
    }


    @Override
    public MediaSession getActiveMediaSession() {
        ln.i("getActiveMediaSession");
        return activeMediaSession;
    }

}
