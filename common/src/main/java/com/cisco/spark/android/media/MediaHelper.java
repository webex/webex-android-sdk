package com.cisco.spark.android.media;

import com.webex.wme.MediaConnection;
import com.webex.wme.MediaTrack;

import java.util.Arrays;

public class MediaHelper {

    private MediaHelper() {
    }

    public static String getMediaTypeString(int mid) {
        if (mid == MediaSessionEngine.AUDIO_MID) {
            return "Audio";
        } else if (mid == MediaSessionEngine.SHARE_MID) {
            return "Share";
        } else if (mid == MediaSessionEngine.VIDEO_MID) {
            return "Video";
        } else {
            return "Unknown";
        }
    }

    public static String getMediaTypeString(MediaConnection.MediaType mediaType) {
        if (mediaType == MediaConnection.MediaType.Audio) {
            return getMediaTypeString(MediaSessionEngine.AUDIO_MID);
        } else if (mediaType == MediaConnection.MediaType.Video) {
            return getMediaTypeString(MediaSessionEngine.VIDEO_MID);
        } else if (mediaType == MediaConnection.MediaType.Sharing) {
            return getMediaTypeString(MediaSessionEngine.SHARE_MID);
        } else {
            return getMediaTypeString(-1);
        }
    }

    public static String getVideoStreamString(int vid) {
        return isActiveSpeaker(vid) ? "ActiveSpeaker" : "Other";
    }

    public static boolean isActiveSpeaker(int videoId) {
        return videoId == 0;
    }

    public static String formatMediaTrack(MediaTrack track) {
        return String.format("[MediaTrack vid=%s csis=%s mediaStatus=%s]", track.getVID(), Arrays.toString(track.getCSI()), track.getMediaStatus());
    }

    public static boolean isVideo(long mid) {
        return mid == MediaSessionEngine.VIDEO_MID;
    }

    public static boolean isShare(long mid) {
        return mid == MediaSessionEngine.SHARE_MID;
    }

    public static void requestRemoteVideo(MediaTrack remoteTrack, MediaEngine.VideoSCRParams p) {
        remoteTrack.RequestVideo(p.maxFs, p.maxFps, p.maxBr, p.maxDpb, p.maxMbps, p.priority, p.grouping, p.duplicate);
    }

}
