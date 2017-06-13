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
    public MediaSession startMediaSession(MediaCallbackObserver mediaCallbackObserver, final MediaDirection mediaDirection) {
        ln.i("startMediaSession");

        MediaSession mediaSession = new MockMediaSession(lnContext);
        activeMediaSession = mediaSession;

        String fakeSdp = "v=0\r\no=linus 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=cisco-mari:v0\r\na=cisco-mari-rate\r\nm=audio 20166 RTP/AVP 101\r\nc=IN IP4 166.78.47.126\r\nb=TIAS:64000\r\na=content:main\r\na=sendrecv\r\na=rtpmap:101 opus/48000/2\r\na=fmtp:101 maxplaybackrate=48000;maxaveragebitrate=64000;stereo=1\r\na=extmap:3/sendrecv urn:ietf:params:rtp-hdrext:toffset\r\na=rtcp-mux\r\nm=video 8060 RTP/AVP 101\r\nc=IN IP4 166.78.47.126\r\nb=TIAS:1000000\r\na=content:main\r\na=sendrecv\r\na=rtpmap:101 H264/90000\r\na=fmtp:101 profile-level-id=42000C;packetization-mode=1;max-mbps=27600;max-fs=920;max-fps=3000;max-br=1000\r\na=rtcp-fb:* nack pli\r\na=extmap:2/sendrecv\r\na=extmap:3/sendrecv urn:ietf:params:rtp-hdrext:toffset\r\na=rtcp-mux";
        mediaCallbackObserver.onSDPReady(mediaSession, fakeSdp);

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
