package com.cisco.spark.android.media;


import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.webex.wme.MediaSessionAPI;
import com.webex.wme.TraceServerSink;

public interface MediaEngine {

    // Parameters
    String WME_BACK_CAMERA = "0";
    String WME_FRONT_CAMERA = "1";
    int MEDIA_OVERRIDE_AUDIO_RECEIVE_ON_PORT = 33430;
    int MEDIA_OVERRIDE_VIDEO_RECEIVE_ON_PORT = 33432;
    String SDP_TYPE = "SDP";
    int AUDIO_SAMPLING_DURATION = 15000;
    int MAX_NUMBER_STREAMS = 4;
    String TCP_CONNECTION_TYPE = "TCP";

    int AUDIO_MID = 1;
    int VIDEO_MID = 2;
    int SHARE_MID = 3;

    class VideoSCRParams {
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

    enum MediaDirection {
        SendReceiveAudioVideoShare, Inactive, SendReceiveShareOnly, SendReceiveAudioOnly
    }

    // Start/Stop Media
    void initialize();
    void uninitialize();
    boolean isInitialized();

    MediaSession createMediaSession(String callId);
    void setActiveMediaSession(MediaSession mediaSession);

    MediaSession getActiveMediaSession();

    void setDeviceSettings(String deviceSettings);
    void setLoggingLevel(MediaSessionAPI.TraceLevelMask level);
    String getVersion();

    /**
     * Request to get audio data from the audio track to use for proximity
     * @param listener for callbacks
     */
    void setAudioDataListener(AudioDataListener listener);

    /**
     * Clear the proximity audio data listener
     */
    void clearAudioDataListener(AudioDataListener listener);

    void headsetPluggedIn();
    void headsetPluggedOut();

    @Nullable
    Bitmap getLastContentFrame();

    // Abstraction used by LinusReachabilityService to avoid directly calling wme code
    void setTraceServerSink(TraceServerSink serverSink);
    void startTraceServer(String clusters);
    boolean isLastTraceServerUsable();

}

