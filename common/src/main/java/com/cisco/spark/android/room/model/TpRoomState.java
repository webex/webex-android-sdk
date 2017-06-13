package com.cisco.spark.android.room.model;

import android.net.Uri;

import java.util.List;

public class TpRoomState {

    private Uri url;
    private String name;
    private Uri tokenBatchesUrl;
    private long maxResponseValidityInSeconds;
    private Uri callStatusUrl;
    private List<RoomUser> participants;
    private Uri audioUrl;
    private Uri bindingsUrl;

    public String getName() {
        return name;
    }

    public Uri getUrl() {
        return url;
    }

    public Uri getTokenBatchesUrl() {
        return tokenBatchesUrl;
    }

    public long getMaxResponseValidityInSeconds() {
        return maxResponseValidityInSeconds;
    }

    public Uri getCallStatusUrl() {
        return callStatusUrl;
    }

    public List<RoomUser> getParticipants() {
        return participants;
    }

    public Uri getAudioSpaceUrl() {
        return audioUrl;
    }

    public Uri getBindingsUrl() {
        return bindingsUrl;
    }

    public TpRoomState(Uri url, String name, Uri tokenBatchesUrl, long maxResponseValidityInSeconds,
                       Uri callStatusUrl, List<RoomUser> participants, Uri audioUrl, Uri bindingsUrl) {
        this.url = url;
        this.name = name;
        this.tokenBatchesUrl = tokenBatchesUrl;
        this.maxResponseValidityInSeconds = maxResponseValidityInSeconds;
        this.callStatusUrl = callStatusUrl;
        this.participants = participants;
        this.audioUrl = audioUrl;
        this.bindingsUrl = bindingsUrl;
    }
}
