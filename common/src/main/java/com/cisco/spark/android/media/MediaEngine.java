package com.cisco.spark.android.media;


import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.webex.wme.MediaSessionAPI;
import com.webex.wme.TraceServerSink;

public interface MediaEngine {
    // Parameters
    public static final String WME_BACK_CAMERA = "0";
    public static final String WME_FRONT_CAMERA = "1";
    public static final int MEDIA_OVERRIDE_AUDIO_RECEIVE_ON_PORT = 33430;
    public static final int MEDIA_OVERRIDE_VIDEO_RECEIVE_ON_PORT = 33432;
    public static final String SDP_TYPE = "SDP";
    public static final int AUDIO_SAMPLING_DURATION = 15000;
    public static final int MAX_NUMBER_STREAMS = 4;
    public static final String TCP_CONNECTION_TYPE = "TCP";

    public static final int AUDIO_MID = 1;
    public static final int VIDEO_MID = 2;
    public static final int SHARE_MID = 3;

    public static class VideoSCRParams {
        public int maxFs;
        public int maxFps;
        public int maxBr;
        public int maxDpb;
        public int maxMbps;
        public int priority;
        public int grouping;
        public boolean duplicate;

        VideoSCRParams(int fs, int fps, int br, int dpb, int mbps, int priority, int grouping, boolean duplicate) {
            this.maxFs = fs;
            this.maxFps = fps;
            this.maxBr = br;
            this.maxDpb = dpb;
            this.maxMbps = mbps;
            this.priority = priority;
            this.grouping = grouping;
            this.duplicate = duplicate;
        }
    }

    public enum MediaDirection {
        SendReceiveAudioVideoShare, Inactive, SendReceiveShareOnly, SendReceiveAudioOnly
    }

    // Start/Stop Media
    public void initialize();
    public void uninitialize();
    public boolean isInitialized();

    public MediaSession startMediaSession(MediaCallbackObserver mediaCallbackObserver, final MediaDirection mediaDirection);
    public void setActiveMediaSession(MediaSession mediaSession);

    public MediaSession getActiveMediaSession();

    public void setDeviceSettings(String deviceSettings);
    public void setLoggingLevel(MediaSessionAPI.TraceLevelMask level);
    public String getVersion();

    /**
     * Request to get audio data from the audio track to use for proximity
     * @param listener for callbacks
     */
    void setAudioDataListener(AudioDataListener listener);

    /**
     * Clear the proximity audio data listener
     */
    void clearAudioDataListener(AudioDataListener listener);

    public void headsetPluggedIn();
    public void headsetPluggedOut();

    // Abstraction used by LinusReachabilityService to avoid directly calling wme code
    void setTraceServerSink(TraceServerSink serverSink);
    void startTraceServer(String clusters);

}

