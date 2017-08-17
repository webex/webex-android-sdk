package com.cisco.spark.android.media;

import android.graphics.Rect;
import android.os.Handler;
import android.view.View;

import com.cisco.spark.android.media.statistics.MediaStats;
import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.webex.wseclient.WseSurfaceView;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface MediaSession {


    void startSession(String deviceSettings, MediaEngine.MediaDirection mediaDirection, MediaSessionCallbacks mediaSessionCallbacks, SdpReadyCallback sdpReadyCallback);
    void updateSession(final MediaEngine.MediaDirection mediaDirection, SdpReadyCallback sdpReadyCallback);
    void endSession();
    boolean isMediaSessionStarted();
    boolean isMediaSessionEnding();
    String getLocalSdp();

    void startMedia();
    void stopMedia();
    void restartMedia();
    boolean isMediaStarted();
    boolean isCompositedVideoStream();

    void setMaxStreamCount(int newMaxStreamCount);
    int getMaxStreamCount();

    Long checkCSIs(List<Long> csiList);

    /**
     * Request to get audio data from the audio track to use for proximity
     * @param listener for callbacks
     */
    void setAudioDataListener(AudioDataListener listener);
    /**
     * Clear proximity audio data listener
     * @param listener for callbacks
     */
    void clearAudioDataListener(AudioDataListener listener);

    void setAudioSampling(int duration);

    MediaStats getStats();
    String getPacketStats();
    String getSessionStats();
    void setStatisticsUIHandler(Handler statisticsUIHandler);


    void setRemoteWindow(View view);
    void removeRemoteWindow(View view);
    void setRemoteWindow(Long csi, View view);
    void removeRemoteWindow(Long csi, View view);
    void removeRemoteWindow(Long csi);
    void removeRemoteVideoWindows();
    void setShareWindow(View view);
    void grabShareView(WseSurfaceView.FrameSaved frameSaved);
    File getLastShareFrame();
    void removeShareWindow();
    void setPreviewWindow(View surface);
    void startSelfView();
    void setActiveSpeakerWindow(View view);
    void removeActiveSpeakerWindow();

    Rect getVideoSize(int vid);
    Rect getFullsceenVideoSize();

    void muteAudio();
    void unMuteAudio();
    void muteRemoteAudio();
    void unMuteRemoteAudio();
    void setAudioVolume(int volume);
    int  getAudioVolume();
    void muteVideo(MediaRequestSource source);
    void unMuteVideo();
    boolean isAudioMuted();
    boolean isVideoMuted();
    boolean isRemoteAudioMuted();
    MediaRequestSource getVideoMuteSource();

    //sdk

    void muteRemoteVideo();
    void unmuteRemoteVideo();


    // Move these to Call and use the onFirstPacketRx/onFirstPacketTx in MediaSessionCallback?
    boolean isReceivedFirstAudioPacket();
    boolean isReceivedFirstVideoPacket();
    boolean isSentFirstAudioPacket();
    boolean isSentFirstVideoPacket();
    Date getFirstAudioPacketReceivedTime();
    Date getFirstVideoPacketReceivedTime();


    boolean usedTcpFallback();
    boolean iceFailed();
    boolean isServerRejected();
    boolean simulcastEnabled();
    boolean wasMediaFlowing();


    void switchCamera();
    void setDisplayRotation(int screenRotation);
    boolean cameraFailed();


    void answerReceived(String sdp, Map<String, String> featureToggles);
    void offerReceived(final String sdp);
    void updateSDP(final String sdp);

    void createAnswer(SdpReadyCallback sdpReadyCallback);


    void headsetPluggedIn();
    void headsetPluggedOut();
    void joinShare(String shareId);
    void leaveShare(String shareId);
    void startScreenShare(String startScreenShare);
    void stopScreenShare(String shareid);
    boolean isScreenSharing();
    void updateShareId(String shareId);


    void setAudioPlaybackFile(String audioPlaybackFile);
    void setVideoPlaybackFile(String videoPlaybackFile);

    class MediaDecodeSizeChangedEvent {
        public final Rect size;
        public final int vid;

        public MediaDecodeSizeChangedEvent(int vid, Rect size) {
            this.size = size;
            this.vid = vid;
        }
    }
}

