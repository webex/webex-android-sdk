package com.cisco.spark.android.callcontrol.model;

import android.support.annotation.Nullable;

import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallEndReason;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusMeetingInfo;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.roap.model.RoapSession;

import java.util.UUID;

public class Call {
    private final String callId;
    private LocusKey locusKey;
    private String invitee;
    private String usingResource;
    private boolean active;
    private boolean isMoveMediaToResource;
    private CallInitiationOrigin callInitiationOrigin;
    private LocusData locusData;
    private MediaEngine.MediaDirection mediaDirection;
    private MediaSession mediaSession;
    private RoapSession roapSession;
    private LocusMeetingInfo locusMeetingInfo;

    private boolean isCallConnected;
    private boolean isCallStarted;
    private long epochStartTime;

    private boolean wasMediaFlowing;
    private boolean isVideoDisabled;

    private boolean isAudioCall;
    private boolean isAnsweringCall;
    private boolean onHold;
    private CallEndReason callEndReason;
    private String joinLocusTrackingID;
    private String hostPin;
    private Boolean moderator;


    private Call(final CallContext callContext) {
        this.callId = UUID.randomUUID().toString();
        this.locusKey = callContext.getLocusKey();
        this.invitee = callContext.getInvitee();
        this.isMoveMediaToResource = callContext.isMoveMediaToResource();
        this.callInitiationOrigin = callContext.getCallInitiationOrigin();
        this.isAnsweringCall = callContext.isAnsweringCall();
        this.hostPin = callContext.getPin();
        this.moderator = callContext.getModerator();
    }

    public static Call createCall(final CallContext callContext) {
        return new Call(callContext);
    }

    public String getCallId() {
        return callId;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public void setLocusKey(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusData getLocusData() {
        return locusData;
    }

    public void setLocusData(LocusData locusData) {
        this.locusData = locusData;
    }

    public String getInvitee() {
        return invitee;
    }

    public String getUsingResource() {
        return usingResource;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMoveMediaToResource() {
        return isMoveMediaToResource;
    }

    public MediaEngine.MediaDirection getMediaDirection() {
        return mediaDirection;
    }

    public void setMediaDirection(MediaEngine.MediaDirection mediaDirection) {
        this.mediaDirection = mediaDirection;
    }

    public boolean isCallConnected() {
        return isCallConnected;
    }

    public void setCallConnected(boolean callConnected) {
        isCallConnected = callConnected;
    }

    public boolean isCallStarted() {
        return isCallStarted;
    }

    public void setCallStarted(boolean callStarted) {
        isCallStarted = callStarted;
    }

    public boolean wasMediaFlowing() {
        return wasMediaFlowing;
    }

    public void setWasMediaFlowing(boolean wasMediaFlowing) {
        this.wasMediaFlowing = wasMediaFlowing;
    }

    public CallEndReason getCallEndReason() {
        return callEndReason;
    }

    public void setCallEndReason(CallEndReason callEndReason) {
        this.callEndReason = callEndReason;
    }

    public long getEpochStartTime() {
        return epochStartTime;
    }

    public void setEpochStartTime(long epochStartTime) {
        this.epochStartTime = epochStartTime;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
    }

    public void setMediaSession(MediaSession mediaSession) {
        this.mediaSession = mediaSession;
    }

    public CallInitiationOrigin getCallInitiationOrigin() {
        return callInitiationOrigin;
    }

    public boolean isAudioCall() {
        return isAudioCall;
    }

    public void setAudioCall(boolean audioCall) {
        isAudioCall = audioCall;
    }

    public boolean isAnsweringCall() {
        return isAnsweringCall;
    }

    public void setAnsweringCall(boolean answeringCall) {
        isAnsweringCall = answeringCall;
    }

    public boolean isOnHold() {
        return onHold;
    }

    public void setOnHold(boolean onHold) {
        this.onHold = onHold;
    }

    public String getJoinLocusTrackingID() {
        return joinLocusTrackingID;
    }

    public void setJoinLocusTrackingID(String joinLocusTrackingID) {
        this.joinLocusTrackingID = joinLocusTrackingID;
    }

    public boolean isVideoDisabled() {
        return isVideoDisabled;
    }

    public void setVideoDisabled(boolean videoDisabled) {
        isVideoDisabled = videoDisabled;
    }

    public void setUsingResource(String usingResource) {
        this.usingResource = usingResource;
    }

    public RoapSession getRoapSession() {
        return roapSession;
    }

    public void setRoapSession(RoapSession roapSession) {
        this.roapSession = roapSession;
    }

    public Boolean getModerator() {
        return moderator;
    }

    public void setModerator(Boolean moderator) {
        this.moderator = moderator;
    }

    public String getHostPin() {
        return hostPin;
    }

    public void setHostPin(String hostPin) {
        this.hostPin = hostPin;
    }

    public @Nullable LocusMeetingInfo getLocusMeetingInfo() {
        return locusMeetingInfo;
    }

    public LocusMeetingInfo.MeetingType getLocusMeetingType() {
        return getLocusMeetingType(locusData);
    }

    public LocusMeetingInfo.MeetingType getLocusMeetingType(LocusData locusData) {
        return locusMeetingInfo == null ? LocusMeetingInfo.MeetingType.UNKNOWN : locusMeetingInfo.determineMeetingType(locusData);
    }

    public void setLocusMeetingInfo(LocusMeetingInfo locusMeetingInfo) {
        this.locusMeetingInfo = locusMeetingInfo;
    }
}
