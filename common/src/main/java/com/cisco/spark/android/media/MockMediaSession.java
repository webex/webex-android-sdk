package com.cisco.spark.android.media;

import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

public class MockMediaSession extends MediaSessionAdapter {

    private final NaturalLog ln;
    // FIXME: doesn't seem to be needed see usage
    private boolean mediaStarted;
    private boolean mediaSessionStarted;
    private boolean isScreenSharing = false;

    public MockMediaSession(Ln.Context lnContext) {
        ln = Ln.get(lnContext, "MockMediaSession");
    }

    @Override
    public void startSession(String deviceSettings, MediaEngine.MediaDirection mediaDirection, MediaSessionCallbacks mediaSessionCallbacks, SdpReadyCallback sdpReadyCallback) {
        ln.i("startSession()");
        mediaSessionStarted = true;

        String fakeSdp = "v=0\r\no=linus 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=cisco-mari:v0\r\na=cisco-mari-rate\r\nm=audio 20166 RTP/AVP 101\r\nc=IN IP4 166.78.47.126\r\nb=TIAS:64000\r\na=content:main\r\na=sendrecv\r\na=rtpmap:101 opus/48000/2\r\na=fmtp:101 maxplaybackrate=48000;maxaveragebitrate=64000;stereo=1\r\na=extmap:3/sendrecv urn:ietf:params:rtp-hdrext:toffset\r\na=rtcp-mux\r\nm=video 8060 RTP/AVP 101\r\nc=IN IP4 166.78.47.126\r\nb=TIAS:1000000\r\na=content:main\r\na=sendrecv\r\na=rtpmap:101 H264/90000\r\na=fmtp:101 profile-level-id=42000C;packetization-mode=1;max-mbps=27600;max-fs=920;max-fps=3000;max-br=1000\r\na=rtcp-fb:* nack pli\r\na=extmap:2/sendrecv\r\na=extmap:3/sendrecv urn:ietf:params:rtp-hdrext:toffset\r\na=rtcp-mux";
        sdpReadyCallback.onSDPReady(fakeSdp);
    }

    @Override
    public void endSession() {
        mediaSessionStarted = false;
    }

    @Override
    public boolean isMediaSessionEnding() {
        return mediaSessionStarted;
    }

    @Override
    public boolean isMediaSessionStarted() {
        return mediaSessionStarted;
    }

    @Override
    public String getLocalSdp() {
        // Overriding since default adapter implementation is to return null
        return "";
    }

    @Override
    public void startMedia() {
        ln.i("startMedia()");
        mediaStarted = true;
    }

    // FIXME: Should this set this.mediaStarted?
    @Override
    public void stopMedia() {
        ln.i("stopMedia()");
    }

    // FIXME: Should this do something to this.mediaStarted?
    @Override
    public void restartMedia() {
        ln.i("restartMedia()");
    }

    // FIXME: Should this report this.mediaStarted?
    @Override
    public boolean isMediaStarted() {
        return false;
    }

    @Override
    public int getMaxStreamCount() {
        return 1;
    }

    @Override
    public void startScreenShare(String startScreenShare) {
        isScreenSharing = true;
    }

    @Override
    public void stopScreenShare(String shareId) {
        isScreenSharing = false;
    }

    @Override
    public boolean isScreenSharing() {
        return isScreenSharing;
    }

}
