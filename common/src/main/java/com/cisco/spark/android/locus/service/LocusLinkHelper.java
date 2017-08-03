package com.cisco.spark.android.locus.service;

import android.net.Uri;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusLink;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.google.gson.JsonObject;


public class LocusLinkHelper {

    private static final String locusControlAudio = "audio";
    private static final String locusControlRecord = "record";
    private static final String locusControlLock = "lock";

    private static final String locusControlStateMuted = "muted";
    private static final String locusControlStateRecording = "recording";
    private static final String locusControlStatePauseRecording = "paused";
    private final static String locusControlStateLocked = "locked";

    private static final String locusControlReason = "reason";
    private static final String locusControlForced = "FORCED";


    private static LocusLink createControlLink(String controlName, String controlStateName, boolean state) {

        JsonObject lockedStateJson = new JsonObject();
        JsonObject controlNameJson = new JsonObject();

        lockedStateJson.addProperty(controlStateName, state);
        controlNameJson.add(controlName, lockedStateJson);

        LocusLink locusLink = new LocusLink();
        locusLink.setBody(controlNameJson);

        return locusLink;
    }

    private static Uri getControlUrl(Locus locus) {
        return locus.getKey().getUrl();
    }

    private static Uri getParticipantControlUrl(LocusParticipant locusParticipant) {
        return locusParticipant.getUrl();
    }

    public static LocusLink getLockLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlLock, locusControlStateLocked, true);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getUnlockLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlLock, locusControlStateLocked, false);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getStartRecordingLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlRecord, locusControlStateRecording, true);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getStopRecordingLocusLink(Locus locus) {
        LocusLink locusLink = createControlLink(locusControlRecord, locusControlStateRecording, false);
        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getResumeRecordingLocusLink(Locus locus) {
        JsonObject lockedStateJson = new JsonObject();
        JsonObject controlNameJson = new JsonObject();

        lockedStateJson.addProperty(locusControlStateRecording, true);
        lockedStateJson.addProperty(locusControlStatePauseRecording, false);
        controlNameJson.add(locusControlRecord, lockedStateJson);

        LocusLink locusLink = new LocusLink();
        locusLink.setBody(controlNameJson);

        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getPauseRecordingLocusLink(Locus locus) {
        JsonObject lockedStateJson = new JsonObject();
        JsonObject controlNameJson = new JsonObject();

        lockedStateJson.addProperty(locusControlStateRecording, true);
        lockedStateJson.addProperty(locusControlStatePauseRecording, true);
        controlNameJson.add(locusControlRecord, lockedStateJson);

        LocusLink locusLink = new LocusLink();
        locusLink.setBody(controlNameJson);

        Uri url = getControlUrl(locus);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getMuteLocusLink(LocusParticipant locusParticipant) {
        LocusLink locusLink = createControlLink(locusControlAudio, locusControlStateMuted, true);
        Uri url = getParticipantControlUrl(locusParticipant);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getUnmuteLocusLink(LocusParticipant locusParticipant) {
        LocusLink locusLink = createControlLink(locusControlAudio, locusControlStateMuted, false);
        Uri url = getParticipantControlUrl(locusParticipant);
        locusLink.setHref(url);
        return locusLink;
    }

    public static LocusLink getExpelLocusLink(LocusParticipant locusParticipant) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(locusControlReason, locusControlForced);
        LocusLink locusLink = new LocusLink();
        locusLink.setBody(jsonObject);

        Uri uri = getParticipantControlUrl(locusParticipant);
        locusLink.setHref(uri);

        return locusLink;
    }

}
