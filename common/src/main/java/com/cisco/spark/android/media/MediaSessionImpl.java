package com.cisco.spark.android.media;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.media.events.AvailableMediaChangeEvent;
import com.cisco.spark.android.media.events.DeviceCameraUnavailable;
import com.cisco.spark.android.media.events.MediaActiveSpeakerChangedEvent;
import com.cisco.spark.android.media.events.MediaBlockedChangeEvent;
import com.cisco.spark.android.media.events.NetworkCongestionEvent;
import com.cisco.spark.android.media.events.NetworkDisableVideoEvent;
import com.cisco.spark.android.media.events.NetworkDisconnectEvent;
import com.cisco.spark.android.media.events.NetworkLostEvent;
import com.cisco.spark.android.media.events.NetworkReconnectEvent;
import com.cisco.spark.android.media.statistics.MediaStats;
import com.cisco.spark.android.media.statistics.PacketStats;
import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wme.appshare.ScreenShareContext;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;
import com.webex.wme.DeviceManager;
import com.webex.wme.MediaConfig;
import com.webex.wme.MediaConnection;
import com.webex.wme.MediaStatistics;
import com.webex.wme.MediaTrack;
import com.webex.wme.WmeError;
import com.webex.wme.WmeSdpParsedInfo;
import com.webex.wseclient.WseEngine;
import com.webex.wseclient.WseSurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.media.MediaHelper.getMediaTypeString;
import static com.cisco.spark.android.media.MediaHelper.getVideoStreamString;
import static com.cisco.spark.android.media.MediaHelper.isActiveSpeaker;
import static com.cisco.spark.android.media.MediaHelper.isShare;
import static com.cisco.spark.android.media.MediaHelper.isVideo;
import static com.cisco.spark.android.media.MediaHelper.requestRemoteVideo;
import static com.cisco.spark.android.media.events.MediaActiveSpeakerVideoMuted.newVideoMuteWithCsiEvent;
import static com.cisco.spark.android.media.events.MediaActiveSpeakerVideoMuted.newVideoOnWithCsiEvent;

