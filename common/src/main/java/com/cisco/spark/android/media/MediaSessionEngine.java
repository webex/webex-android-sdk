package com.cisco.spark.android.media;

import android.content.Context;

import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.media.events.StunTraceResultEvent;
import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;
import com.webex.wme.DeviceManager;
import com.webex.wme.MediaSessionAPI;
import com.webex.wme.StunTrace;
import com.webex.wme.StunTraceSink;
import com.webex.wme.TraceServer;
import com.webex.wme.TraceServerSink;
import com.webex.wme.WmeStunTraceResult;

import de.greenrobot.event.EventBus;


public class MediaSessionEngine implements MediaEngine, StunTraceSink {
    private static final int COMMIT_TAG_SIZE = 14;

    public static final String AUDIO_MEDIA_INPUT_FILE = "audio_1_16000_16.pcm";
    public static final String VIDEO_MEDIA_INPUT_FILE = "video_320_192_12_i420.yuv";
    private final NaturalLog ln;

    private boolean initialized;

    private final LogFilePrint log;
    private EventBus bus;
    private Settings settings;
    private Context context;
    private String commitHash;
    private final DeviceRegistration deviceRegistration;
    private final Gson gson;
    private final Ln.Context lnContext;
    private String deviceSettings;
    private DeviceManager devManager;

    private MediaSession activeMediaSession;

    public MediaSessionEngine(LogFilePrint log, EventBus bus, Settings settings, Context context,
                              String commitHash, DeviceRegistration deviceRegistration, Gson gson, Ln.Context lnContext) {
        this.log = log;
        this.bus = bus;
        this.settings = settings;
        this.context = context;
        this.deviceRegistration = deviceRegistration;
        this.gson = gson;
        this.lnContext = lnContext;
        this.ln = Ln.get(lnContext, "MediaSessionEngine");

        if (commitHash.length() > COMMIT_TAG_SIZE)
            this.commitHash = commitHash.substring(0, COMMIT_TAG_SIZE);
        else // handle debug builds where hash not set, and defaults to "undefined"
            this.commitHash = commitHash;
    }

    @Override
    public void setTraceServerSink(TraceServerSink serverSink) {
        ln.i("setTraceServerSink");
        TraceServer.INSTANCE.setTraceServerSink(serverSink);
    }

    @Override
    public void startTraceServer(String clusters) {
        ln.i("setTraceServer");
        TraceServer.INSTANCE.startTraceServer(clusters, clusters.length());
    }

    @Override
    public void initialize() {
        Ln.d("MediaSessionEngine.initialize(), MediaSessionEngine initialized = " + initialized);

        if (initialized) {
            return;
        }

        MediaSessionAPI.init(context);
        devManager = new DeviceManager();

        String logPath = log.getLogDirectory().getPath();
        Ln.d("MediaSessionEngine.initialize(), logPath = '" + logPath + "', git_hash = '" + commitHash + "'");
        MediaSessionAPI.INSTANCE.initMiniDump(logPath);

        StunTrace.INSTANCE.setStunTraceSink(this);

        initialized = true;
    }

    @Override
    public void uninitialize() {
        Ln.d("MediaSessionEngine.uninitialize(), initialized = " + initialized);

        if (initialized) {
            if (activeMediaSession != null) {
                activeMediaSession.endSession();
                activeMediaSession = null;
            }

            initialized = false;
            devManager = null;
        }
    }


    @Override
    public boolean isInitialized() {
        return initialized;
    }


    @Override
    public MediaSession startMediaSession(MediaCallbackObserver mediaCallbackObserver, final MediaDirection mediaDirection) {
        Ln.d("MediaSessionEngine.startMediaSession(), media direction = " + mediaDirection);

        MediaSession mediaSession = new MediaSessionImpl(devManager, bus, deviceRegistration, settings, gson, context, lnContext);
        activeMediaSession = mediaSession;
        mediaSession.startSession(deviceSettings, mediaDirection, mediaCallbackObserver);

        return mediaSession;
    }

    //@Override
    public MediaSession getActiveMediaSession() {
        return activeMediaSession;
    }

    @Override
    public void setActiveMediaSession(MediaSession mediaSession) {
        activeMediaSession = mediaSession;
    }


    @Override
    public void setDeviceSettings(String deviceSettings) {
        Ln.i("MediaSessionEngine.setDeviceSettings, deviceSettings:" + deviceSettings);
        this.deviceSettings = deviceSettings;
    }

    @Override
    public void setLoggingLevel(MediaSessionAPI.TraceLevelMask level) {
        Ln.d("MediaSessionEngine.setLoggingLevel(%d)", level.value());
        MediaSessionAPI.INSTANCE.setTraceMask(level);
    }

    @Override
    public void setAudioDataListener(AudioDataListener listener) {
        if (activeMediaSession != null) {
            Ln.d("setAudioDataListener: %s", listener);
            activeMediaSession.setAudioDataListener(listener);
        }
    }

    @Override
    public void clearAudioDataListener(AudioDataListener listener) {
        if (activeMediaSession != null) {
            Ln.d("clearAudioDataListener: %s", listener);
            activeMediaSession.clearAudioDataListener(listener);
        }
    }

    @Override
    public String getVersion() {
        // need to ensure that initialize is called before calling version()
        initialize();
        return MediaSessionAPI.INSTANCE.version() + " (MediaSession)";
    }


    public void headsetPluggedIn() {
        if (activeMediaSession != null) {
            activeMediaSession.headsetPluggedIn();
        }
    }

    @Override
    public void headsetPluggedOut() {
        if (activeMediaSession != null) {
            activeMediaSession.headsetPluggedOut();
        }
    }


    @Override
    public void OnResult(WmeStunTraceResult wmeStunTraceResult, String detail, long callId) {
        Ln.d("Stun Trace result, detail = " + detail + ", callId = " + callId);
        bus.post(new StunTraceResultEvent(detail));
    }
}
