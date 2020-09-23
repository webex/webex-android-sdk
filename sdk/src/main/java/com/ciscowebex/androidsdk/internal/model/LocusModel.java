/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.ciscowebex.androidsdk.internal.Device;
import com.ciscowebex.androidsdk.phone.Call;
import com.ciscowebex.androidsdk.utils.DateUtils;
import com.ciscowebex.androidsdk.utils.Json;
import com.google.gson.annotations.SerializedName;

import me.helloworld.utils.Checker;
import me.helloworld.utils.Objects;

import java.text.DateFormat;
import java.util.*;

public class LocusModel {

    public enum LocusTag {
        ONE_ON_ONE,
        ONE_ON_ONE_MEETING,
        WEBEX,
        EXTERNAL_URL
    }

    @SerializedName("url")
    private LocusKeyModel key;
    private Date created;
    private LocusParticipantInfoModel host;
    private LocusStateModel fullState = new LocusStateModel(false, 0, LocusStateModel.State.INACTIVE);
    private LocusControlModel controls;
    private List<LocusParticipantModel> participants = new ArrayList<>();
    private LocusSelfModel self;
    private LocusSequenceModel sequence;
    private LocusSequenceModel baseSequence;
    private String syncUrl;
    private List<MediaShareModel> mediaShares = new ArrayList<>();
    private List<LocusReplacesModel> replaces = new ArrayList<>();
    private String conversationUrl;
    private LocusDescriptionModel info;
    private LocusScheduledMeetingModel meeting;
    private List<LocusScheduledMeetingModel> meetings;
    private Uri aclUrl;
    private List<MediaConnectionModel> mediaConnections;

    public LocusKeyModel getKey() {
        return key;
    }

    public Date getCreated() {
        return created;
    }

    public LocusParticipantInfoModel getHost() {
        return host;
    }

    public LocusStateModel getFullState() {
        return fullState;
    }

    public LocusControlModel getControls() {
        return controls;
    }

    public LocusLockControlModel getLockControl() {
        return controls == null ? null : controls.getLock();
    }

    public LocusRecordControlModel getRecordControl() {
        return controls == null ? null : controls.getRecord();
    }

    public List<LocusParticipantModel> getRawParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public List<LocusParticipantModel> getParticipants() {
        ArrayList<LocusParticipantModel> filtered = new ArrayList<>(participants.size());
        for (LocusParticipantModel part : participants) {
            if (!part.isRemoved())
                filtered.add(part);
        }
        return filtered;
    }

    @Nullable
    public LocusSelfModel getSelf() {
        return self;
    }

    public LocusSequenceModel getSequence() {
        return sequence;
    }

    public List<MediaShareModel> getMediaShares() {
        return mediaShares;
    }

    public LocusSequenceModel getBaseSequence() {
        return baseSequence;
    }

    public String getSyncUrl() {
        return syncUrl;
    }

    public List<LocusReplacesModel> getReplaces() {
        return replaces;
    }

    public List<MediaConnectionModel> getMediaConnections() {
        return mediaConnections;
    }

    public void setMediaConnections(List<MediaConnectionModel> mediaConnections) {
        this.mediaConnections = mediaConnections;
    }

    public String getConversationUrl() {
        return (conversationUrl == null && info != null) ? info.getConversationUrl() : conversationUrl;
    }

    public LocusDescriptionModel getInfo() {
        return info;
    }

    public LocusScheduledMeetingModel getMeeting() {
        return meeting;
    }

    public List<LocusScheduledMeetingModel> getMeetings() {
        return meetings;
    }

    /**
     * NOTE TO USERS: When this method returns false it has two separate, distinct meanings.
     * <p>
     * 1. The meeting object on the DTO is null
     * 2. The meeting object is non-null, and is inactive.
     * <p>
     * If you do not know if (1) from above is not the case based on prior processing then this method
     * should be called in conjunction with {@link #getMeeting()} to see if the object is null or not.
     * Do not assume that because this reports false that there is an inactive meeting object
     *
     * @return true if the meeting is active, false if either the meeting doesn't exist or it does exist and is inactive
     */
    public boolean isMeetingActive() {
        return meeting != null && !meeting.isRemoved();
    }