public class MediaSessionImpl implements MediaSession, MediaConnection.MediaConnectionListener,
        ScreenShareContext.OnShareStoppedListener {

    // video SCRs for typical 360p and 720p video
    // VideoSCRParams(int fs, int fps, int br, int dpb, int mbps, int priority, int grouping, boolean duplicate)
    private static final MediaEngine.VideoSCRParams videoScr180p = new MediaEngine.VideoSCRParams(396, 3000, 256000, 0, 7200, 255, 0, false);
    private static final MediaEngine.VideoSCRParams videoScr360p = new MediaEngine.VideoSCRParams(920, 3000, 640000, 0, 27600, 255, 0, false);
    private static final MediaEngine.VideoSCRParams videoScr720p = new MediaEngine.VideoSCRParams(3600, 3000, 1792000, 0, 108000, 255, 0, false);

    private static final int CONGESTION_THRESHOLD_LO_AUDIO_RTT = 600;
    private static final int CONGESTION_THRESHOLD_HI_AUDIO_RTT = 2000;
    private static final int CONGESTION_THRESHOLD_LO_VIDEO_RTT = 600;
    private static final int CONGESTION_THRESHOLD_HI_VIDEO_RTT = 2000;
    private static final int MIN_VIDEO_WIDTH = 160;
    private static final int MIN_VIDEO_HEIGHT = 90;
    private static final int MIN_ADAPTATION_BITRATE = 100000;

    private static final String HEADSET_PLUGIN_NOTIFICATION = "WmeAudioAndroid_HeadsetPlugin";
    private static final String HEADSET_PLUGOUT_NOTIFICATION = "WmeAudioAndroid_HeadsetPlugout";

    private static final long MAIN_VIDEO_ID = 0L;

    private final String callId;
    private final DeviceManager deviceManager;
    private final EventBus bus;
    private final DeviceRegistration deviceRegistration;
    private final Settings settings;
    private final Gson gson;
    private final Context context;
    private final NaturalLog ln;

    private String deviceSettings;
    private SdpReadyCallback sdpReadyCallback;
    private MediaSessionCallbacks mediaSessionCallbacks;


    private MediaConnection mediaConnection;

    private MediaTrack trackVideoLocal;
    private MediaTrack trackAudioLocal;
    private MediaTrack trackAudioRemote;
    private MediaTrack shareTrack;
    private MediaTrack screenTrack;

    private String localSdp;

    private View shareView;
    private View previewWindow;
    private View activeSpeakerView;

    private Date firstAudioPacketReceivedTime;
    private Date firstVideoPacketReceivedTime;

    private volatile boolean receivedFirstAudioPacket;
    private volatile boolean receivedFirstVideoPacket;
    private volatile boolean sentFirstAudioPacket;
    private volatile boolean sentFirstVideoPacket;

    private String audioPlaybackFilePath = "";
    private String videoPlaybackFilePath = "";

    private ConcurrentHashMap<Long, MediaTrack> remoteVideoTracks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, MediaTrack> csiTrackMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long, View> csiViewMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, View> vidViewMap = new ConcurrentHashMap<>();


    public String hwVideoSetting = "{\"hw-whitelist\": { "
            + "\"android\": { "
            + "\"defaults\": { "
            + "\"mediaCodec\":true,"
            + "\"yv12Capture\":false"
            + "}}}}";

    public String sw720pSetting = "{\n" +
            "    \"enable_sw720p\":{\n" +
            "        \"send\": {\n" +
            "            \"720p\": {\n" +
            "                \"general\": {}\n" +
            "            }\n" +
            "        },\n" +
            "        \"receive\": {\n" +
            "            \"720p\": {\n" +
            "                \"general\": {}\n" +
            "            }\n" +
            "        },\n" +
            "        \"simul\": {\n" +
            "            \"4l\": {\n" +
            "              \"general\": {}\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private boolean hardwareCodecEnabled;
    private boolean usedTcpFallback;
    private boolean cameraFailed;
    private boolean iceFailed;
    private boolean serverRejected;
    private boolean connected;
    private boolean mediaStarted;
    private boolean audioMuted;
    private boolean remoteAudioMuted;
    private boolean mediaSessionStarted;
    private boolean selfViewStarted;
    private boolean screenSharing;

    // enable simulcast by default
    private boolean simulcastEnabled = true;

    // enable qos by default
    private boolean qosEnabled = true;


    private boolean srtpEnabled;


    private MediaRequestSource videoMutedSource = MediaRequestSource.NONE;
    private String selectedCamera = MediaEngine.WME_FRONT_CAMERA;


    private SparseArray<Rect> decodeSizes = new SparseArray<>();
    private Rect decodeSizeShare = new Rect(0, 0, 1280, 720);

    private int maxStreamCount = 1;

    private String currentShareId = "";
    private boolean asShareViewer = false;

    private boolean alreadyWarnedNetworkCongestion;

    private Handler networkConnectionHandler;
    private AtomicBoolean isNetworkConnected = new AtomicBoolean(true);;

    private boolean enableStatistics = true;
    private Timer statisticsTimer;
    private Handler statisticsUIHandler;
    private String sessionStats;
    private PacketStats packetStats;
    private boolean endingSession = false;
    // incoming stream is composed from multiple video streams into a single video stream
    private boolean isCompositedVideoStream;

    private File lastShareContentFrame;

    enum MediaType {
        VIDEO, SHARING, AUDIO
    };

    public MediaSessionImpl(String callId, DeviceManager deviceManager, EventBus bus, DeviceRegistration deviceRegistration, Settings settings, Gson gson, Context context, Ln.Context lnContext) {
        this.callId = callId;
        this.deviceManager = deviceManager;
        this.bus = bus;
        this.deviceRegistration = deviceRegistration;
        this.settings = settings;
        this.gson = gson;
        this.context = context;
        this.ln = Ln.get(lnContext, "MediaSessionImpl");
    }


    @Override
    public void startSession(final String deviceSettings, final MediaEngine.MediaDirection mediaDirection,
                             MediaSessionCallbacks mediaSessionCallbacks, SdpReadyCallback sdpReadyCallback) {
        Ln.d("MediaSessionImpl.startSession(), mediaDirection= " + mediaDirection + ", this = " + this);
        endingSession = false;

        this.deviceSettings = deviceSettings;
        this.mediaSessionCallbacks = mediaSessionCallbacks;
        this.sdpReadyCallback = sdpReadyCallback;

        mediaSessionStarted = true;
        mediaStarted = false;
        audioMuted = false;
        remoteAudioMuted = false;
        screenSharing = false;
        videoMutedSource = MediaRequestSource.NONE;
        selectedCamera = MediaEngine.WME_FRONT_CAMERA;

        if (deviceRegistration.getFeatures().isScreenSharingEnabled()) {
            ScreenShareContext.getInstance().registerCallback(this);
        }

        mediaConnection = new MediaConnection();
        mediaConnection.setListener(this);


        if (settings.isSw720pEnabed()) {
            mediaConnection.GetGlobalConfig().SetDeviceMediaSettings(sw720pSetting);
        } else {
            if (deviceSettings != null && deviceSettings.trim().length() > 0) {
                mediaConnection.GetGlobalConfig().SetDeviceMediaSettings(deviceSettings);
            }
        }

        boolean enableTcAec = deviceRegistration.getFeatures().isTcAecEnabled() || settings.isTcAecEnabled();
        mediaConnection.GetGlobalConfig().enableTCAEC(enableTcAec);


        // hide/show stun trace ip address in metrics
        boolean hidShowIpAddresses = deviceRegistration.getFeatures().isHidePathIpAddressEnabled();
        mediaConnection.GetGlobalConfig().SetShowStunTraceIP(!hidShowIpAddresses);


        // Always start with full media (otherwise tracks won't be created!), then update if not SendRecv.
        addSendRecvMedia();
        if (!mediaDirection.equals(MediaEngine.MediaDirection.SendReceiveAudioVideoShare)) {
            updateMedia(mediaDirection);
        }

        createOffer();
    }



    @Override
    public void updateSession(final MediaEngine.MediaDirection mediaDirection, SdpReadyCallback sdpReadyCallback) {
        Ln.d("MediaSessionImpl.updateSession(), mediaDirection= " + mediaDirection + ", this = " + this);
        this.sdpReadyCallback = sdpReadyCallback;
        updateMedia(mediaDirection);
        createOffer();
    }

    @Override
    public synchronized void endSession() {
        Ln.d("MediaSessionImpl.endSession(), mediaConnection= " + mediaConnection + ", this = " + this);
        endingSession = true;

        if (statisticsTimer != null) {
            statisticsTimer.cancel();
            statisticsTimer = null;
        }

        // remove preview/remote render views
        setPreviewWindow(null);
        removeRemoteVideoWindows();

        if (deviceRegistration.getFeatures().isScreenSharingEnabled()) {
            ScreenShareContext.getInstance().unregisterCallback(this);
        }

        mediaSessionStarted = false;
        if (mediaConnection != null) {
            if (connected) {
                sessionStats = mediaConnection.getMediaSessionMetrics();
            }
            mediaConnection.stopMediaLogging();
            mediaConnection.setListener(null);
            mediaConnection.stop();
            mediaConnection = null;
        }

        selfViewStarted = false;
        trackAudioLocal = null;
        trackAudioRemote = null;
        trackVideoLocal = null;
        shareTrack = null;
    }

    @Override
    public boolean isMediaSessionEnding() {
        return endingSession;
    }

    @Override
    public boolean isMediaSessionStarted() {
        return mediaSessionStarted;
    }


    private void addSendRecvMedia() {
        MediaConnection.MediaDirection direction;
        if (selfViewStarted)
            direction = MediaConnection.MediaDirection.RecvOnly;
        else {
            direction = MediaConnection.MediaDirection.SendRecv;
            selfViewStarted = true;
        }

        mediaConnection.addMedia(MediaConnection.MediaType.Audio, direction, MediaEngine.AUDIO_MID, "");
        mediaConnection.addMedia(MediaConnection.MediaType.Video, direction, MediaEngine.VIDEO_MID, "");
        if (deviceRegistration.getFeatures().isScreenSharingEnabled()) {
            mediaConnection.addMedia(MediaConnection.MediaType.Sharing, MediaConnection.MediaDirection.SendRecv, MediaEngine.SHARE_MID, "");
        } else {
            mediaConnection.addMedia(MediaConnection.MediaType.Sharing, MediaConnection.MediaDirection.RecvOnly, MediaEngine.SHARE_MID, "");
        }
    }

    private void updateMedia(MediaEngine.MediaDirection mediaDirection) {
        Ln.d("MediaSessionImpl.updateMedia(%s)", mediaDirection.toString());

        boolean isScreenSharingEnabled = deviceRegistration.getFeatures().isScreenSharingEnabled();

        if (mediaDirection.equals(MediaEngine.MediaDirection.SendReceiveAudioVideoShare)) {
            mediaConnection.updateMedia(MediaConnection.MediaDirection.SendRecv, MediaEngine.AUDIO_MID);
            mediaConnection.updateMedia(MediaConnection.MediaDirection.SendRecv, MediaEngine.VIDEO_MID);
            if (isScreenSharingEnabled) {
                mediaConnection.updateMedia(MediaConnection.MediaDirection.SendRecv, MediaEngine.SHARE_MID);
            } else {
                mediaConnection.updateMedia(MediaConnection.MediaDirection.RecvOnly, MediaEngine.SHARE_MID);
            }
        } else if (mediaDirection.equals(MediaEngine.MediaDirection.SendReceiveShareOnly)) {
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.AUDIO_MID);
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.VIDEO_MID);
            if (isScreenSharingEnabled) {
                mediaConnection.updateMedia(MediaConnection.MediaDirection.SendRecv, MediaEngine.SHARE_MID);
            } else {
                mediaConnection.updateMedia(MediaConnection.MediaDirection.RecvOnly, MediaEngine.SHARE_MID);
            }
        } else if (mediaDirection.equals(MediaEngine.MediaDirection.SendReceiveAudioOnly)) {
            mediaConnection.updateMedia(MediaConnection.MediaDirection.SendRecv, MediaEngine.AUDIO_MID);
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.VIDEO_MID);
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.SHARE_MID);
        } else {
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.AUDIO_MID);
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.VIDEO_MID);
            mediaConnection.updateMedia(MediaConnection.MediaDirection.Inactive, MediaEngine.SHARE_MID);
        }
    }


    @Override
    public void onMediaReady(int mid, MediaConnection.MediaDirection dir, MediaConnection.MediaType type, MediaTrack track) {
        boolean isMuted = false;
        long retValue = 0;
        if (mid == MediaEngine.VIDEO_MID) {
            MediaConfig.VideoConfig videoConfigInst = mediaConnection.GetVideoConfig(MediaEngine.VIDEO_MID);
            if (videoConfigInst != null) {
                checkHardwareCodecSettings(videoConfigInst);
            }
            if (dir == MediaConnection.MediaDirection.SendOnly) {
                trackVideoLocal = track;

                // select front camera by default
                if (deviceManager != null) {
                    DeviceManager.MediaDevice dev = deviceManager.getCamera(DeviceManager.CameraType.Front);
                    if (dev != null) {
                        trackVideoLocal.SetCaptureDevice(dev);
                    }
                }
                retValue = track.Start(isMuted);

                if (previewWindow != null) {
                    trackVideoLocal.addRenderWindow(previewWindow);
                    trackVideoLocal.SetRenderMode(MediaTrack.ScalingMode.CropFill);
                }

            } else if (dir == MediaConnection.MediaDirection.RecvOnly) {
                Ln.d("MediaSessionImpl.onMediaReady, (MediaDirection.RecvOnly) (VIDEO_MID) track, vid  = " + track.getVID() + ", csi = " + Arrays.toString(track.getCSI()));
                Long vid = track.getVID();
                remoteVideoTracks.put(vid, track);

                View view = vidViewMap.get(vid);
                if (view != null) {
                    track.addRenderWindow(view);
                    setRemoteRenderMode(track, MediaEngine.VIDEO_MID);
                }
                //single video video shared between active video and share
                if (isSharingAsViewer() && vid == MAIN_VIDEO_ID) {
                    switchSingleVideoViewToShare(true);
                }

                track.Start(isMuted);
                if (hardwareCodecEnabled || settings.isSw720pEnabed()) {
                    requestRemoteVideo(track, videoScr720p);
                } else {
                    requestRemoteVideo(track, videoScr360p);
                }
            }
        } else if (mid == MediaEngine.AUDIO_MID) {
            if (dir == MediaConnection.MediaDirection.SendOnly) {
                trackAudioLocal = track;
            } else if (dir == MediaConnection.MediaDirection.RecvOnly) {
                trackAudioRemote = track;
            }
            retValue = track.Start(isMuted);
        } else if (mid == MediaEngine.SHARE_MID) {
            if (dir == MediaConnection.MediaDirection.RecvOnly) {
                Ln.d("MediaSessionImpl.onMediaReady, (MediaDirection.RecvOnly)(SHARE_MID) track, vid  = " + track.getVID() + ", csi = " + Arrays.toString(track.getCSI()));
                shareTrack = track;
                if (shareView != null) {
                    shareTrack.addRenderWindow(shareView);
                    setRemoteRenderMode(shareTrack, MediaEngine.SHARE_MID);
                }
                //single video video shared between active video and share
                if (isSharingAsViewer()) {
                    switchSingleVideoViewToShare(true);
                }

                //send SCR first to reduce delay.
                shareTrack.Start();

                //wme default : VideoSCRParams contentScr = new VideoSCRParams(20340, 3000, 14000000, 6750, 108000, 255, 0, false);
                //use 1080p first
                MediaEngine.VideoSCRParams contentScr = new MediaEngine.VideoSCRParams(8160, 3000, 1500000, 891, 108000, 255, 0, false);
                requestRemoteVideo(shareTrack, contentScr);
            } else if (dir == MediaConnection.MediaDirection.SendOnly) {
                screenTrack = track;
            }
        }


        Ln.d("MediaSessionImpl.onMediaReady, track=" + track + ", mConn=" + mediaConnection + ", mid=" + mid + ", isMuted=" + isMuted);
        if (retValue == WmeError.E_VIDEO_CAMERA_FAIL || retValue == WmeError.E_VIDEO_CAMERA_NO_DEVICE || retValue == WmeError.E_VIDEO_RENDERTHREAD_GL) {
            cameraFailed = true;
            Ln.e("camera error (%d) when starting video track", retValue);
            bus.post(new DeviceCameraUnavailable(callId));
        }

    }

    private void createOffer() {
        Ln.d("MediaSessionImpl.createOffer");
        setupMediaParameters();
        mediaConnection.createOffer();
    }

    @Override
    public void createAnswer(SdpReadyCallback sdpReadyCallback) {
        Ln.d("MediaSessionImpl.createAnswer");
        this.sdpReadyCallback = sdpReadyCallback;
        mediaConnection.createAnswer();
    }


    @Override
    public void onSDPReady(MediaConnection.SDPType type, String sdp) {
        Ln.v("MediaSessionImpl.onSDPReady, typ=" + type + ", sdp=" + sdp);
        if (sdpReadyCallback != null) {
            sdpReadyCallback.onSDPReady(sdp);
        }
        localSdp = sdp;
    }

    @Override
    public void answerReceived(final String sdp, final Map<String, String> featureToggles) {
        Ln.v("MediaSessionImpl.answerReceived: " + sdp);
        if (!sdp.isEmpty()) {
            if (mediaConnection != null) {
                if (featureToggles != null) {
                    String featureTogglesString = gson.toJson(featureToggles);
                    mediaConnection.GetGlobalConfig().SetFeatureToggles(featureTogglesString);
                }
                WmeSdpParsedInfo[] wmeSdpInfo =  mediaConnection.setReceivedSDP(MediaConnection.SDPType.Answer, sdp);
                for (WmeSdpParsedInfo sdpInfo : wmeSdpInfo) {
                    if (sdpInfo.mediaType == com.webex.wme.MediaConnection.MediaType.Video) {
                        this.isCompositedVideoStream = !sdpInfo.isMultistream;
                    }
                }
                mediaConnection.startMediaLogging(1000);
            }
        }
    }


    @Override
    public void offerReceived(final String sdp) {
        Ln.v("MediaSessionImpl.offerReceived: " + sdp);
        if (!sdp.isEmpty()) {
            if (mediaConnection != null) {
                mediaConnection.setReceivedSDP(MediaConnection.SDPType.Offer, sdp);
            }
        }
    }

    @Override
    public void updateSDP(final String sdp) {
        Ln.v("MediaSessionImpl.updateSDP: " + sdp);
        if (mediaConnection != null && !sdp.isEmpty()) {
            boolean isScreenSharingEnabled = deviceRegistration.getFeatures().isScreenSharingEnabled();
            WmeSdpParsedInfo[] sdpParsedInfoList = mediaConnection.setReceivedSDP(MediaConnection.SDPType.Answer, sdp);

            for (WmeSdpParsedInfo sdpParsedInfo : sdpParsedInfoList) {
                MediaConnection.MediaDirection mediaDirection = sdpParsedInfo.remoteNegotiatedDirection;
                long mid = sdpParsedInfo.mid;
                if (mid == MediaEngine.VIDEO_MID) {
                    if (mediaDirection == MediaConnection.MediaDirection.SendRecv) {
                        trackVideoLocal.Start(isVideoMuted());
                    } else if (mediaDirection == MediaConnection.MediaDirection.Inactive) {
                        trackVideoLocal.Stop();
                    }
                } else if (mid == MediaEngine.AUDIO_MID) {
                    if (mediaDirection == MediaConnection.MediaDirection.SendRecv) {
                        trackAudioLocal.Start();
                    } else if (mediaDirection == MediaConnection.MediaDirection.Inactive) {
                        trackAudioLocal.Stop();
                    }
                } else if (mid == MediaEngine.SHARE_MID) {
                    if (mediaDirection == MediaConnection.MediaDirection.RecvOnly) {
                        screenTrack.Start();
                    } else if (mediaDirection == MediaConnection.MediaDirection.SendOnly) {
                        shareTrack.Start();
                    } else if (mediaDirection == MediaConnection.MediaDirection.SendRecv) {
                        screenTrack.Start();
                        shareTrack.Start();
                    } else if (mediaDirection == MediaConnection.MediaDirection.Inactive) {
                        if (isScreenSharingEnabled) {
                            screenTrack.Stop();
                        }
                        shareTrack.Stop();
                    }
                }
            }
        }
    }


    @Override
    public String getLocalSdp() {
        return localSdp;
    }


    private void setupMediaParameters() {
        MediaConfig.AudioConfig audioConfig = mediaConnection.GetAudioConfig(MediaEngine.AUDIO_MID);

        // Set custom Audio Codec (if other than default selected in dev console)....otherwise default to Opus
        MediaConfig.WmeCodecType selectedCodecType = MediaConfig.WmeCodecType.WmeCodecType_OPUS;
        MediaConfig.WmeCodecType customCodecType = normalizeAudioCodec(settings.getAudioCodec());
        if (!customCodecType.equals(MediaConfig.WmeCodecType.WmeCodecType_Unknown)) {
            selectedCodecType = customCodecType;
        }
        if (!deviceRegistration.getFeatures().isMediaAudioAllCodecsEnabled()) {
            audioConfig.SetSelectedCodec(selectedCodecType);
        }
        audioConfig.SetPreferedCodec(selectedCodecType);

        // always enable audio FEC
        audioConfig.EnableFec(true);
        audioConfig.EnableRecordLossData(deviceRegistration.getFeatures().isCallLossRecordAudioEnabled());

        // turn off local audio mixing for now (until we know the impact of increased CPU and bandwidth usage)
        audioConfig.EnableClientMix(1);

        MediaConfig.VideoConfig videoConfig = mediaConnection.GetVideoConfig(MediaEngine.VIDEO_MID);

        // video FEC needs to always be enabled for probing
        // but we can set FEC overhead to 0 to turn off video FEC for recovery as needed.
        videoConfig.EnableFec(true);
        if (!deviceRegistration.getFeatures().isCallFecVideoEnabled())
            videoConfig.SetMaxFecOverhead(0);

        videoConfig.EnableRecordLossData(deviceRegistration.getFeatures().isCallLossRecordVideoEnabled());

        // set video encode parameters
        videoConfig.SetPreferedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        videoConfig.SetSelectedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        videoConfig.SetPacketizationMode(MediaConfig.WmePacketizationMode.WmePacketizationMode_1);

        // adjust bandwidth for multistream to keep MARI library happy
        int bandwidth = 1500000;
        if (deviceRegistration.getFeatures().isMultistreamEnabled())
            bandwidth *= MediaEngine.MAX_NUMBER_STREAMS;
        videoConfig.SetMaxBandwidth(bandwidth);

        // check hardware codec settings
        hardwareCodecEnabled = checkHardwareCodecSettings(videoConfig);

        // set encoder codec params
        String profileID;
        int maxMbps, maxFs, maxBr, maxFps;
        if (hardwareCodecEnabled || settings.isSw720pEnabed())  { //720p
            profileID = "420014";
            maxMbps = 108000;
            maxFs = 3600;
            maxBr = 1500;
            maxFps = 3000;
        } else { // 360p
            profileID = "42000D";
            maxMbps = 27600;
            maxFs = 920;
            maxBr = 1000;
            maxFps = 3000;
        }

        MediaConfig.WmeVideoCodecCapability videoEncoderCodecCapability = new MediaConfig.WmeVideoCodecCapability();
        videoEncoderCodecCapability.uProfileLevelID = Long.parseLong(profileID, 16);
        videoEncoderCodecCapability.max_mbps = maxMbps;
        videoEncoderCodecCapability.max_fs = maxFs;
        videoEncoderCodecCapability.max_br = maxBr;
        videoEncoderCodecCapability.max_fps = maxFps;
        videoConfig.SetEncodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, videoEncoderCodecCapability);

        // set decoder codec params
        MediaConfig.WmeVideoCodecCapability videoDecoderCodecCapability = new MediaConfig.WmeVideoCodecCapability();
        videoDecoderCodecCapability.uProfileLevelID = Long.parseLong(profileID, 16);
        videoDecoderCodecCapability.max_mbps = maxMbps;
        videoDecoderCodecCapability.max_fs = 8160;  // use 1080p instead of 360p since we use same stream for receiving share
        videoDecoderCodecCapability.max_br = maxBr;
        videoDecoderCodecCapability.max_fps = maxFps;
        videoConfig.SetDecodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, videoDecoderCodecCapability);

        videoConfig.Disable90PVideo(true);
        videoConfig.EnableAVCSimulcast(simulcastEnabled);
        videoConfig.EnableSelfPreviewHorizontalMirror(true);


        srtpEnabled = !settings.isSrtpDisabled();

        // Override IP address
        boolean iceEnabled = true;
        String overrideMediaIPAddress = settings.getMediaOverrideIpAddress();
        if (overrideMediaIPAddress != null && !overrideMediaIPAddress.isEmpty()) {
            audioConfig.OverrideMediaIPAddress(overrideMediaIPAddress, MediaEngine.MEDIA_OVERRIDE_AUDIO_RECEIVE_ON_PORT);
            videoConfig.OverrideMediaIPAddress(overrideMediaIPAddress, MediaEngine.MEDIA_OVERRIDE_VIDEO_RECEIVE_ON_PORT);
            srtpEnabled = false; // disable SRTP if using IP address override
            iceEnabled = false;
        }


        // set global media params
        MediaConfig.GlobalConfig globalConfig = mediaConnection.GetGlobalConfig();
        globalConfig.EnableICE(iceEnabled);
        globalConfig.EnableSRTP(srtpEnabled);
        globalConfig.EnableQos(qosEnabled);


        // enable multistream and set max number of video streams we can receive
        globalConfig.EnableMultiStream(true);

        if (deviceRegistration.getFeatures().isMultistreamEnabled()) {
            maxStreamCount = MediaEngine.MAX_NUMBER_STREAMS;
        } else {
            maxStreamCount = 1;
        }
        videoConfig.SetInitSubscribeCount(maxStreamCount);

        // enable performance statistics trace dump?
        globalConfig.EnablePerformanceTraceDump(
                settings.isPerformanceStatsEnabled() ? MediaConfig.WmePerformanceDumpType.WmePerformanceDumpAll : MediaConfig.WmePerformanceDumpType.WmePerformanceDumpNone);

        //if enable performance stats, then DISABLE QoS for reproductive testing
        globalConfig.EnableQos(!settings.isPerformanceStatsEnabled() && qosEnabled);

        // Playback from audio/video files
        if (audioPlaybackFilePath.length() > 0) {
            audioConfig.EnableFileCapture(audioPlaybackFilePath, true);
        }
        if (videoPlaybackFilePath.length() > 0) {
            videoConfig.EnableFileCapture(videoPlaybackFilePath, true);
        }

        // QoS parameters
        int warningWaitMs = 5000;
        int disableVideoWaitMs = 7000;
        int recoverWaitMs = 10000;
        globalConfig.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_bad, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, warningWaitMs);
        globalConfig.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_video_off, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, disableVideoWaitMs);
        globalConfig.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_recovered, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, recoverWaitMs);

        float maxLossRatio = 0.08f;
        globalConfig.SetQoSMaxLossRatio(maxLossRatio);
    }

    private boolean checkHardwareCodecSettings(MediaConfig.VideoConfig videoConfig) {
        // enable hardware codec?
        // note that this is based on combination of associated feature toggle option being enabled and also if call to EnableHWAcceleration was successful
        boolean isHardwareCodecFeatureToggleEnabled = deviceRegistration.getFeatures().isHardwareCodecEnabled();

        // has setting been overridden in dev console
        if (settings.isForceHardwareCodecEnabled()) {
            isHardwareCodecFeatureToggleEnabled = true;
            mediaConnection.GetGlobalConfig().SetDeviceMediaSettings(hwVideoSetting);
        }

        long hardwareEncoderEnabled = videoConfig.EnableHWAcceleration(isHardwareCodecFeatureToggleEnabled, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Encoder);
        long hardwareDecoderEnabled = videoConfig.EnableHWAcceleration(isHardwareCodecFeatureToggleEnabled, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Decoder);
        return isHardwareCodecFeatureToggleEnabled && WmeError.Succeeded(hardwareEncoderEnabled) && WmeError.Succeeded(hardwareDecoderEnabled);
    }


    @Override
    public synchronized void startMedia() {
        Ln.d("MediaSessionImpl.startMedia(), this = " + this + ", media connection = " + mediaConnection);
        mediaStarted = true;
        bus.post(new MediaStartedEvent());
        alreadyWarnedNetworkCongestion = false;


        // schedule timer to log media statistics
        if (enableStatistics && statisticsTimer == null) {
            statisticsTimer = new Timer("WMEMediaEngine timer");
            statisticsTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getStats();
                }
            }, 5000, 1000);
        }
    }

    @Override
    public synchronized void stopMedia() {
        Ln.d("MediaSessionImpl.stopMedia(), mediaStarted = " + mediaStarted + ", this = " + this + ", media connection = " + mediaConnection);
        bus.post(new MediaStoppingEvent());

        if (mediaConnection != null && mediaStarted) {

            // remove preview/remote render views
            setPreviewWindow(null);
            removeRemoteVideoWindows();

            mediaStarted = false;

            if (statisticsTimer != null) {
                statisticsTimer.cancel();
                statisticsTimer = null;
            }

            mediaConnection.stopMediaLogging();

            // get packet stats
            MediaStatistics.AudioStatistics audioStats = mediaConnection.getAudioStatistics(MediaEngine.AUDIO_MID);
            MediaStatistics.VideoStatistics videoStats = mediaConnection.getVideoStatistics(MediaEngine.VIDEO_MID);
            packetStats = new PacketStats(audioStats.mConnection.uRTPSent, audioStats.mConnection.uRTPReceived,
                    videoStats.mConnection.uRTPSent, videoStats.mConnection.uRTPReceived);

            if (trackVideoLocal != null) {
                Ln.d("MediaSessionImpl.stopMedia(), stopping local video track");
                trackVideoLocal.Stop();
            }
            for (MediaTrack trackVideoRemote : remoteVideoTracks.values()) {
                Ln.d("MediaSessionImpl.stopMedia(), stopping remote video track");
                trackVideoRemote.Stop();
            }
            if (trackAudioLocal != null) {
                Ln.d("MediaSessionImpl.stopMedia(), remove potential external renderer and stopping local audio track");
                trackAudioLocal.removeAuidoPairingExternalRenderer();
                trackAudioLocal.Stop();
            }
            if (trackAudioRemote != null) {
                Ln.d("MediaSessionImpl.stopMedia(), stopping remote audio track");
                trackAudioRemote.Stop();
            }
            if (shareTrack != null) {
                Ln.d("MediaSessionImpl.stopMedia(), stopping local share track");
                shareTrack.Stop();
            }
        }

        Ln.d("MediaSessionImpl.stopMedia() end");
    }

    @Override
    public synchronized void restartMedia() {
        Ln.d("MediaSessionImpl.restartMedia(), mediaStarted = " + mediaStarted);

        mediaStarted = true;

        if (trackVideoLocal != null) {
            trackVideoLocal.Start();
        }
        for (MediaTrack trackVideoRemote : remoteVideoTracks.values()) {
            trackVideoRemote.Start();
        }
        if (trackAudioLocal != null) {
            trackAudioLocal.Start();
        }
        if (trackAudioRemote != null) {
            trackAudioRemote.Start();
        }
        if (shareTrack != null) {
            shareTrack.Start();
        }

        mediaConnection.startMediaLogging(1000);

        Ln.d("MediaSessionImpl.restartMedia() end");
    }

    @Override
    public boolean isMediaStarted() {
        return mediaStarted;
    }

    @Override
    public boolean isCompositedVideoStream() {
        return isCompositedVideoStream;
    }

    private MediaConfig.WmeCodecType normalizeAudioCodec(String audioCodec) {
        for (MediaConfig.WmeCodecType codecType : MediaConfig.WmeCodecType.values()) {
            if (audioCodec.equalsIgnoreCase(codecType.name())) {
                return codecType;
            }
        }
        return MediaConfig.WmeCodecType.WmeCodecType_Unknown;
    }

    @Override
    public boolean simulcastEnabled() {
        return simulcastEnabled;
    }

    @Override
    public boolean wasMediaFlowing() {
        return isReceivedFirstAudioPacket() || isReceivedFirstVideoPacket()
                || isSentFirstAudioPacket() || isSentFirstVideoPacket();

    }

    @Override
    public boolean usedTcpFallback() {
        return usedTcpFallback;
    }



    @Override
    public void setMaxStreamCount(int newMaxStreamCount) {
        Ln.d("MediaSessionImpl.setMaxStreamCount() maxStreamCount = " + maxStreamCount);

        if (newMaxStreamCount != this.maxStreamCount) {

            // unsubscribe to streams no longer required
            for (int vid = newMaxStreamCount; vid < this.maxStreamCount; vid++) {
                mediaConnection.unsubscribe(MediaEngine.VIDEO_MID, vid);
            }

            // subscribe to any additional streams requested (all are 360p)
            MediaEngine.VideoSCRParams p = videoScr360p;
            for (int vid = this.maxStreamCount; vid < newMaxStreamCount; vid++) {
                try {
                    mediaConnection.subscribe(MediaEngine.VIDEO_MID, MediaConfig.WmeSubscribePolicy.ActiveSpeaker, p.maxFs, p.maxFps,
                            p.maxBr, p.maxDpb, p.maxMbps, p.priority, p.grouping, p.duplicate, 0);
                } catch (IllegalArgumentException e) {
                    Ln.e(e, "error calling MediaConnection subscribe");
                }
            }

            this.maxStreamCount = newMaxStreamCount;
        }
    }

    @Override
    public int getMaxStreamCount() {
        return maxStreamCount;
    }


    @Override
    public void onMediaBlocked(int mid, int vid, boolean blocked) {
        Ln.d("MediaSessionImpl.onMediaBlocked, mid=" + mid + ", vid = " + vid + ", blocked=" + blocked);
        if ((isVideo(mid) || isShare(mid)) && isActiveSpeaker(vid) && mediaSessionCallbacks != null) {
            com.cisco.spark.android.media.MediaType mediaType;
            if (isVideo(mid)) {
                mediaType = com.cisco.spark.android.media.MediaType.VIDEO;
            } else {
                mediaType = com.cisco.spark.android.media.MediaType.CONTENT_SHARE;
            }
            mediaSessionCallbacks.onMediaBlocked(callId, mediaType, blocked);
        }
        bus.post(new MediaBlockedChangeEvent(mid, vid, blocked));
    }


    @Override
    public void onRenderSizeChanged(int var1, int var2, MediaConnection.MediaDirection var3, MediaConnection.WmeVideoSizeInfo var4) {
        Ln.d("MediaSessionImpl.onRenderSizeChanged");
    }

    @Override
    public void onEncodeSizeChanged(int mid, int width, int height) {
        Ln.d("MediaSessionImpl.onEncodeSizeChanged, mid = " + mid + ", width = " + width + ", height = ", height);
    }


    @Override
    public void onMediaError(int mid, int vid, int errorCode) {
        Ln.d("MediaSessionImpl.onMediaError, vid = " + vid + ", mid = " + mid + ", errorCode = " + errorCode);

    }

    @Override
    public void onAvailableMediaChanged(int mid, int count) {
        Ln.d("MediaSessionImpl.onAvailableMediaChanged, mid = " + mid + ", count = " + count);
        if (mid == MediaEngine.SHARE_MID) {
            if (count == 0) {
                saveShareFrame();
            } else {
                cleanShareFrame();
            }
        }
        bus.post(new AvailableMediaChangeEvent(mid, count));
    }


    @Override
    public void onError(int errorCode) {
        Ln.d("MediaSession.onError, code = " + errorCode);

        if (!iceFailed && errorCode == WmeError.E_ICE_SERVER_REJECTED) {
            serverRejected = true;
        } else if (errorCode == WmeError.E_VIDEO_RENDERTHREAD_GL) {
            if (trackVideoLocal != null) {
                Ln.d("MediaSessionImpl.onError(), stopping local video track");
                trackVideoLocal.Stop();
            }
        }
    }

    @Override
    public void OnCSIsChanged(long mid, long vid, long[] oldCSIs, long[] newCSIs) {
        if (isVideo(mid)) {
            MediaTrack remoteTrack = remoteVideoTracks.get(vid);
            Ln.d("MediaSessionImpl.OnCSIsChanged (VIDEO), vid = " + vid + ", oldCSIs = " + Arrays.toString(oldCSIs) + ", newCSIs = " + Arrays.toString(newCSIs)
                    + " track = ", MediaHelper.formatMediaTrack(remoteTrack));

            if (oldCSIs.length > 0) {
                long oldVideoCSI = oldCSIs[0];
                View view = csiViewMap.get(oldVideoCSI);
                if (view != null) {
                    remoteTrack.removeRenderWindow(view);
                }
                csiTrackMap.remove(oldVideoCSI);
            }

            if (newCSIs.length > 0 && remoteTrack != null) {
                long videoCSI = newCSIs[0];

                csiTrackMap.put(videoCSI, remoteTrack);

                View view = csiViewMap.get(videoCSI);
                if (view != null) {
                    remoteTrack.addRenderWindow(view);
                    setRemoteRenderMode(remoteTrack, MediaEngine.VIDEO_MID);
                }
            }
        } else if (mid == MediaEngine.SHARE_MID) {
            Ln.d("MediaSessionImpl.OnCSIsChanged (SHARE), vid = " + vid + ", oldCSIs = " + Arrays.toString(oldCSIs) + ", newCSIs = " + Arrays.toString(newCSIs));

            if (oldCSIs.length > 0) {
                // remove window handles
                if (shareView != null) {
                    shareTrack.removeRenderWindow(shareView);
                }
            }

            if (newCSIs.length > 0) {
                if (shareView != null) {
                    shareTrack.addRenderWindow(shareView);
                }
            }
        }


        bus.post(new MediaActiveSpeakerChangedEvent(callId, mid, vid, oldCSIs, newCSIs));
    }


    // ***************************************
    // Congestion/Network Connection Handling
    // ***************************************

    @Override
    public void onNetworkStatus(MediaConnection.NetworkStatus networkStatus, MediaConnection.NetworkDirection networkDirection) {
        Ln.d("MediaSessionImpl.onNetworkStatus, status = " + networkStatus.toString());

        switch (networkStatus) {
            case bad:
                if (!alreadyWarnedNetworkCongestion) {
                    alreadyWarnedNetworkCongestion = true;
                    bus.post(new NetworkCongestionEvent(callId));
                }
                break;
            case videoOff:
                if (networkDirection.equals(MediaConnection.NetworkDirection.Uplink) && !isVideoMuted()) {
                    bus.post(new NetworkDisableVideoEvent(callId));
                }
                break;
            case recovered:
                alreadyWarnedNetworkCongestion = false;
                break;
        }
    }

    @Override
    public void onSessionStatus(int mid, MediaConnection.MediaType mediaType, MediaConnection.ConnectionStatus status) {
        Ln.v("MediaSessionImpl.onSessionStatus: mid=" + mid + ", mediaType=" + mediaType + ", ConnectionStatus=" + status);
        if (status == MediaConnection.ConnectionStatus.Sent) {
            if (mediaType == MediaConnection.MediaType.Audio)
                setFirstAudioPacketSent(new Date());
            else if (mediaType == MediaConnection.MediaType.Video) {
                setFirstVideoPacketSent(new Date());
            } else if (mediaType == MediaConnection.MediaType.Sharing) {
                if (mediaSessionCallbacks != null) {
                    mediaSessionCallbacks.onFirstPacketTx(callId, com.cisco.spark.android.media.MediaType.CONTENT_SHARE);
                }
            }
        } else if (status == MediaConnection.ConnectionStatus.Received) {
            if (mediaType == MediaConnection.MediaType.Audio)
                setFirstAudioPacketReceived(new Date());
            else if (mediaType == MediaConnection.MediaType.Video) {
                setFirstVideoPacketReceived(new Date());
            } else if (mediaType == MediaConnection.MediaType.Sharing) {
                if (mediaSessionCallbacks != null) {
                    mediaSessionCallbacks.onFirstPacketRx(callId, com.cisco.spark.android.media.MediaType.CONTENT_SHARE);
                }
            }
        } else if (status == MediaConnection.ConnectionStatus.Connected) {
            if (!connected && mediaSessionCallbacks != null) {
                mediaSessionCallbacks.onICEComplete(callId);
            }

            connected = true;
            checkTcpFallback();
        } else if (status == MediaConnection.ConnectionStatus.Disconnected) {
            Ln.d("onNetworkDisconnect received");

            // if we get disconnected notification without having been already connected then
            // this indicates an ICE connection error
            if (!connected && !iceFailed) {
                iceFailed = true;

                if (mediaSessionCallbacks != null) {
                    mediaSessionCallbacks.onICEFailed(callId);
                }
            } else {
                isNetworkConnected.set(false);

                // For the first event
                if (networkConnectionHandler == null) {
                    // Signal that our network connection is down.  Could be: momentary disruption in network;
                    // switching from one network to another; or a permanent loss.  Time(r's) will tell...
                    int mediaReconnectTimeout = deviceRegistration.getFeatures().getMediaReconnectTimeout();
                    Ln.d("NetworkDisconnectEvent: Waiting %d secs for reconnection.", mediaReconnectTimeout);
                    bus.post(new NetworkDisconnectEvent(callId, getMediaTypeString(mediaType)));

                    networkConnectionHandler = new Handler(Looper.getMainLooper());
                    networkConnectionHandler.postDelayed(networkConnectionRunnable, mediaReconnectTimeout * 1000);
                }
            }
        } else if (status == MediaConnection.ConnectionStatus.Reconnected) {
            Ln.d("onNetworkReconnect received");
            if (networkConnectionHandler != null) {
                networkConnectionHandler.removeCallbacks(networkConnectionRunnable);
                networkConnectionHandler = null;
            }
            isNetworkConnected.set(true);
            bus.post(new NetworkReconnectEvent(callId, getMediaTypeString(mediaType)));
            checkTcpFallback();
        } else if (status == MediaConnection.ConnectionStatus.FileCaptureEnded) {

        }
    }


    Runnable networkConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            NetworkAsyncReconnectCheck networkAsyncReconnectCheck = new NetworkAsyncReconnectCheck();
            networkAsyncReconnectCheck.execute();
        }
    };

    @Override
    public boolean iceFailed() {
        return iceFailed;
    }

    @Override
    public boolean isServerRejected() {
        return serverRejected;
    }

    private void checkTcpFallback() {
        if (!usedTcpFallback && mediaConnection != null && mediaStarted) {
            MediaStatistics.AudioStatistics audio = mediaConnection.getAudioStatistics(MediaEngine.AUDIO_MID);
            MediaStatistics.VideoStatistics video = mediaConnection.getVideoStatistics(MediaEngine.VIDEO_MID);
            String audioConnectionType = audio.mConnection.connectionType;
            String videoConnectionType = video.mConnection.connectionType;
            if (MediaEngine.TCP_CONNECTION_TYPE.equals(audioConnectionType) || MediaEngine.TCP_CONNECTION_TYPE.equals(videoConnectionType)) {
                usedTcpFallback = true;
            }
        }
    }

    private class NetworkAsyncReconnectCheck extends SafeAsyncTask<Boolean> {

        @Override
        public Boolean call() throws Exception {
            if (!isNetworkConnected.get()) {
                Ln.i("NetworkLostEvent: No reconnect detected.");
                bus.post(new NetworkLostEvent(callId));
            } else {
                Ln.i("NetworkReconnectEvent: Network is reconnected.");
                bus.post(new NetworkReconnectEvent(callId));
            }
            networkConnectionHandler = null;
            return true;
        }

        @Override
        public void onException(Exception ex) {
            Ln.i("NetworkLostEvent: Some error occurred waiting for stability.");
            bus.post(new NetworkLostEvent(callId));
        }
    }




    @Override
    public synchronized void onMediaStatus(int mid, int vid, MediaConnection.MediaStatus mediaStatus, boolean hasCSI, long csi) {
        Ln.d("MediaSessionImpl.onMediaStatus, mid = %s (%d) vid = %d (%s) status = %s hasCsi = %s csi = %d",
                getMediaTypeString(mid), mid, vid, getVideoStreamString(vid), mediaStatus, hasCSI, csi);

        if (isVideo(mid) || isShare(mid)) {
            if (isActiveSpeaker(vid)) {

                // Don't update active speaker due to unavailable media.
                if (hasCSI && mediaStatus != MediaConnection.MediaStatus.ERR_TEMP_UNAVAIL_NO_MEDIA) {
                    bus.post(new MediaActiveSpeakerChangedEvent(callId, mid, vid, new long[]{}, new long[]{csi}));
                }

                switch (mediaStatus) {
                    case ERR_TEMP_UNAVAIL_NO_MEDIA: {
                        // Active speaker muted video
                        bus.post(newVideoMuteWithCsiEvent(callId, csi, vid));
                        onMediaBlocked(mid, vid, true);
                        break;
                    }
                    case Available: {
                        // Active speaker turned on video
                        bus.post(newVideoOnWithCsiEvent(callId, csi, vid));
                        break;
                    }
                    default: {
                        // ignore currently
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onDecodeSizeChanged(int mid, int vid, int width, int height) {
        Ln.d("MediaSessionImpl.onDecodeSizeChanged, mid = " + mid + ", vid = " + vid + ", width = " + width + ", height = " + height);

        Rect newSize = new Rect(0, 0, width, height);

        if (mid == MediaEngine.VIDEO_MID) {
            decodeSizes.put(vid, newSize);

            MediaTrack remoteTrack = remoteVideoTracks.get((long) vid);
            if (remoteTrack != null) {
                setRemoteRenderMode(remoteTrack, MediaEngine.VIDEO_MID);
            }

            if (!isSharingAsViewer()) {
                bus.post(new MediaDecodeSizeChangedEvent(vid, newSize));
            }
        } else if (mid == MediaEngine.SHARE_MID) {
            decodeSizeShare = newSize;
            if (isSharingAsViewer()) {
                bus.post(new MediaDecodeSizeChangedEvent(vid, decodeSizeShare));
            }
        }
    }


    @Override
    public Rect getVideoSize(int vid) {
        return decodeSizes.get(vid);
    }

    @Override
    public Rect getFullsceenVideoSize() {
        if (isSharingAsViewer()) {
            return decodeSizeShare;
        }
        return getVideoSize((int) MAIN_VIDEO_ID);
    }


    // ***************************************
    // Rendering
    // ***************************************

    private MediaTrack.ScalingMode getShareScreenVideoRenderMode(MediaTrack track) {
        if (!isDisplayActiveSpeakerWindow() || shareView == null) {
            return getRemoteVideoRenderMode(track);
        }

        View containerView = shareView;
        if (containerView == null || track == null) {
            return MediaTrack.ScalingMode.CropFill;
        }

        long containerViewWidth  = containerView.getWidth();
        long containerViewHeight = containerView.getHeight();
        boolean isContainerViewPortrait = containerViewHeight > containerViewWidth;

        long decodedWidth  = decodeSizeShare.width();
        long decodedHeight = decodeSizeShare.height();
        boolean receivingPortrait = decodedHeight > decodedWidth;

        if ((isContainerViewPortrait && receivingPortrait) || (!isContainerViewPortrait && !receivingPortrait)) {
            Ln.d("getShareScreenVideoRenderMode, mode = CROP_FILL");
            return MediaTrack.ScalingMode.CropFill;
        }

        Ln.d("getShareScreenVideoRenderMode, mode = LETTER_BOX");
        return MediaTrack.ScalingMode.LetterBox;
    }

    private MediaTrack.ScalingMode getRemoteVideoRenderMode(MediaTrack track) {
        View containerView = vidViewMap.get(MAIN_VIDEO_ID);
        if (containerView == null || track == null) {
            return MediaTrack.ScalingMode.CropFill;
        }

        long decodedHeight = track.getVideoTrackStatistics().uHeight;
        long decodedWidth  = track.getVideoTrackStatistics().uWidth;
        boolean receivingPortrait = decodedHeight > decodedWidth;

        int orientation = context.getResources().getConfiguration().orientation;
        if (deviceRegistration.getFeatures().isActiveSpeakerViewEnabled()) {
            if ((orientation == Configuration.ORIENTATION_PORTRAIT && receivingPortrait) ||
                    (orientation == Configuration.ORIENTATION_LANDSCAPE && !receivingPortrait)) {
                Ln.d("getRemoteVideoRenderMode, mode = CROP_FILL");
                return MediaTrack.ScalingMode.CropFill;
            }
        } else {
            if (orientation == Configuration.ORIENTATION_PORTRAIT && receivingPortrait) {
                Ln.d("getRemoteVideoRenderMode, mode = CROP_FILL");
                return MediaTrack.ScalingMode.CropFill;
            }
        }
        Ln.d("getRemoteVideoRenderMode, mode = LETTER_BOX");
        return MediaTrack.ScalingMode.LetterBox;
    }

    private MediaTrack.ScalingMode getActiveSpeakerVideoRenderMode() {
        // display the active speaker video with cropfill mode.
        return MediaTrack.ScalingMode.CropFill;
    }

    public synchronized void setRemoteRenderMode(MediaTrack track, int mediaType) {
        Ln.d("setRemoteRenderMode, mediaType = " + mediaType);
        if (track != null) {
            try {
                MediaTrack.ScalingMode mode;
                if (!deviceRegistration.getFeatures().isMultistreamEnabled()) {
                    if (isDisplayActiveSpeakerWindow()) {
                        if (mediaType == MediaEngine.VIDEO_MID) {
                            mode = getActiveSpeakerVideoRenderMode();
                        } else if (mediaType == MediaEngine.SHARE_MID) {
                            mode = getShareScreenVideoRenderMode(track);
                        } else {
                            mode = MediaTrack.ScalingMode.CropFill;
                        }
                    } else {
                        mode = getRemoteVideoRenderMode(track);
                    }
                } else {
                    mode = MediaTrack.ScalingMode.CropFill;
                }

                Ln.d("MediaSessionEngine.setRemoteRenderMode(): setting mode to " + mode);
                track.SetRenderMode(mode);

            } catch (IllegalArgumentException ex) {
                Ln.e(false, ex, "MediaSessionEngine.setRemoteRenderMode(): error getting video statistics, to determine render mode");
            }
        }
    }

    @Override
    public synchronized void setRemoteWindow(View view) {
        // set window handle for main video stream
        setRemoteWindowForVid(MAIN_VIDEO_ID, view);
    }

    @Override
    public synchronized void removeRemoteWindow(View view) {
        // remove window handle for main video stream
        removeRemoteWindowForVid(MAIN_VIDEO_ID, view);
    }

    @Override
    public synchronized void setActiveSpeakerWindow(View view) {
        Ln.d("MediaSessionImpl.setActiveSpeakerWindow");
        // set window handle for active speaker video stream
        activeSpeakerView = view;

        if (isDisplayActiveSpeakerWindow()) {
            switchSingleVideoViewToShare(asShareViewer);
        }
    }

    @Override
    public synchronized void removeActiveSpeakerWindow() {
        Ln.d("MediaSessionImpl.removeActiveSpeakerWindow() : asShareViewer = " + asShareViewer);
        switchSingleVideoViewToShare(asShareViewer);
        activeSpeakerView = null;
    }

    public synchronized void setRemoteWindowForVid(long vid, View view) {
        Ln.d("MediaSessionImpl.setRemoteWindowForVid(), vid = " + vid + ", handle = " + view);

        // Track the videoId for the remoteWindowView
        vidViewMap.put(vid, view);

        //single video video shared between active video and share
        if (!deviceRegistration.getFeatures().isMultistreamEnabled() && isSharingAsViewer()) {
            Ln.d("MediaSessionImpl.setRemoteWindowForVid(), switchSingleVideoViewToShare(true)");
            switchSingleVideoViewToShare(true);
            return;
        }

        MediaTrack remoteVideoTrack = remoteVideoTracks.get(vid);
        if (remoteVideoTrack != null) {
            Ln.d("MediaSessionImpl.setRemoteWindowForVid(), to remoteVideoTrack, view = " + view);
            remoteVideoTrack.addRenderWindow(view);
            setRemoteRenderMode(remoteVideoTrack, MediaEngine.VIDEO_MID);
        }
    }


    public synchronized void removeRemoteWindowForVid(long vid, View view) {
        Ln.d("MediaSessionImpl.removeRemoteWindowForVid(), vid = " + vid + ", mediaStarted = " + mediaStarted);

        if (mediaStarted) {
            //single video video shared between active video and share
            if (!deviceRegistration.getFeatures().isMultistreamEnabled() && isSharingAsViewer()) {
                if (shareTrack != null) {
                    Ln.d("MediaSessionImpl.removeRemoteWindowForVid(), from shareTrack, view = " + view);
                    shareTrack.removeRenderWindow(view);
                    vidViewMap.remove(vid);
                }
            }

            MediaTrack remoteVideoTrack = remoteVideoTracks.get(vid);
            if (remoteVideoTrack != null) {
                Ln.d("MediaSessionImpl.removeRemoteWindowForVid(), from remoteVideoTrack, view = " + view);
                remoteVideoTrack.removeRenderWindow(view);
                vidViewMap.remove(vid);
            }
        }
    }


    @Override
    public synchronized void setRemoteWindow(Long csi, View view) {
        Ln.d("MediaSessionImpl.setRemoteWindow(), csi = " + csi, ", handle = " + view);
        csiViewMap.put(csi, view);

        if (!deviceRegistration.getFeatures().isMultistreamEnabled()) {
            Ln.e("MediaSessionImpl.setRemoteWindow(Long csi, View view), should be used when enable isMultistreamEnabled");
        }

        MediaTrack remoteVideoTrack = csiTrackMap.get(csi);
        if (remoteVideoTrack != null) {
            Ln.d("MediaSessionImpl.setRemoteWindow(), to remoteVideoTrack, view = " + view);
            remoteVideoTrack.addRenderWindow(view);
            setRemoteRenderMode(remoteVideoTrack, MediaEngine.VIDEO_MID);
        }
    }


    @Override
    public synchronized void setShareWindow(View view) {
        Ln.d("MediaSessionImpl.setShareWindow()");
        shareView = view;
        if (shareTrack != null) {
            shareTrack.addRenderWindow(view);
        }
    }

    @Override
    public void grabShareView(WseSurfaceView.FrameSaved callback) {
        Ln.d("MediaSessionImpl.grabShareView()");
        if (!isSharingAsViewer()) {
            Ln.w("MediaSessionImpl.grabShareView() no sharing now");
            callback.Done(null);
            return;
        }

        View currentShareView = null;
        if (shareView != null) {
            currentShareView = shareView;
        } else if (!deviceRegistration.getFeatures().isMultistreamEnabled()) {
            currentShareView = vidViewMap.get(MAIN_VIDEO_ID);
        }

        if (currentShareView != null && currentShareView instanceof WseSurfaceView) {
            WseSurfaceView wseSurfaceView = (WseSurfaceView) currentShareView;
            wseSurfaceView.saveFrame(callback);
        } else {
            callback.Done(null);
        }
    }

    @Override
    public File getLastShareFrame() {
        return lastShareContentFrame;
    }

    private void saveShareFrame() {
        grabShareView(bitmap -> {
            File tmpFile = null;
            try {
                tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".png");
                tmpFile.deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (ImageUtils.writeBitmap(tmpFile, bitmap)) {
                ln.d("MediaSessionImpl.saveShareFrame success");
                lastShareContentFrame = tmpFile;
            } else {
                ln.d("MediaSessionImpl.saveShareFrame failure");
            }
        });
    }

    private void cleanShareFrame() {
        if (lastShareContentFrame == null) {
            return;
        }
        lastShareContentFrame.delete();
        lastShareContentFrame = null;
    }

    @Override
    public synchronized void removeShareWindow() {
        Ln.d("MediaSessionImpl.removeShareWindow()");

        if (shareView != null && shareTrack != null) {
            shareTrack.removeRenderWindow(shareView);
        }
        shareView = null;
    }

    @Override
    public synchronized void removeRemoteWindow(Long csi, View view) {
        Ln.d("MediaSessionImpl.removeRemoteWindow(), csi = " + csi, ", handle = " + view);
        csiViewMap.remove(csi);

        MediaTrack remoteVideoTrack = csiTrackMap.get(csi);
        if (remoteVideoTrack != null) {
            Ln.d("MediaSessionImpl.removeRemoteWindow() from remoteVideoTrack, view = " + view);
            remoteVideoTrack.removeRenderWindow(view);
        }
    }


    @Override
    public synchronized void removeRemoteWindow(Long csi) {
        Ln.d("MediaSessionImpl.removeRemoteWindow(), csi = " + csi);
        View view = csiViewMap.get(csi);
        if (view != null) {
            MediaTrack remoteVideoTrack = csiTrackMap.get(csi);
            if (remoteVideoTrack != null) {
                remoteVideoTrack.removeRenderWindow(view);
            }
            csiViewMap.remove(csi);
        }
    }


    @Override
    public synchronized void removeRemoteVideoWindows() {
        Ln.d("MediaSessionImpl.removeRemoteVideoWindows()");
        for (Long vid : vidViewMap.keySet()) {
            View view = vidViewMap.get(vid);
            removeRemoteWindowForVid(vid, view);
        }
        for (Long csi : csiTrackMap.keySet()) {
            removeRemoteWindow(csi);
        }
    }

    @Override
    public synchronized void setPreviewWindow(View surface) {
        Ln.d("MediaSessionImpl.setPreviewWindow(), handle = " + surface);
        if (previewWindow != null && trackVideoLocal != null) {
            Ln.d("MediaSessionImpl.setPreviewWindow(), removeRenderWindow(), previewWindow = " + previewWindow);
            trackVideoLocal.removeRenderWindow(previewWindow);
        }
        previewWindow = surface;
        if (trackVideoLocal != null) {
            Ln.d("MediaSessionImpl.setPreviewWindow(), addRenderWindow(), previewWindow = " + previewWindow);
            trackVideoLocal.addRenderWindow(previewWindow);
            trackVideoLocal.SetRenderMode(MediaTrack.ScalingMode.CropFill);
        }
    }


    @Override
    public synchronized void startSelfView() {
        Ln.d("MediaSessionImpl.startSelfView() - " + selfViewStarted);
        if (!selfViewStarted) {
            if (mediaConnection == null) {
                mediaConnection = new MediaConnection();
                mediaConnection.setListener(this);

                if (deviceSettings != null && deviceSettings.trim().length() > 0) {
                    mediaConnection.GetGlobalConfig().SetDeviceMediaSettings(deviceSettings);
                }
            }

            // TODO shouldn't need to start audio here...
            mediaConnection.addMedia(MediaConnection.MediaType.Audio, MediaConnection.MediaDirection.SendOnly, MediaEngine.AUDIO_MID, "");
            mediaConnection.addMedia(MediaConnection.MediaType.Video, MediaConnection.MediaDirection.SendOnly, MediaEngine.VIDEO_MID, "");
            selfViewStarted = true;
        }
    }




    // ***************************************
    // Sharing
    // ***************************************

    void setupTracks(boolean isSharing) {
        Ln.i("MediaSessionImpl.setupTracks isSharing = " + isSharing);
        if (deviceRegistration.getFeatures().isMultistreamEnabled() ||
            !deviceRegistration.getFeatures().isActiveSpeakerViewEnabled()) {
            Ln.w("MediaSessionImpl.setupTracks, Either multi stream is enabled or active speaker view is disabled");
            return;
        }

        View viewSingleVideo = vidViewMap.get(MAIN_VIDEO_ID);
        MediaTrack remoteTrack = remoteVideoTracks.get(MAIN_VIDEO_ID);

        // When sharing is started or stopped, the destination views for remote tracks should be changed.
        // 1. Stop the remote track.
        // 2. Change the destination view
        //    - if sharing started, switch from video view to active speaker view.
        //    - if sharing stopped, switch back to video view from active speaker view.
        // 3. Restart the remote track.
        // 4. Set the display mode of the destination view.
        if (remoteTrack != null) {
            remoteTrack.Stop();

            View currentView = isSharing ? viewSingleVideo : activeSpeakerView;
            if (currentView != null) {
                remoteTrack.removeRenderWindow(currentView);
            }

            View nextView = isSharing ? activeSpeakerView : viewSingleVideo;
            if (nextView != null) {
                remoteTrack.addRenderWindow(nextView);
            }

            remoteTrack.Start();
            setRemoteRenderMode(remoteTrack, MediaEngine.VIDEO_MID);
        }

        // When sharing started, start the share track and set shareView as the dest view.
        // When sharing stopped, stop the share track and remove shareView as the dest view.
        if (shareTrack != null) {
            if (isSharing) {
                shareTrack.Start();
                if (shareView != null) {
                    shareTrack.addRenderWindow(shareView);
                }
                setRemoteRenderMode(shareTrack, MediaEngine.SHARE_MID);
            } else {
                shareTrack.Stop();
                if (shareView != null) {
                    shareTrack.removeRenderWindow(shareView);
                }
            }
        }

        // reset the video resolution of the active speaker video after stopping screen sharing
        if (!isSharing && remoteTrack != null) {
            setRemoteVideoResolution(remoteTrack);
        }
    }

    void switchSingleVideoViewToShare(boolean bShare) {
        Ln.i("MediaSessionImpl.switchSingleVideoViewToShare bShare=%s", bShare);
        if (deviceRegistration.getFeatures().isActiveSpeakerViewEnabled()) {
            setupTracks(bShare);
            return;
        }

        if (deviceRegistration.getFeatures().isMultistreamEnabled()) {
            Ln.w("MediaSessionImpl.switchSingleVideoViewToShare media-enable-filmstrip-android is enabled");
            return;
        }

        View viewSingleVideo = vidViewMap.get(MAIN_VIDEO_ID);
        if (viewSingleVideo == null) {
            Ln.w("MediaSessionImpl.switchSingleVideoViewToShare viewSingleVideo is null");
            return;
        }

        MediaTrack remoteTrack = remoteVideoTracks.get(MAIN_VIDEO_ID);

        if (bShare) {
            if (remoteTrack != null) {
                remoteTrack.removeRenderWindow(viewSingleVideo);
                remoteTrack.Stop();
            }
            if (shareTrack != null) {
                shareTrack.Start();
                shareTrack.addRenderWindow(viewSingleVideo);
                setRemoteRenderMode(shareTrack, MediaEngine.SHARE_MID);
            }
        } else {
            if (shareTrack != null) {
                shareTrack.removeRenderWindow(viewSingleVideo);
                shareTrack.Stop();
            }

            if (remoteTrack != null) {
                remoteTrack.Start();
                remoteTrack.addRenderWindow(viewSingleVideo);
                setRemoteRenderMode(remoteTrack, MediaEngine.VIDEO_MID);

                // reset the video resolution of the active speaker video after stopping screen sharing
                setRemoteVideoResolution(remoteTrack);
            }
        }
    }

    void setRemoteVideoResolution(MediaTrack remoteTrack) {
        if (remoteTrack == null) {
            return;
        }

        // reset the video resolution of the active speaker video after stopping screen sharing
        if (hardwareCodecEnabled || settings.isSw720pEnabed()) {
            requestRemoteVideo(remoteTrack, videoScr720p);
        } else {
            requestRemoteVideo(remoteTrack, videoScr360p);
        }
    }

    @Override
    public void joinShare(String shareId) {
        Ln.i("MediaSessionImpl.joinShare shareId=%s", shareId);
        asShareViewer = true;
        //single video video shared between active video and share
        switchSingleVideoViewToShare(true);
    }

    @Override
    public void leaveShare(String shareId) {
        Ln.i("MediaSessionImpl.leaveShare shareId=%s", shareId);
        asShareViewer = false;
        //single video video shared between active video and share
        switchSingleVideoViewToShare(false);
    }

    @Override
    public void startScreenShare(String shareId) {
        if (screenTrack != null) {
            screenTrack.Start();
            screenSharing = true;
        }
    }

    @Override
    public void stopScreenShare(String shareId) {
        if (screenTrack != null) {
            screenTrack.Stop();
            if (screenSharing) {
                ScreenShareContext.getInstance().finit();
            }
            screenSharing = false;
        }
    }

    @Override
    public void updateShareId(String shareId) {
        //set shareId to WME for metrics and trace trouble shooting.
        currentShareId = shareId;

        if (screenTrack != null) {
            screenTrack.SetScreenSharingID(currentShareId);
        }

        if (shareTrack != null) {
            shareTrack.SetScreenSharingID(currentShareId);
        }
    }

    @Override
    public boolean isScreenSharing() {
        return screenSharing;
    }

    @Override
    public void onShareStopped() {
        if (mediaSessionCallbacks != null) {
            mediaSessionCallbacks.onShareStopped(callId);
        }
    }

    // TODO we might need to rename this now that we're getting capability
    // to share from android....this shold probably be isReceivingShare?
    boolean isSharing() {
        boolean result = (currentShareId.length() > 0);
        Ln.d("MediaSessionImpl.isSharing() = %b", result);
        return result;
    }

    boolean isSharingAsViewer() {
        Ln.d("MediaSessionImpl.isSharingAsViewer() = %b", asShareViewer);
        return asShareViewer;
    }

    // ***************************************
    // Statistics
    // ***************************************

    @Override
    public MediaStats getStats() {
        if (mediaConnection != null && mediaStarted) {
            MediaStatistics.AudioStatistics audio = mediaConnection.getAudioStatistics(MediaEngine.AUDIO_MID);
            MediaStatistics.VideoStatistics video = mediaConnection.getVideoStatistics(MediaEngine.VIDEO_MID);
            MediaStats mediaStats = new MediaStats();
            mediaStats.getAudio().getTx().setChannelStats(audio.mLocalSession.uBytes, audio.mLocalSession.uPackets,
                    audio.mLocalSession.fLossRatio, audio.mLocalSession.uJitter, audio.mLocalSession.uRoundTripTime,
                    audio.mLocalSession.uBitRate);
            mediaStats.getAudio().getRx().setChannelStats(audio.mRemoteSession.uBytes, audio.mRemoteSession.uPackets,
                    audio.mRemoteSession.fLossRatio, audio.mRemoteSession.uJitter, audio.mRemoteSession.uRoundTripTime,
                    audio.mRemoteSession.uBitRate);

            mediaStats.getVideo().getTx().setChannelStats(video.mLocalSession.uBytes, video.mLocalSession.uPackets,
                    video.mLocalSession.fLossRatio, video.mLocalSession.uJitter, video.mLocalSession.uRoundTripTime,
                    video.mLocalSession.uBitRate);
            mediaStats.getVideo().getRx().setChannelStats(video.mRemoteSession.uBytes, video.mRemoteSession.uPackets,
                    video.mRemoteSession.fLossRatio, video.mRemoteSession.uJitter, video.mRemoteSession.uRoundTripTime,
                    video.mRemoteSession.uBitRate);

            mediaStats.getVideo().getTx().setVideoChannelStats(video.mLocal.uWidth, video.mLocal.uHeight,
                    (long) video.mLocal.fFrameRate, video.mLocal.uIDRSentRecvNum);
            mediaStats.getVideo().getRx().setVideoChannelStats(video.mRemote.uWidth, video.mRemote.uHeight,
                    (long) video.mRemote.fFrameRate, video.mRemote.uIDRSentRecvNum);

            if ("low".equals(settings.getNetworkCongestion()) || "high".equals(settings.getNetworkCongestion()))
                mediaStats = getDistortedMediaStats(mediaStats);
            if (statisticsUIHandler != null) {
                Message statsMsg = statisticsUIHandler.obtainMessage();
                statsMsg.obj = mediaStats;
                statsMsg.sendToTarget();
            }

            return mediaStats;
        }

        return null;
    }

    @Override
    public String getPacketStats() {
        String packetStatsJsonString = "";

        if (mediaConnection != null && mediaStarted) {
            MediaStatistics.AudioStatistics audioStats = mediaConnection.getAudioStatistics(MediaEngine.AUDIO_MID);
            MediaStatistics.VideoStatistics videoStats = mediaConnection.getVideoStatistics(MediaEngine.VIDEO_MID);
            packetStats = new PacketStats(audioStats.mConnection.uRTPSent, audioStats.mConnection.uRTPReceived,
                    videoStats.mConnection.uRTPSent, videoStats.mConnection.uRTPReceived);
        }

        if (packetStats != null) {
            packetStatsJsonString = gson.toJson(packetStats);
        }
        return packetStatsJsonString;
    }


    @Override
    public String getSessionStats() {
        if (mediaConnection != null) {
            // get latest update
            sessionStats = mediaConnection.getMediaSessionMetrics();
        }
        return sessionStats;
    }



    @Override
    public void setStatisticsUIHandler(Handler statisticsUIHandler) {
        this.statisticsUIHandler = statisticsUIHandler;
    }

    private MediaStats getDistortedMediaStats(MediaStats mediaStats) {
        boolean largeDistortion = "high".equals(settings.getNetworkCongestion());

        Ln.d("Distorting MediaStat by a %s", largeDistortion ? "lot" : "little");
        mediaStats.getAudio().getTx().setRtt(largeDistortion ? CONGESTION_THRESHOLD_HI_AUDIO_RTT + 1 : CONGESTION_THRESHOLD_LO_AUDIO_RTT + 1);
        mediaStats.getVideo().getTx().setRtt(largeDistortion ? CONGESTION_THRESHOLD_HI_VIDEO_RTT + 1 : CONGESTION_THRESHOLD_LO_VIDEO_RTT + 1);
        mediaStats.getVideo().getTx().setBitrate(MIN_ADAPTATION_BITRATE);
        mediaStats.getVideo().getTx().setFrameWidth(MIN_VIDEO_WIDTH);
        mediaStats.getVideo().getTx().setFrameHeight(MIN_VIDEO_HEIGHT);

        return mediaStats;
    }

    private boolean isDisplayActiveSpeakerWindow() {
        // show active speaker view during the screen share
        return (deviceRegistration.getFeatures().isActiveSpeakerViewEnabled() && activeSpeakerView != null && isSharingAsViewer());
    }

    public void setFirstAudioPacketSent(Date date) {
        Ln.i("MediaSessionImpl.setFirstAudioPacketSent, date = " + date.getTime() + ", this = " + this);
        sentFirstAudioPacket = true;

        if (mediaSessionCallbacks != null) {
            mediaSessionCallbacks.onFirstPacketTx(callId, com.cisco.spark.android.media.MediaType.AUDIO);
        }
    }

    public void setFirstAudioPacketReceived(Date date) {
        Ln.i("MediaSessionImpl.setFirstAudioPacketReceived date = " + date.getTime());
        firstAudioPacketReceivedTime = date;
        receivedFirstAudioPacket = true;

        if (mediaSessionCallbacks != null) {
            mediaSessionCallbacks.onFirstPacketRx(callId, com.cisco.spark.android.media.MediaType.AUDIO);
        }
    }

    public void setFirstVideoPacketSent(Date date) {
        Ln.i("MediaSessionImpl.setFirstVideoPacketSent date = " + date.getTime());
        sentFirstVideoPacket = true;

        if (mediaSessionCallbacks != null) {
            mediaSessionCallbacks.onFirstPacketTx(callId, com.cisco.spark.android.media.MediaType.VIDEO);
        }
    }

    public void setFirstVideoPacketReceived(Date date) {
        Ln.i("MediaSessionImpl.setFirstVideoPacketReceived date = " + date.getTime());
        firstVideoPacketReceivedTime = date;
        receivedFirstVideoPacket = true;

        if (mediaSessionCallbacks != null) {
            mediaSessionCallbacks.onFirstPacketRx(callId, com.cisco.spark.android.media.MediaType.VIDEO);
        }
    }


    @Override
    public boolean isReceivedFirstAudioPacket() {
        return receivedFirstAudioPacket;
    }

    @Override
    public boolean isReceivedFirstVideoPacket() {
        return receivedFirstVideoPacket;
    }

    @Override
    public boolean isSentFirstAudioPacket() {
        return sentFirstAudioPacket;
    }

    @Override
    public boolean isSentFirstVideoPacket() {
        return sentFirstVideoPacket;
    }

    @Override
    public Date getFirstAudioPacketReceivedTime() {
        return firstAudioPacketReceivedTime;
    }

    @Override
    public Date getFirstVideoPacketReceivedTime() {
        return firstVideoPacketReceivedTime;
    }




    @Override
    public void setAudioDataListener(AudioDataListener listener) {
        // set audio callback (to allow checking for pairing data while on a call)
        Ln.d("setAudioPairingExternalRenderer listener trackAudioLocal = %s listener = %s", trackAudioLocal, listener);
        if (trackAudioLocal != null) {
            trackAudioLocal.addAuidoPairingExternalRenderer(new MediaAudioPairingExternalRender(listener));
        } else {
            Ln.w("Could not set external renderer, no local audio track");
        }
    }

    @Override
    public void clearAudioDataListener(AudioDataListener listener) {
        Ln.d("clearAudioDataListener()");
        if (trackAudioLocal != null) {
            trackAudioLocal.removeAuidoPairingExternalRenderer();
        } else {
            Ln.i("Could not clear external renderer, no local audio track");
        }
    }

    /**
     * check a list of csis against the csiTrackMap, return the first csi in the list that is tracked
     */
    @Override
    public Long checkCSIs(List<Long> csiList) {
        for (Long csi : csiList) {
            if (csiTrackMap.get(csi) != null) {
                return csi;
            }
        }
        return null;
    }

    @Override
    public void setAudioSampling(int duration) {
        if (mediaConnection != null) {
            mediaConnection.GetAudioConfig(MediaEngine.AUDIO_MID).EnableKeyDumpFiles(duration);
        }
    }

    public void setAudioPlaybackFile(String audioPlaybackFile) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/" + audioPlaybackFile);
            if (file.exists()) {
                audioPlaybackFilePath = file.getAbsolutePath();
            }
        }
    }

    public void setVideoPlaybackFile(String videoPlaybackFile) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/" + videoPlaybackFile);
            if (file.exists()) {
                videoPlaybackFilePath = file.getAbsolutePath();
            }
        }
    }


    // ***************************************
    // Mute/Unmute
    // ***************************************


    @Override
    public void muteAudio() {
        Ln.d("MediaSessionImpl.muteAudio()");

        if (trackAudioLocal != null) {
            trackAudioLocal.Mute();
        }
        audioMuted = true;
    }

    @Override
    public void unMuteAudio() {
        Ln.d("MediaSessionImpl.unMuteAudio()");

        if (trackAudioLocal != null) {
            trackAudioLocal.Unmute();
        }
        audioMuted = false;
    }

    @Override
    public boolean isAudioMuted() {
        // TODO - can we query this from WME
        return audioMuted;
    }


    @Override
    public void muteRemoteAudio() {
        Ln.d("MediaSessionImpl.muteRemoteAudio()");

        if (trackAudioRemote != null) {
            trackAudioRemote.Mute();
        }
        remoteAudioMuted = true;
    }

    @Override
    public void unMuteRemoteAudio() {
        Ln.d("MediaSessionImpl.unMuteRemoteAudio()");

        if (trackAudioRemote != null) {
            trackAudioRemote.Unmute();
        }
        remoteAudioMuted = false;
    }

    //sdk
    @Override
    public void muteRemoteVideo() {
        Ln.i("muteRemoteVideo");

        for (MediaTrack trackVideoRemote : remoteVideoTracks.values()) {
            Ln.i("muteRemoteVideo(), mute remote video track");
            trackVideoRemote.Mute();
        }

    }


    //sdk
    @Override
    public void unmuteRemoteVideo() {
        Ln.i("unmuteRemoteVideo");

        for (MediaTrack trackVideoRemote : remoteVideoTracks.values()) {
            Ln.i("unmuteRemoteVideo(), Unmute remote video track");
            trackVideoRemote.Unmute();
        }

    }

    @Override
    public boolean isRemoteAudioMuted() {
        return remoteAudioMuted;
    }


    @Override
    public void OnRequestAvatarForMute(boolean mute) {
        Ln.d("MediaSessionImpl.OnRequestAvatarForMute(), mute = " + mute);
    }


    @Override
    public void setAudioVolume(int volume) {
        Ln.d("MediaSessionImpl.setAudioVolume(), volume = %d", volume);

        if (trackAudioRemote != null) {
            trackAudioRemote.setTrackVolume(volume);
        }
    }

    @Override
    public int getAudioVolume() {
        Ln.d("MediaSessionImpl.getAudioVolume()");

        int volume = 0;
        if (trackAudioRemote != null) {
            volume = trackAudioRemote.getTrackVolume();
        }
        return volume;
    }


    @Override
    public void muteVideo(MediaRequestSource source) {
        Ln.d("MediaSessionImpl.muteVideo() request from source = %s", source.toString());
        if (trackVideoLocal != null) {
            trackVideoLocal.Mute();
        }
        videoMutedSource = source;
    }

    @Override
    public void unMuteVideo() {
        Ln.d("MediaSessionImpl.unMuteVideo()");
        if (trackVideoLocal != null) {
            trackVideoLocal.Unmute();
        }
        videoMutedSource = MediaRequestSource.NONE;
    }

    @Override
    public boolean isVideoMuted() {
        return videoMutedSource != MediaRequestSource.NONE;

    }

    @Override
    public MediaRequestSource getVideoMuteSource() {
        return videoMutedSource;
    }




    // ***************************************
    // Camera
    // ***************************************


    @Override
    public void switchCamera() {
        Ln.d("MediaSessionImpl.switchCamera");
        DeviceManager.MediaDevice dev;

        if (deviceManager != null) {
            if (selectedCamera.equals(MediaEngine.WME_FRONT_CAMERA)) {
                selectedCamera = MediaEngine.WME_BACK_CAMERA;
                dev = deviceManager.getCamera(DeviceManager.CameraType.Back);
            } else {
                selectedCamera = MediaEngine.WME_FRONT_CAMERA;
                dev = deviceManager.getCamera(DeviceManager.CameraType.Front);
            }
        } else {
            Ln.e("MediaSessionImpl.switchCamera, cannot retrieve camera device object--Device Manager is null.");
            return;
        }

        if (trackVideoLocal != null) {
            if (dev != null) {
                trackVideoLocal.SetCaptureDevice(dev);
            } else {
                Ln.e("MediaDevice is null");
            }
        } else {
            Ln.e("MediaSessionImpl.switchCamera, no local track!");
        }
    }

    @Override
    public boolean cameraFailed() {
        return cameraFailed;
    }


    @Override
    public void setDisplayRotation(int screenRotation) {
        WseEngine.setDisplayRotation(screenRotation);
    }

    // ***************************************
    // Audio Devices
    // ***************************************
    @Override
    public void headsetPluggedIn() {
        Ln.d("MediaSessionImpl.headsetPluggedIn");
        if (trackAudioLocal != null) {
            trackAudioLocal.AudioDeviceNotification(HEADSET_PLUGIN_NOTIFICATION, 0, 0);
        }
    }

    @Override
    public void headsetPluggedOut() {
        Ln.d("MediaSessionImpl.headsetPluggedOut");
        if (trackAudioLocal != null) {
            trackAudioLocal.AudioDeviceNotification(HEADSET_PLUGOUT_NOTIFICATION, 0, 0);
        }
    }

}
