package com.cisco.spark.android.media;

import android.graphics.Rect;
import android.os.Handler;
import android.view.View;

import com.cisco.spark.android.media.statistics.MediaStats;
import com.cisco.spark.android.room.audiopairing.AudioDataListener;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MediaSessionAdapter implements MediaSession {

    @Override
    public void startSession(String deviceSettings, MediaEngine.MediaDirection mediaDirection, MediaCallbackObserver mediaCallbackObserver) {
    }

    @Override
    public void updateSession(MediaCallbackObserver mediaCallbackObserver, final MediaEngine.MediaDirection mediaDirection) {
    }


    @Override
    public void endSession() {
    }

    @Override
    public boolean isMediaSessionStarted() {
        return false;
    }

    @Override
    public boolean isMediaSessionEnding() {
        return false;
    }

    @Override
    public String getLocalSdp() {
        return null;
    }

    @Override
    public void startMedia() {

    }

    @Override
    public void stopMedia() {

    }

    @Override
    public void restartMedia() {

    }

    @Override
    public boolean isMediaStarted() {
        return false;
    }

    @Override
    public boolean isCompositedVideoStream() {
        return false;
    }

    @Override
    public void setMaxStreamCount(int newMaxStreamCount) {

    }

    @Override
    public int getMaxStreamCount() {
        return 0;
    }

    @Override
    public Long checkCSIs(List<Long> csiList) {
        return null;
    }

    @Override
    public void setAudioDataListener(AudioDataListener listener) {

    }

    @Override
    public void clearAudioDataListener(AudioDataListener listener) {

    }

    @Override
    public void setAudioSampling(int duration) {

    }

    @Override
    public MediaStats getStats() {
        return null;
    }

    @Override
    public String getPacketStats() {
        return null;
    }

    @Override
    public String getSessionStats() {
        return null;
    }

    @Override
    public void setStatisticsUIHandler(Handler statisticsUIHandler) {

    }

    @Override
    public void setRemoteWindow(View view) {

    }

    @Override
    public void removeRemoteWindow(View view) {

    }

    @Override
    public void setRemoteWindow(Long csi, View view) {

    }

    @Override
    public void removeRemoteWindow(Long csi, View view) {

    }

    @Override
    public void removeRemoteWindow(Long csi) {

    }

    @Override
    public void removeRemoteVideoWindows() {

    }

    @Override
    public void setShareWindow(View view) {

    }

    @Override
    public void removeShareWindow() {

    }

    @Override
    public void setPreviewWindow(View surface) {

    }

    @Override
    public void startSelfView() {

    }

    @Override
    public Rect getVideoSize(int vid) {
        return null;
    }

    @Override
    public Rect getFullsceenVideoSize() {
        return null;
    }

    @Override
    public void muteAudio() {

    }

    @Override
    public void unMuteAudio() {

    }

    @Override
    public void muteRemoteAudio() {

    }

    @Override
    public void unMuteRemoteAudio() {

    }

    @Override
    public void setAudioVolume(int volume) {

    }

    @Override
    public int getAudioVolume() {
        return 0;
    }

    @Override
    public void muteVideo(MediaRequestSource source) {

    }

    @Override
    public void unMuteVideo() {

    }

    @Override
    public boolean isAudioMuted() {
        return false;
    }

    @Override
    public boolean isVideoMuted() {
        return false;
    }

    @Override
    public boolean isRemoteAudioMuted() {
        return false;
    }

    @Override
    public MediaRequestSource getVideoMuteSource() {
        return null;
    }

    @Override
    public boolean isReceivedFirstAudioPacket() {
        return false;
    }

    @Override
    public boolean isReceivedFirstVideoPacket() {
        return false;
    }

    @Override
    public boolean isSentFirstAudioPacket() {
        return false;
    }

    @Override
    public boolean isSentFirstVideoPacket() {
        return false;
    }

    @Override
    public Date getFirstAudioPacketReceivedTime() {
        return null;
    }

    @Override
    public Date getFirstVideoPacketReceivedTime() {
        return null;
    }

    @Override
    public boolean usedTcpFallback() {
        return false;
    }

    @Override
    public boolean iceFailed() {
        return false;
    }

    @Override
    public boolean isServerRejected() {
        return false;
    }

    @Override
    public boolean simulcastEnabled() {
        return false;
    }

    @Override
    public boolean wasMediaFlowing() {
        return false;
    }

    @Override
    public void switchCamera() {

    }

    @Override
    public void setDisplayRotation(int screenRotation) {

    }

    @Override
    public boolean cameraFailed() {
        return false;
    }

    @Override
    public void answerReceived(String sdp, Map<String, String> featureToggles) {

    }

    @Override
    public void updateSDP(final String sdp) {

    }

    @Override
    public void headsetPluggedIn() {

    }

    @Override
    public void headsetPluggedOut() {

    }

    @Override
    public void joinShare(String shareId) {

    }

    @Override
    public void leaveShare(String shareId) {

    }

    @Override
    public void startScreenShare(ScreenShareCallback screenShareCallback, String startScreenShare) {

    }

    @Override
    public void stopScreenShare(String shareid) {

    }

    @Override
    public boolean isScreenSharing() {
        return false;
    }

    @Override
    public void updateShareId(String shareId) {

    }

    @Override
    public void setAudioPlaybackFile(String audioPlaybackFile) {

    }

    @Override
    public void setVideoPlaybackFile(String videoPlaybackFile) {

    }
}
