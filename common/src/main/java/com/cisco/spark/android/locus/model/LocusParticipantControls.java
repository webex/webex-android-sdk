package com.cisco.spark.android.locus.model;

import android.net.Uri;

import java.util.List;

public class LocusParticipantControls {
    private Uri url;
    private LocusParticipantAudioControl audio;
    private LocusParticipantChallengeControl challenge;
    private List<LocusLink> links;


    public LocusParticipantControls(Uri url, List<LocusLink> links, LocusParticipantAudioControl audio, LocusParticipantChallengeControl challenge) {
        this.url = url;
        this.links = links;
        this.audio = audio;
        this.challenge = challenge;
    }

    public LocusParticipantAudioControl getAudio() {
        return audio;
    }

    public LocusParticipantChallengeControl getChallenge() {
        return challenge;
    }

    public List<LocusLink> getLinks() {
        return links;
    }

    public Uri getUrl() {
        return url;
    }
}