    public Uri getAclUrl() {
        return aclUrl;
    }

    public void setAclUrl(Uri aclUrl) {
        this.aclUrl = aclUrl;
    }

    public String getUniqueCallID() {
        String locusId = key.getLocusId();
        DateFormat dateFormat = DateUtils.buildIso8601Format();
        String lastActive = dateFormat.format(getFullState().getLastActive());
        if (locusId.length() > 0 && lastActive.length() > 0) {
            return String.format("%s,%s", locusId, lastActive);
        }
        return null;
    }

    public boolean isJoinedFromThisDevice(String deviceUrl) {
        if (getSelf() != null) {
            if (getFullState().isActive() && getSelf().getState().equals(LocusParticipantModel.State.JOINED)) {
                List<LocusParticipantDeviceModel> deviceList = getSelf().getDevices();
                if (deviceList != null) {
                    for (LocusParticipantDeviceModel device : deviceList) {
                        if (device.getUrl().equals(deviceUrl) && LocusParticipantModel.State.JOINED == device.getState()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    public LocusParticipantDeviceModel getMyDevice() {
        if (getSelf() != null) {
            return getMyDevice(getSelf().getDeviceUrl());
        }
        return null;
    }

    @Nullable
    public LocusParticipantDeviceModel getMyDevice(String deviceUrl) {
        if (deviceUrl != null && getSelf() != null) {
            List<LocusParticipantDeviceModel> deviceList = getSelf().getDevices();
            if (deviceList != null) {
                for (LocusParticipantDeviceModel device : deviceList) {
                    if (device.getUrl().equals(deviceUrl)) {
                        return device;
                    }
                }
            }
        }
        return null;
    }

    public boolean isJoinedFromOtherDevice(Uri deviceUrl) {
        if (getSelf() != null) {
            if (getFullState().isActive() && getSelf().getState().equals(LocusParticipantModel.State.JOINED)) {
                List<LocusParticipantDeviceModel> deviceList = getSelf().getDevices();
                if (deviceList != null) {
                    for (LocusParticipantDeviceModel device : deviceList) {
                        if (!device.getUrl().equals(deviceUrl) && LocusParticipantModel.State.JOINED == device.getState()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This is the  participant who called a Cisco Spark user or the participant a user on Cisco Spark called
     * For instance, User A calling B with telephone number +1-408-474-3XXX using our app
     * Note that this participant is not to be confused with a call-in participant in the PSTN feature
     * The call must be initiated between Cisco Spark app user and a cellular/landline network or vice versa
     * This call must not be a PMR call, one of the participant the cellular/landline network must be external, and
     * the participant's primary display name must be a telephone number
     *
     * @return
     */
    @Nullable
    public LocusParticipantModel getFirstExternalParticipantOnTelephone() {
        List<LocusParticipantModel> participants = getParticipants();
        if (!getInfo().isPmr() && participants != null && !participants.isEmpty()) {
            for (LocusParticipantModel participant : participants) {
                if (participant.getPerson().isExternal()) {
                    String displayName = participant.getPerson().getDisplayName();
                    if (PhoneNumberUtils.isGlobalPhoneNumber(displayName) && displayName.equals(participant.getPerson().getPrimaryDisplayString())) {
                        return participant;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Counts the number of participants with type=USER or type=RESOURCE_ROOM who are joined or in lobby
     * <p>
     * If a user is observing a resource we remove that room from the count.
     * <p>
     * We only count a room if it's not being observed, and we count everyone observing the room as a participant
     * <p>
     * Bob and Alice is observing Batcave
     * Charlie is joined from android
     * Mayflower is a room joined without any observers
     * <p>
     * Count Bob, Alice, Charlie and Mayflower (Ignore Batcave)
     * <p>
     * Tests in common/LocusTest
     *
     * @return joinedAndInLobbyParticipants
     */
    public int getJoinedAndInLobbyParticipantCount() {
        int joinedAndInLobbyParticipantCount = 0;
        Set<String> observedResources = new HashSet<>();
        List<LocusParticipantModel> participants = getParticipants();
        for (LocusParticipantModel locusParticipant : participants) {
            List<LocusParticipantDeviceModel> devices = locusParticipant.getDevices();
            if (devices != null) {
                for (LocusParticipantDeviceModel device : devices) {
                    IntentModel intent = device.getIntent();
                    if (intent == null) {
                        continue;
                    }
                    if (intent.getType() == LocusParticipantModel.IntentType.OBSERVE) {
                        observedResources.add(intent.getAssociatedWith());
                    }
                }
            }
            if ((locusParticipant.isJoined() || locusParticipant.isInLobby()) && locusParticipant.isUserOrResourceRoom()) {
                joinedAndInLobbyParticipantCount += 1;
            }
        }
        joinedAndInLobbyParticipantCount -= observedResources.size();
        return joinedAndInLobbyParticipantCount;
    }

    public boolean isObtp() {
        return getMeeting() != null;
    }

    public boolean isActiveObtp() {
        return isMeetingActive() &&
                (getFullState().getState() == LocusStateModel.State.INITIALIZING || getFullState().getState() == LocusStateModel.State.ACTIVE);
    }

    public boolean isAnyoneJoined() {
        List<LocusParticipantModel> locusParticipants = getParticipants();
        if (locusParticipants != null) {
            for (LocusParticipantModel participant : locusParticipants) {
                if (participant.isUserOrResourceRoom() &&
                        (participant.isJoined() || participant.isInLobby()))
                    return true;
            }
        }
        return false;
    }

    public boolean isAnyNonHostJoined() {
        LocusParticipantInfoModel host = getHost();
        List<LocusParticipantModel> locusParticipants = getParticipants();
        if (host != null && locusParticipants != null) {
            String hostId = host.getId();
            for (LocusParticipantModel participant : locusParticipants) {
                LocusParticipantInfoModel person = participant.getPerson();
                if (person != null) {
                    String participantId = person.getId();
                    if (hostId != null &&
                            participant.isUserOrResourceRoom() &&
                            !hostId.equals(participantId) &&
                            (participant.isJoined() || participant.isInLobby())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isSelfHost() {
        LocusParticipantInfoModel host = getHost();
        LocusSelfModel self = getSelf();

        if (host != null && self != null) {
            LocusParticipantInfoModel selfParticipant = self.getPerson();
            if (selfParticipant != null) {
                String hostId = host.getId();
                String selfId = self.getId().toString();
                String hostEmail = host.getEmail();
                String selfEmail = selfParticipant.getEmail();
                return (hostId != null && hostId.equals(selfId)) ||
                        (hostEmail != null && hostEmail.equals(selfEmail));
            }
        }
        return false;
    }

    public boolean isSelfGuest() {
        LocusSelfModel selfRepresentation = getSelf();
        return selfRepresentation != null && selfRepresentation.isGuest();
    }

    public boolean isHostJoined() {
        LocusParticipantInfoModel host = getHost();
        List<LocusParticipantModel> locusParticipants = getParticipants();
        if (host != null && locusParticipants != null) {
            String hostId = host.getId();
            for (LocusParticipantModel participant : locusParticipants) {
                if (participant.getPerson() != null &&
                        participant.getPerson().getId() != null &&
                        participant.getPerson().getId().equals(hostId) &&
                        participant.isJoined()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInLobbyFromThisDevice(String deviceUrl) {
        boolean isInLobby = false;
        if (self != null && self.isIdle()) {
            if (self.isWaitingOnDevice(deviceUrl)) {
                isInLobby = true;
            } else if (self.isObservingOnDevice(deviceUrl)) {
                // Determine if paired device is a Webex Share and if it is in the lobby.
                LocusParticipantDeviceModel selfDevice = getMyDevice(deviceUrl);
                if (selfDevice != null && selfDevice.getIntent() != null && selfDevice.getIntent().getAssociatedWith() != null) {
                    LocusParticipantModel associatedParticipant = getParticipant(Uri.parse(selfDevice.getIntent().getAssociatedWith()));
                    if (associatedParticipant != null && associatedParticipant.isDeviceType(Device.Type.WEBEX_SHARE)) {
                        if (associatedParticipant.isInLobby()) {
                            isInLobby = true;
                        }
                    }
                }
            }
        }
        return isInLobby;
    }

    public boolean isParticipantJoinedWithWebShare(LocusParticipantModel participant) {
        if (participant == null) {
            return false;
        }
        for (LocusParticipantModel locusParticipant : getParticipants()) {
            if (participant.getId().equals(locusParticipant.getId())) {
                String deviceUrl = locusParticipant.getDeviceUrl();
                if (deviceUrl == null) {
                    continue;
                }
                for (LocusParticipantDeviceModel device : locusParticipant.getDevices()) {
                    if (device != null && device.getIntent() != null && device.getIntent().getAssociatedWith() != null) {
                        LocusParticipantModel associatedParticipant = getParticipant(Uri.parse(device.getIntent().getAssociatedWith()));
                        if (associatedParticipant != null && associatedParticipant.isDeviceType(Device.Type.WEBEX_SHARE) &&
                                associatedParticipant.getState() == LocusParticipantModel.State.JOINED) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public IntentModel getIntent(Uri deviceUrl) {
        if (getSelf() != null) {
            List<LocusParticipantDeviceModel> deviceList = getSelf().getDevices();
            if (deviceList != null) {
                for (LocusParticipantDeviceModel device : deviceList) {
                    if (device.getUrl().equals(deviceUrl)) {
                        return device.getIntent();
                    }
                }
            }
        }
        return null;
    }

    public LocusParticipantModel getParticipant(Uri participantUrl) {
        for (LocusParticipantModel locusParticipant : getParticipants()) {
            if (locusParticipant.getUrl().equals(participantUrl)) {
                return locusParticipant;
            }
        }
        return null;
    }


    public boolean isObserving(Uri deviceUrl) {
        IntentModel intent = getIntent(deviceUrl);
        return intent != null && intent.getType() != null &&
                (intent.getType().equals(LocusParticipantModel.IntentType.OBSERVE) || intent.getType().equals(LocusParticipantModel.IntentType.MOVE_MEDIA));
    }


    public boolean isSelfJoinedFromDeviceType(String deviceType) {
        LocusParticipantModel participant = getSelf();
        if (participant == null) {
            return false;
        }
        return isParticipantJoinedFromDeviceType(participant, deviceType);
    }

    @Nullable
    public String getDisplayNameOrEmailForCaller() {
        List<LocusParticipantModel> locusParticipants = getParticipants();
        if (locusParticipants != null && !locusParticipants.isEmpty()) {
            for (LocusParticipantModel locusParticipant : locusParticipants) {
                LocusSelfModel self = getSelf();
                if (self != null && !locusParticipant.getId().equals(self.getId()) && locusParticipant.isCreator) {
                    String displayName = locusParticipant.getPerson().getDisplayName();
                    return TextUtils.isEmpty(displayName) ? locusParticipant.getPerson().getEmail() : displayName;
                }
            }
        }
        return null;
    }

    public boolean isRemoteParticipantJoinedFromDeviceType(String deviceType) {
        for (LocusParticipantModel participant : getParticipants()) {
            // Ignore self
            if (getSelf().getId().equals(participant.getId())) {
                continue;
            }
            return isParticipantJoinedFromDeviceType(participant, deviceType);
        }
        return false;
    }

    public boolean isParticipantJoinedFromDeviceType(LocusParticipantModel participant, String deviceType) {
        LocusParticipantModel.State state = participant.getState();
        // self.state is leaving a brief while when the incoming share is ended.
        if (getFullState().isActive() && (state.equals(LocusParticipantModel.State.JOINED) || state.equals(LocusParticipantModel.State.LEAVING))) {
            List<LocusParticipantDeviceModel> deviceList = participant.getDevices();
            if (deviceList != null) {
                for (LocusParticipantDeviceModel device : deviceList) {
                    if (device.getDeviceType() != null && device.getDeviceType().equalsIgnoreCase(deviceType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public MediaShareModel getGrantedMediaShare() {
        if (getMediaShares() != null) {
            for (MediaShareModel mediaShare : getMediaShares()) {
                if (mediaShare.isValidType() && mediaShare.getFloor() != null && mediaShare.getFloor().getDisposition() == FloorModel.Disposition.GRANTED) {
                    return mediaShare;
                }
            }
        }
        return null;
    }

    public FloorModel getGrantedFloor() {
        MediaShareModel model = getGrantedMediaShare();
        return model == null ? null : model.getFloor();
    }

    public boolean isFloorGranted() {
        return getGrantedFloor() != null;
    }

    public MediaShareModel getWhiteboardMedia() {
        if (getMediaShares() != null) {
            for (MediaShareModel mediaShare : getMediaShares()) {
                if (mediaShare.isWhiteboard()) {
                    return mediaShare;
                }
            }
        }
        return null;
    }

    public boolean isHeld() {
        if (getSelf() != null && getSelf().getIntents() != null) {
            for (IntentModel intent : getSelf().getIntents()) {
                if (intent.getType().equals(LocusParticipantModel.IntentType.HELD)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isMeeting() {
        return fullState.getType() == LocusStateModel.Type.MEETING;
    }

    public boolean isSparkSpaceMeeting() {
        return isMeeting() && hasConversationUrl();
    }

    /**
     * Is this OBTP for one-on-one space?
     * One-on-one spaces have one fixed one-on-one locus for calls associated with the space.
     * And another separate locus for one-on-one scheduled space meetings.
     * Having two separate loci allows us to have one-on-one scheduled meetings with sharable meeting link while maintaining the current one-on-one private call feature.
     * One-on-one scheduled space meetings (OBTPs) will have ONE_ON_ONE_MEETING locus tag to identify them
     *
     * @return
     */
    public boolean isOneOnOneSpaceObtp() {
        return getInfo().getLocusTags().contains(LocusTag.ONE_ON_ONE_MEETING);
    }

    public boolean hasConversationUrl() {
        return conversationUrl != null;
    }

    public boolean isProvisionalDeviceConnected(String provisionalDeviceUrl) {
        boolean isConnected = false;
        LocusParticipantDeviceModel provisionalDevice = getMyDevice(provisionalDeviceUrl);
        if (provisionalDevice == null || provisionalDevice.getFinalUrl() == null) {
            return isConnected;
        }

        if (isJoinedFromThisDevice(provisionalDeviceUrl)) {
            List<LocusParticipantDeviceModel> devices = getSelf().getDevices();
            for (LocusParticipantDeviceModel device : devices) {
                // Find a SIP device and points to the provisional device and vice versa and is also either
                // joined or in the lobby state.
                if (Device.Type.SIP.getTypeName().equalsIgnoreCase(device.getDeviceType()) &&
                        provisionalDevice.getFinalUrl().equals(device.getUrl()) &&
                        provisionalDevice.getUrl().equals(device.getProvisionalUrl())) {
                    isConnected = isJoinedFromThisDevice(device.getUrl());
                    break;
                }
            }
        }
        return isConnected;
    }

    public boolean isProvisionalDevicePresent(String provisionalDeviceUrl) {
        return (getMyDevice(provisionalDeviceUrl) != null);
    }

    // MARK Shortcut
    public boolean isValid() {
        String url = getCallUrl();
        LocusSelfModel self = getSelf();
        LocusParticipantInfoModel host = getHost();
        if (url != null && self != null && host != null) {
            return true;
        }
        return url != null && getBaseSequence() != null;
    }

    public boolean isOneOnOne() {
        LocusStateModel model = getFullState();
        return (model != null && model.getType() != LocusStateModel.Type.MEETING) || isOneOnOneMeeting();
    }

    public boolean isOneOnOneMeeting() {
        LocusStateModel model = getFullState();
        if (model != null && model.getType() == LocusStateModel.Type.MEETING) {
            LocusDescriptionModel info = getInfo();
            return info != null && info.getLocusTags().contains(LocusTag.ONE_ON_ONE_MEETING);
        }
        return false;
    }

    public boolean isIncomingCall() {
        LocusStateModel full = getFullState();
        if (full == null) {
            return false;
        }
        AlertTypeModel alert = (getSelf() == null) ? null : getSelf().getAlertType();
        return (full.getState() == LocusStateModel.State.ACTIVE && alert != null && AlertTypeModel.ALERT_FULL.equalsIgnoreCase(alert.getAction()))
                || ((full.getState() == LocusStateModel.State.ACTIVE || full.getState() == LocusStateModel.State.INITIALIZING) && getMeeting() != null);
    }

    public boolean isInactive() {
        LocusStateModel model = getFullState();
        return  model != null && model.getState() == LocusStateModel.State.INACTIVE;
    }

    public boolean isSelfInLobby() {
        LocusSelfModel self = getSelf();
        if (self == null) {
            return false;
        }
        if (!self.isIdle()) {
            return false;
        }
        List<LocusParticipantDeviceModel> devices = self.getDevices();
        if (Checker.isEmpty(devices)) {
            return false;
        }
        for (LocusParticipantDeviceModel device : devices) {
            IntentModel intent = device.getIntent();
            if (intent != null && intent.getType() == LocusParticipantModel.IntentType.WAIT) {
                return true;
            }
        }
        return false;
    }

    public boolean isLocalSupportDTMF() {
        return getSelf() != null && getSelf().isEnableDTMF();
    }

    public boolean isRemoteAudioMuted() {
        List<LocusParticipantModel> participants = getParticipants();
        LocusSelfModel self = getSelf();
        if (participants == null || self == null) {
            return true;
        }
        for (LocusParticipantModel participant : participants) {
            if (!Checker.isEqual(participant.getId(), self.getId()) && participant.isJoined() && participant.isValidCiUser()) {
                LocusParticipantStatusModel status = participant.getStatus();
                if (status != null && !LocusMediaDirection.RECVONLY.equals(status.getAudioStatus()) && !LocusMediaDirection.INACTIVE.equals(status.getAudioStatus())) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getCallUrl() {
        List<LocusReplacesModel> replaces = getReplaces();
        if (!Checker.isEmpty(replaces)) {
            for (LocusReplacesModel replace : replaces) {
                return replace.getLocusKey().getUrl();
            }
        }
        return key.getUrl();
    }

    public String getSelfId() {
        LocusSelfModel self = getSelf();
        return self == null ? null : self.getId();
    }

    public String getLocalSdp() {
        MediaConnectionModel mc = Checker.isEmpty(mediaConnections) ? null : mediaConnections.get(0);
        if (mc == null) {
            return null;
        }
        MediaInfoModel m = Json.fromJson(mc.getLocalSdp(), MediaInfoModel.class);
        if (m == null) {
            return null;
        }
        return m.getSdp();
    }

    public String getRemoteSdp(String deviceUrl) {
        MediaConnectionModel mc = getMediaConnection(deviceUrl);
        if (mc == null || mc.getRemoteSdp() == null) {
            mc = getMediaConnection(null);
        }
        if (mc == null || mc.getRemoteSdp() == null) {
            return null;
        }
        MediaInfoModel m = Json.fromJson(mc.getRemoteSdp(), MediaInfoModel.class);
        if (m == null) {
            return null;
        }
        return m.getSdp();
    }

    public @Nullable String getMediaShareUrl() {
        if (getMediaShares() != null) {
            for (MediaShareModel mediaShare : getMediaShares()) {
                if (MediaShareModel.SHARE_CONTENT_TYPE.equals(mediaShare.getName())) {
                    return mediaShare.getUrl();
                }
            }
        }
        return null;
    }

    public String getMediaId(String deviceUrl) {
        MediaConnectionModel mc = getMediaConnection(deviceUrl);
        return mc == null ? null : mc.getMediaId();
    }

    public String getKeepAliveUrl(String deviceUrl) {
        MediaConnectionModel mc = getMediaConnection(deviceUrl);
        return mc == null ? null : mc.getKeepAliveUrl();
    }

    public int getKeepAliceSecs(String deviceUrl) {
        MediaConnectionModel mc = getMediaConnection(deviceUrl);
        return mc == null ? -1 : mc.getKeepAliveSecs();
    }

    private MediaConnectionModel getMediaConnection(String deviceUrl) {
        MediaConnectionModel ret = null;
        if (deviceUrl != null) {
            LocusSelfModel self = getSelf();
            if (self != null) {
                List<LocusParticipantDeviceModel> devices = self.getDevices();
                if (!Checker.isEmpty(devices)) {
                    for (LocusParticipantDeviceModel device : devices) {
                        if (Checker.isEqual(deviceUrl, device.getUrl())) {
                            ret = Checker.isEmpty(device.getMediaConnections()) ? null : device.getMediaConnections().get(0);
                            break;
                        }
                    }
                }
            }
        }
        if (ret == null) {
            ret = Checker.isEmpty(mediaConnections) ? null : mediaConnections.get(0);
        }
        return ret;
    }

    public Call.WaitReason getWaitReason() {
        LocusStateModel model = getFullState();
        if (model != null && model.isActive()) {
            return Call.WaitReason.WAITING_FOR_ADMITTING;
        }
        return Call.WaitReason.MEETING_NOT_START;
    }

    public LocusModel applyDelta(LocusModel model) {
        LocusModel ret = new LocusModel();
        ret.key = Objects.defaultIfNull(model.key, key);
        ret.created = Objects.defaultIfNull(model.created, created);
        ret.host = Objects.defaultIfNull(model.host, host);
        ret.fullState = Objects.defaultIfNull(model.fullState, fullState);
        ret.controls = Objects.defaultIfNull(model.controls, controls);
        ret.self = Objects.defaultIfNull(model.self, self);
        ret.sequence = model.sequence;
        ret.baseSequence = model.baseSequence;
        ret.syncUrl = model.syncUrl;
        ret.mediaShares = Objects.defaultIfNull(model.mediaShares, mediaShares);
        ret.replaces = Objects.defaultIfNull(model.replaces, replaces);
        ret.conversationUrl = Objects.defaultIfNull(model.conversationUrl, conversationUrl);
        ret.info = Objects.defaultIfNull(model.info, info);
        ret.meeting = Objects.defaultIfNull(model.meeting, meeting);
        ret.aclUrl = Objects.defaultIfNull(model.aclUrl, aclUrl);
        ret.mediaConnections = Objects.defaultIfNull(model.mediaConnections, mediaConnections);
        HashSet<LocusParticipantModel> participants = new HashSet<>();
        if (!Checker.isEmpty(this.participants)) {
            participants.addAll(this.participants);
        }
        if (!Checker.isEmpty(model.participants)) {
            for (LocusParticipantModel participant : model.participants) {
                participants.remove(participant);
                if (!participant.isRemoved()) {
                    participants.add(participant);
                }
            }
        }
        ret.participants = new ArrayList<>(participants);
        return ret;
    }

}
