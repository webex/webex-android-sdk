package com.cisco.spark.android.locus.model;

import java.util.List;

public class LocusParticipantStatus {

    public final MediaDirection audioStatus;
    public final MediaDirection videoStatus;
    public final List<Long> csis;

    public LocusParticipantStatus(MediaDirection audioStatus, MediaDirection videoStatus, List<Long> csis) {
        this.audioStatus = audioStatus;
        this.videoStatus = videoStatus;
        this.csis = csis;
    }

    public MediaDirection getAudioStatus() {
        return audioStatus;
    }

    public MediaDirection getVideoStatus() {
        return videoStatus;
    }

    public List<Long> getCsis() {
        return csis;
    }
}
