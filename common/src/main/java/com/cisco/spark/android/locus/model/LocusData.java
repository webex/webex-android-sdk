package com.cisco.spark.android.locus.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.callcontrol.CallEndReason;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.NameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class LocusData {

    private Locus locus;
    private LocusKey locusKey;
    private long epochStartTime;

    private MediaSession mediaSession;

    private boolean isVideoDisabled;
    private boolean isCallConnected;
    private boolean isCallStarted;
    private boolean wasMediaFlowing;
    private CallContext callContext;
    private boolean isToasting;
    private CallInitiationOrigin callInitiationOrigin;
    private CallEndReason callEndReason;
    private UUID activeSpeakerId;
    private String observingResource;
    private boolean onHold;
    private boolean isAudioCall;

    // need to keep this state in client for now to know which media session to carry forward to new merged call
    // should no longer be needed once following has been added: https://sqbu-github.cisco.com/WebExSquared/cloud-apps/issues/2528
    private boolean initiatedMerge;

    private String remoteParticipantName;
    private String remoteParticipantEmail;


    public LocusData(Locus locus) {
        this.locus = locus;
        this.locusKey = locus.getKey();
        this.callInitiationOrigin = CallInitiationOrigin.CallOriginationUnknown;
        this.callEndReason = new CallEndReason();
    }

    public void reset() {
        locus = null;
    }

    /**
     * Fair warning this might be null
     */
    public @Nullable Locus getLocus() {
        return locus;
    }

    public void setLocus(Locus locus) {
        this.locus = locus;
    }

    public LocusKey getKey() {
        return locusKey;
    }

    public void setLocusKey(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public String getHostName() {
        return locus.getHost() != null ? NameUtils.getShortName(locus.getHost().getName()) : "";
    }

    public String getHostEmail() {
        return locus.getHost() != null ? locus.getHost().getEmail() : "";
    }

    public String getHostId() {

        if (locus.getHost() == null)
            return null;

        LocusParticipantInfo host = locus.getHost();
        return host.getId() != null ? host.getId() : "";
    }

    public String getRemoteParticipantName() {
        return remoteParticipantName;
    }

    public void setRemoteParticipantName(String remoteParticipantName) {
        this.remoteParticipantName = remoteParticipantName;
    }

    public Date getStartTime() {
        return locus.getFullState().getLastActive();
    }

    public long getEpochStartTime() {
        return epochStartTime;
    }

    public void setEpochStartTime(long timestamp) {
        this.epochStartTime = timestamp;
    }

    public void setRemoteParticipantEmail(String remoteParticipantEmail) {
        this.remoteParticipantEmail = remoteParticipantEmail;
    }

    public String getRemoteParticipantEmail() {
        return remoteParticipantEmail;
    }

    public CallContext getCallContext() {
        return callContext;
    }

    public void setCallContext(CallContext callContext) {
        this.callContext = callContext;
    }

    public void setVideoDisabled(boolean videoDisabled) {
        isVideoDisabled = videoDisabled;
    }

    public CallInitiationOrigin getCallInitiationOrigin() {
        return callInitiationOrigin;
    }

    public void setCallInitiationOrigin(CallInitiationOrigin callInitiationOrigin) {
        this.callInitiationOrigin = callInitiationOrigin;
    }

    public CallEndReason getCallEndReason() {
        return callEndReason;
    }

    public void setCallEndReason(CallEndReason reason) {
        callEndReason = reason;
    }

    public boolean isBridge() {

        if (locus == null || locus.getParticipants() == null) {
            return false;
        }

        // The ones that are always counted as a participant
        final List<LocusParticipant.Type> validTypes = Arrays.asList(LocusParticipant.Type.RESOURCE_ROOM,
                                                                     LocusParticipant.Type.ANONYMOUS,
                                                                     LocusParticipant.Type.MEETING_BRIDGE);

        int devices = 0;
        for (LocusParticipant ppt : locus.getParticipants()) {

            if (validTypes.contains(ppt.getType())) {
                if (ppt.getState() != LocusParticipant.State.JOINED) {
                    // Machines/bridges should never count if they aren't joined
                    continue;
                }

                devices++;

            } else if (ppt.getType() == LocusParticipant.Type.USER) {

                if (ppt.isGuest() && ppt.getState() == LocusParticipant.State.LEFT) {
                    // Guests who have left shouldn't be counted
                    continue;
                }

                // If there are no devices they can't be associated with a machine
                List<LocusParticipantDevice> deviceList = ppt.getDevices();
                if (deviceList == null || deviceList.isEmpty()) {
                    devices++;
                } else {
                    for (LocusParticipantDevice device : deviceList) {
                        if (!isPaired(device)) {
                            devices++;
                        }
                    }
                }
            }

            if (devices > 2) {
                return true;
            }
        }

        return false;
    }

    private boolean isPaired(LocusParticipantDevice device) {

        return device.getIntent() != null &&
               device.getIntent().getType() == LocusParticipant.IntentType.OBSERVE &&
               device.getIntent().getAssociatedWith() != null;
    }

    public boolean isEmptyBridge() {
        return isBridge() && (locus.getFullState().getCount() == 1);
    }

    public boolean isMeeting() {
        return locus.isMeeting();
    }

    public boolean isEmptyMeeting() {
        if (!isMeeting()) {
            return false;
        }

        int users = 0;
        for (LocusParticipant participant : locus.getParticipants()) {
            if (participant.getType() != null && participant.getType().equals(LocusParticipant.Type.USER) && participant.isJoined()) {
                // TODO: Should this account for single users joined on multiple devices?
                users++;
                if (users > 1)
                    return false;
            }
        }
        return users == 1;
    }

    public boolean containsNonUsers() {
        for (LocusParticipant participant : locus.getParticipants()) {
            if (participant.getType() == null || !participant.getType().equals(LocusParticipant.Type.USER)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public List<LocusParticipant> getJoinedParticipants() {
        List<LocusParticipant> joinedParticipants = new ArrayList<>();
        for (LocusParticipant participant : locus.getParticipants()) {
            if (participant.getState() == LocusParticipant.State.JOINED) {
                if (!participant.isObserving()) {
                    joinedParticipants.add(participant);
                }
            }
        }

        return joinedParticipants;
    }

    /**
     * Is the room associated to the UUID joined in this locus
     *
     * @param uuid
     * @return
     */
    public boolean isRoomJoinedLocusParticipant(@NonNull String uuid) {
        return getRoomJoinedLocusParticipant(uuid) != null ? true : false;
    }

    /**
     * Get the room associated to the UUID, as a LocusParticipant, if joined in this locus
     *
     * @param uuid
     * @return null if not joined; LocusParticipant if room is joined
     */
    public LocusParticipant getRoomJoinedLocusParticipant(@NonNull String uuid) {
        for (LocusParticipant participant : locus.getParticipants()) {
            if (isParticipantJoinedRoom(participant, uuid)) {
                return participant;
            }
        }
        return null;
    }

    public static boolean isParticipantJoinedRoom(LocusParticipant participant, String uuid) {
        if (participant.isRoom() && participant.isJoined()) {
            String roomIdentifier = participant.getRoomIdentifier();
            if (uuid != null && uuid.equals(roomIdentifier)) {
                return true;
            }
        }
        return false;
    }

    public LocusParticipant getActiveSpeaker() {
        if (activeSpeakerId != null) {
            for (LocusParticipant participant : locus.getParticipants()) {
                if (activeSpeakerId.equals(participant.getId())) {
                    return participant;
                }
            }
        }
        return null;
    }

    public void setActiveSpeakerId(UUID activeSpeakerId) {
        this.activeSpeakerId = activeSpeakerId;
    }


    public boolean isFloorGranted() {
        return locus.isFloorGranted();
    }

    public boolean isFloorMine() {
        if (isFloorGranted()) {
            LocusParticipant beneficiary = getLocus().getFloorBeneficiary();
            LocusParticipant self = getLocus().getSelf();
            if (self != null && beneficiary != null) {
                return self.getId().equals(beneficiary.getId());
            }
        }
        return false;
    }

    public LocusParticipant getParticipantSharing() {
        if (locus.isFloorGranted()) {
            for (MediaShare mediaShare : locus.getMediaShares()) {
                if (mediaShare.getFloor() != null && mediaShare.isValidType() && mediaShare.isMediaShareGranted()) {
                    UUID beneficiaryId = mediaShare.getFloor().getBeneficiary().getId();

                    for (LocusParticipant locusParticipant : locus.getParticipants()) {
                        if (locusParticipant.getId().equals(beneficiaryId)) {
                            return locusParticipant;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getFloorGrantedId() {

        String floorGrantedId = "";

        if (locus.getMediaShares() != null) {
            for (MediaShare mediaShare : locus.getMediaShares()) {
                if (mediaShare.isValidType()) {
                    if (mediaShare.getFloor() != null && Floor.GRANTED.equals(mediaShare.getFloor().getDisposition())) {
                        floorGrantedId = DateUtils.formatUTCDateString(mediaShare.getFloor().getGranted());
                    }
                }
            }
        }

        return floorGrantedId;
    }

    public LocusParticipant getParticipant(Uri participantUrl) {
        for (LocusParticipant locusParticipant : locus.getParticipants()) {
            if (locusParticipant.getUrl().equals(participantUrl)) {
                return locusParticipant;
            }
        }
        return null;
    }


    public void resetObservingResource() {
        observingResource = null;
    }

    public void setObservingResource() {
        if (locus.getSelf() != null) {
            for (LocusParticipantDevice device : locus.getSelf().getDevices()) {
                LocusParticipant.Intent intent = device.getIntent();
                if (intent != null && intent.getAssociatedWith() != null && intent.getType() != null && intent.getType().equals(LocusParticipant.IntentType.OBSERVE)) {
                    LocusParticipant associatedParticiant = getParticipant(Uri.parse(intent.getAssociatedWith()));

                    if (associatedParticiant != null) {
                        observingResource = associatedParticiant.getPerson().getId();
                    }
                }
            }
        }
    }

    public String getObservingResource() {
        return observingResource;
    }

    public boolean isObserving(Uri deviceUrl) {
        LocusParticipant.Intent intent = locus.getIntent(deviceUrl);
        return intent != null && intent.getType() != null &&
                (intent.getType().equals(LocusParticipant.IntentType.OBSERVE) || intent.getType().equals(LocusParticipant.IntentType.MOVE_MEDIA));
    }

    public boolean isObservingNotMoving(Uri deviceUrl) {
        LocusParticipant.Intent intent = locus.getIntent(deviceUrl);
        return intent != null && intent.getType() != null && (intent.getType().equals(LocusParticipant.IntentType.OBSERVE));
    }

    /**
     * A call is considered SIP call if one of the other participants reports getSipUrl or getTelUrl
     */
    public boolean isSipCall() {
        for (LocusParticipant locusParticipant : locus.getParticipants()) {
            LocusParticipantInfo locusUser = locusParticipant.getPerson();

            // Ignore self
            LocusSelfRepresentation self = locus.getSelf();
            if (self != null && self.getId().equals(locusParticipant.getId()))
                continue;

            if (locusUser != null && (locusUser.getSipUrl() != null || locusUser.getTelUrl() != null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Complicated by the fact that in the paired case the paired room is part of all participants
     * and there is no way to explicitly know we are paired to this room based on locus only.  In this
     * case we can simplify by just checking if all other participants are considered a room, which will
     * be true both in paired case and regular case
     *
     * Client-side room service knows this if we have not lost the state after making the paired call
     * but actually checking that requires a dependency on RoomService which we want to avoid
     *
     * There is ongoing work on the back-end to alleviate this work from the clients
     */
    public boolean isRemoteParticipantARoom() {

        // Simplified by checking that all other users a rooms
        for (LocusParticipant participant : locus.getParticipants()) {

            // ignore self
            LocusSelfRepresentation self = locus.getSelf();
            if (self != null && self.getId().equals(participant.getId()))
                continue;

            if (!participant.isRoom()) {
                return false;
            }
        }
        return true;

    }

    public boolean isGuest() {
        if (locus != null && locus.getSelf() != null) {
            return locus.getSelf().isGuest();
        }
        return false;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean onlyMeJoined() {

        List<LocusParticipant> joined = getJoinedParticipants(); // Doesn't include observers
        if (joined.size() == 0)
            return false;

        LocusSelfRepresentation self = locus.getSelf();
        if (self == null || self.getState() != LocusParticipant.State.JOINED)
            return false;

        if (self.isObserving()) {
            // Don't count us, only count the person we're observing
            return joined.size() == 1; // FIXME Should this be more explicit?
        }

        if (joined.size() > 1)
            return false;

        return joined.get(0).getPerson().getId().equals(self.getPerson().getId());
    }

    public boolean isOneOnOne() {
        return !isBridge();
    }

    @Nullable
    public LocusParticipant getInviter() {

        LocusSelfRepresentation self = locus.getSelf();
        if (self == null)
            return null;

        for (LocusParticipant locusParticipant : locus.getParticipants()) {
            if (locusParticipant.getUrl().equals(self.getInvitedBy())) {
                return locusParticipant;
            }
        }
        return null;
    }

    /**
     * Get the remote participant in a 1:1
     * this only produces a valid result if there are exactly two participants and self is one of
     * those.  Otherwise it will return the first other participant on a random basis
     */
    public LocusParticipant getOtherParticipant() {
        for (LocusParticipant locusParticipant : locus.getParticipants()) {
            if (!locus.getSelf().getId().equals(locusParticipant.getId())) {
                return locusParticipant;
            }
        }
        return null;
    }

    public LocusParticipant getParticipantByEmail(String email) {
        for (LocusParticipant participant : locus.getParticipants()) {
            if (participant.getPerson().getEmail().equals(email))
                return participant;
        }
        return null;
    }

    public boolean isCallConnected() {
        return isCallConnected;
    }

    public void setCallConnected(boolean isCallConnected) {
        this.isCallConnected = isCallConnected;
    }

    public boolean isCallStarted() {
        return isCallStarted;
    }

    public void setCallStarted(boolean isCallStarted) {
        this.isCallStarted = isCallStarted;
    }


    public boolean wasMediaFlowing() {
        return wasMediaFlowing;
    }

    public void setWasMediaFlowing(boolean wasMediaFlowing) {
        this.wasMediaFlowing = wasMediaFlowing;
    }

    public boolean isToasting() {
        return isToasting;
    }

    public void setIsToasting(boolean isToasting) {
        this.isToasting = isToasting;
    }

    // get remote participant's details (should be refactored for group calling)
    // This is undefined for a bridge call, and should only be used to get the remote participant
    // for a 1:1 call.
    // This looks for the first remote participant this is a User, or another type if there are no
    // remote participants of type User.  Ignores self and the observing resources if any.
    //
    public void setRemoteParticipantDetails() {
        LocusParticipant selfParticipant = locus.getSelf();
        String observingResource = getObservingResource();

        LocusParticipant remoteCandidate = null;

        for (LocusParticipant participant : locus.getParticipants()) {

            LocusParticipantInfo person = participant.getPerson();

            // The observing resource should not be considered the remote participant
            if (person.getId() != null && person.getId().equals(observingResource)) {
                continue;
            }

            if (person.getEmail() != null && selfParticipant != null && selfParticipant.getPerson() != null &&
                person.getEmail().equals(selfParticipant.getPerson().getEmail())) {
                continue;
            }

            if (participant.getType() == LocusParticipant.Type.USER) {
                remoteCandidate = participant;
                break;
            } else {
                // Record this participant as a candidate if we do not find a user.
                // we could be calling a room
                remoteCandidate = participant;
            }

        }

        if (remoteCandidate != null) {
            LocusParticipantInfo person = remoteCandidate.getPerson();
            setRemoteParticipantName(NameUtils.getShortName(person.getDisplayName()));
            setRemoteParticipantEmail(person.getEmail());
        }
    }

    public boolean isOnHold() {
        return onHold;
    }

    public void setOnHold(boolean onHold) {
        this.onHold = onHold;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
    }

    public void setMediaSession(MediaSession mediaSession) {
        this.mediaSession = mediaSession;
    }

    public boolean isInitiatedMerge() {
        return initiatedMerge;
    }

    public void setInitiatedMerge(boolean initiatedMerge) {
        this.initiatedMerge = initiatedMerge;
    }

    public boolean isAudioCall() {
        return isAudioCall;
    }

    public void setAudioCall(boolean audioCall) {
        isAudioCall = audioCall;
    }
}
