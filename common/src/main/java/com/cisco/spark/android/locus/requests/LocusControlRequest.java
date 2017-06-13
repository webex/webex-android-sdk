package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusLink;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.google.gson.JsonObject;

import javax.inject.Inject;

public class LocusControlRequest {

    private final String locusControlAudio = "audio";
    private final String locusControlRecord = "record";
    private final String locusControlLock = "lock";

    private final String locusControlStateMuted = "muted";
    private final String locusControlStateRecording = "recording";
    private final String locusControlStateLocked = "locked";

    private final String locusControlReason = "reason";
    private final String locusControlForced = "FORCED";

    @Inject
    public LocusControlRequest() {
    }

    private LocusLink createControlLink(String controlName, String controlStateName, boolean state) {

        JsonObject lockedStateJson = new JsonObject();
        JsonObject controlNameJson = new JsonObject();

        lockedStateJson.addProperty(controlStateName, state);
        controlNameJson.add(controlName, lockedStateJson);

        LocusLink locusLink = new LocusLink();
        locusLink.setBody(controlNameJson);

        return locusLink;
    }

    private Uri getControlUrl(Locus locus) {
        return locus.getKey().getUrl();
    }

    private Uri getParticipantControlUrl(LocusParticipant locusParticipant) {
        return locusParticipant.getUrl();
    }

    public LocusLink getLockLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlLock, locusControlStateLocked, true);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public LocusLink getUnlockLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlLock, locusControlStateLocked, false);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public LocusLink getStartRecordingLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlRecord, locusControlStateRecording, true);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public LocusLink getStopRecordingLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlRecord, locusControlStateRecording, false);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public LocusLink getMuteLocusLink(LocusParticipant locusParticipant) {
        LocusLink locusLink = createControlLink(locusControlAudio, locusControlStateMuted, true);
        Uri url = getParticipantControlUrl(locusParticipant);
        locusLink.setHref(url);
        return locusLink;
    }

    public LocusLink getUnmuteLocusLink(LocusParticipant locusParticipant) {
        LocusLink locusLink = createControlLink(locusControlAudio, locusControlStateMuted, false);
        Uri url = getParticipantControlUrl(locusParticipant);
        locusLink.setHref(url);
        return locusLink;
    }

    public LocusLink getExpelLocusLink(LocusParticipant locusParticipant) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(locusControlReason, locusControlForced);
        LocusLink locusLink = new LocusLink();
        locusLink.setBody(jsonObject);

        Uri uri = getParticipantControlUrl(locusParticipant);
        locusLink.setHref(uri);

        return locusLink;
    }

}
