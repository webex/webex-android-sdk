package com.cisco.spark.android.locus.model;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.cisco.spark.android.util.DateUtils;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Locus {

    // fields set by gson
    @SerializedName("url")
    private LocusKey key;
    private Date created;
    private LocusParticipantInfo host;
    private LocusState fullState = new LocusState(false, 0, LocusState.State.INACTIVE);
    private LocusControl controls;
    private List<LocusParticipant> participants = new ArrayList<LocusParticipant>();
    private LocusSelfRepresentation self;
    private LocusSequenceInfo sequence;
    private LocusSequenceInfo baseSequence;
    private String syncUrl;
    private List<MediaShare> mediaShares = new ArrayList<>();
    private List<LocusReplaces> replaces = new ArrayList<>();
    private String conversationUrl;
    private LocusDescription info;
    private LocusScheduledMeeting meeting;
    private Uri aclUrl;

    private final static String SUGGESTEDMEDIA_DIRECTION_SENDRECV = "sendrecv";
    private final static String SUGGESTEDMEDIA_DIRECTION_INACTIVE = "inactive";
    private final static String SUGGESTEDMEDIA_MEDIACONTENT_MAIN = "main";
    private final static String SUGGESTEDMEDIA_MEDIACONTENT_SLIDES = "slides";

    /*
     * Please note that clients implementing permanent loci MUST ignore this enum
     * Older clients will only have two states, STARTED or ENDED
     * The additional enum fields are there as a bridge between transient loci and permanent loci
     * and clients will NEVER see ACTIVE or INACTIVE in the "state" field of the Locus JSON
     * This state field will go away once all clients migrate to permanent loci
     */
    public static enum State {
        STARTED, ENDED, ACTIVE, INACTIVE
    }

    /**
     * Returns the time the locus was created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * returns information about the entity that created the locus
     * The email address indicates the contact information about the entity that created the locus
     * If a Squared user created the locus, the uuid is also populated
     *
     * @return Host information
     */
    public LocusParticipantInfo getHost() {
        return host;
    }

    /**
     * returns the full state of the locus which includes
     * whether the locus is active or not, the number of user currently in the locus (JOINED)
     * and the last time the locus was active.
     *
     * @return the current state of the locus
     */
    public LocusState getFullState() {
        return fullState;
    }

    public LocusControl getControls() {
        return controls;
    }

    public List<LocusParticipant> getParticipants() {
        return this.participants;
    }


    public @Nullable LocusSelfRepresentation getSelf() {
        return this.self;
    }

    public LocusKey getKey() {
        return key;
    }

    public LocusSequenceInfo getSequence() {
        return sequence;
    }

    public List<MediaShare> getMediaShares() {
        return mediaShares;
    }

    public LocusSequenceInfo getBaseSequence() {
        return baseSequence;
    }

    public String getSyncUrl() {
        return syncUrl;
    }

    public List<LocusReplaces> getReplaces() {
        return replaces;
    }

    public String getConversationUrl() {
        return conversationUrl;
    }

    public LocusDescription getInfo() {
        return info;
    }

    public LocusScheduledMeeting getMeeting() {
        return meeting;
    }

    public Uri getAclUrl() {
        return aclUrl;
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

    public boolean isJoinedFromThisDevice(Uri deviceUrl) {
        if (getSelf() != null) {
            if (getFullState().isActive() && getSelf().getState().equals(LocusParticipant.State.JOINED)) {
                List<LocusParticipantDevice> deviceList = getSelf().getDevices();
                if (deviceList != null) {
                    for (LocusParticipantDevice device : deviceList) {
                        if (device.getUrl().equals(deviceUrl)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isJoinedFromOtherDevice(Uri deviceUrl) {
        if (getSelf() != null) {
            if (getFullState().isActive() && getSelf().getState().equals(LocusParticipant.State.JOINED)) {
                List<LocusParticipantDevice> deviceList = getSelf().getDevices();
                if (deviceList != null) {
                    for (LocusParticipantDevice device : deviceList) {
                        if (!device.getUrl().equals(deviceUrl)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isInLobbyFromThisDevice(Uri deviceUrl) {
        if (getSelf() != null && getSelf().getState().equals(LocusParticipant.State.IDLE)) {
            List<LocusParticipantDevice> deviceList = getSelf().getDevices();
            if (deviceList != null) {
                for (LocusParticipantDevice device : deviceList) {
                    if (device.getUrl().equals(deviceUrl) &&
                            device.getIntent() != null &&
                            device.getIntent().getType() != null &&
                            device.getIntent().getType().equals(LocusParticipant.IntentType.WAIT)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public LocusParticipant.Intent getIntent(Uri deviceUrl) {
        if (getSelf() != null) {
            List<LocusParticipantDevice> deviceList = getSelf().getDevices();
            if (deviceList != null) {
                for (LocusParticipantDevice device : deviceList) {
                    if (device.getUrl().equals(deviceUrl)) {
                        return device.getIntent();
                    }
                }
            }
        }
        return null;
    }


    public boolean isSelfJoinedFromDeviceType(String deviceType) {
        LocusParticipant participant = getSelf();
        if (participant == null)
            return false;

        return isParticipantJoinedFromDeviceType(participant, deviceType);
    }

    /**
     * Use to check if the remote participant in a one-on-one is joined from device
     */
    public boolean isRemoteParticipantJoinedFromDeviceType(String deviceType) {
        for (LocusParticipant participant : getParticipants()) {
            // Ignore self
            if (getSelf().getId().equals(participant.getId()))
                continue;
            return isParticipantJoinedFromDeviceType(participant, deviceType);
        }
        return false;
    }

    public boolean isParticipantJoinedFromDeviceType(LocusParticipant participant, String deviceType) {
        LocusParticipant.State state = participant.getState();
        // self.state is leaving a brief while when the incoming share is ended.
        if (getFullState().isActive() && (state.equals(LocusParticipant.State.JOINED) || state.equals(LocusParticipant.State.LEAVING))) {
            List<LocusParticipantDevice> deviceList = participant.getDevices();
            if (deviceList != null) {
                for (LocusParticipantDevice device : deviceList) {
                    if (device.getDeviceType() != null && device.getDeviceType().equalsIgnoreCase(deviceType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public MediaShare getGrantedFloor() {
        if (getMediaShares() != null) {
            for (MediaShare mediaShare : getMediaShares()) {
                if (mediaShare.isValidType() && mediaShare.getFloor() != null && Floor.GRANTED.equals(mediaShare.getFloor().getDisposition())) {
                    return mediaShare;
                }
            }
        }
        return null;
    }

    public MediaShare getReleasedFloor() {
        if (getMediaShares() != null) {
            for (MediaShare mediaShare : getMediaShares()) {
                if (mediaShare.isValidType() && mediaShare.getFloor() != null && Floor.RELEASED.equals(mediaShare.getFloor().getDisposition())) {
                    return mediaShare;
                }
            }
        }
        return null;
    }

    public MediaShare getShareContentMedia() {
        if (getMediaShares() != null) {
            for (MediaShare mediaShare : getMediaShares()) {
                if (mediaShare.isValidType() && MediaShare.SHARE_CONTENT_TYPE.equals(mediaShare.getName())) {
                    return mediaShare;
                }
            }
        }
        return null;
    }

    public boolean isFloorGranted() {
        Floor floor = getFloor();
        return floor != null && Floor.GRANTED.equals(floor.getDisposition());
    }

    /**
     * Returns the whiteboard media share whether floor has been granted or not.
     */
    public MediaShare getWhiteboardMedia() {
        if (getMediaShares() != null) {
            for (MediaShare mediaShare : getMediaShares()) {
                if (mediaShare.isWhiteboard()) {
                    return mediaShare;
                }
            }
        }
        return null;
    }

    private Floor getFloor() {
        MediaShare content = getGrantedFloor();
        if (content != null) {
            return content.getFloor();
        }
        return null;
    }



    public boolean isFloorReleased() {
        if (getMediaShares() != null) {
            for (MediaShare mediaShare : getMediaShares()) {
                if (mediaShare.isValidType() && mediaShare.getFloor() != null && Floor.RELEASED.equals(mediaShare.getFloor().getDisposition())) {
                    return true;
                }
            }
        }
        return false;
    }

    public LocusParticipant getFloorBeneficiary() {
        Floor floor = getFloor();
        return floor != null ? floor.getBeneficiary() : null;
    }

    public boolean isHeld() {
        if (getSelf() != null && getSelf().getIntents() != null) {
            for (LocusParticipant.Intent intent : getSelf().getIntents()) {
                if (intent.getType().equals(LocusParticipant.IntentType.HELD)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<LocusParticipantInfo> getUsers() {
        List<LocusParticipantInfo> users = new ArrayList<>();
        for (LocusParticipant participant : participants) {
            users.add(participant.getPerson());
        }
        return users;
    }



    public boolean isMeeting() {
        return fullState.getType() == LocusState.Type.MEETING;
    }

    public boolean isSparkSpaceMeeting() {
        return isMeeting() && hasConversationUrl();
    }

    public boolean hasConversationUrl() {
        return conversationUrl != null;
    }

    public void setAclUrl(Uri aclUrl) {
        this.aclUrl = aclUrl;
    }

    public static class Builder {
        private Locus locus;

        public Builder() {
            locus = new Locus();
            locus.self = new LocusSelfRepresentation();
            locus.sequence = new LocusSequenceInfo();
            locus.info = new LocusDescription();
        }

        public Builder setKey(LocusKey key) {
            locus.key = key;
            return this;
        }

        public Locus build() {
            return locus;
        }

        public Builder addParticipant(LocusParticipant participant) {
            locus.participants.add(participant);
            return this;
        }

        public Builder setFloorGranted() {
            return setFloorChanged(MediaShare.SHARE_CONTENT_TYPE, Floor.GRANTED);
        }

        public Builder setFloorReleased() {
            return setFloorChanged(MediaShare.SHARE_CONTENT_TYPE, Floor.RELEASED);
        }

        public Builder setFloorChanged(String type, String floorState) {
            MediaShare mediaShare = new MediaShare(Uri.parse(""), new Floor(floorState));
            mediaShare.setName(type);
            locus.mediaShares = new ArrayList<>();
            locus.mediaShares.add(mediaShare);
            return this;
        }

        public Builder setFloorBeneficiary(String type, LocusParticipant beneficiary) {
            if (locus.getMediaShares() != null) {
                for (MediaShare mediaShare : locus.getMediaShares()) {
                    if (mediaShare.getName().equals(type) && mediaShare.getFloor() != null) {
                        mediaShare.getFloor().setBeneficiary(beneficiary);
                    }
                }
            }
            return this;
        }

        public Builder setSelfRepresentation(LocusSelfRepresentation self) {
            locus.self = self;
            return this;
        }

        public Builder setParticipantControlsMutedState(boolean muted) {
            LocusParticipantAudioControl locusParticipantAudioControl = new LocusParticipantAudioControl(muted, null);
            locus.getSelf().setControls(new LocusParticipantControls(null, null, locusParticipantAudioControl, null));
            return this;
        }

        public Builder addAlertType(String action, Date expiration) {
            LocusSelfRepresentation.AlertType alertType = new LocusSelfRepresentation.AlertType();
            alertType.setAction(action);
            alertType.setExpiration(expiration);
            locus.getSelf().setAlertType(alertType);
            return this;
        }

        public Builder setDate(Date created) {
            locus.created = created;
            return this;
        }

        public Builder setLocusParticipantHostInfo(LocusParticipantInfo host) {
            locus.host = host;
            return this;
        }

        public Builder setLocusFullState(LocusState fullState) {
            locus.fullState = fullState;
            return this;
        }

        public Builder setLocusControl(LocusControl controls) {
            locus.controls = controls;
            return this;
        }

        public Builder setParticipants(List<LocusParticipant> participants) {
            locus.participants = participants;
            return this;
        }

        public Builder setLocusSequenceInfo(LocusSequenceInfo sequence) {
            locus.sequence = sequence;
            return this;
        }

        public Builder setLocusBaseSequenceInfo(LocusSequenceInfo baseSequence) {
            locus.baseSequence = baseSequence;
            return this;
        }

        public Builder setSyncUrl(String syncUrl) {
            locus.syncUrl = syncUrl;
            return this;
        }

        public Builder setMediaShares(List<MediaShare> mediaShares) {
            locus.mediaShares = mediaShares;
            return this;
        }

        public Builder setLocusReplaces(List<LocusReplaces> replaces) {
            locus.replaces = replaces;
            return this;
        }

        public Builder setConversationUrl(String conversationUrl) {
            locus.conversationUrl = conversationUrl;
            return this;
        }

        public Builder setLocusDescriptionInfo(LocusDescription info) {
            locus.info = info;
            return this;
        }

        public Builder setLocusScheduledMeeting(LocusScheduledMeeting meeting) {
            locus.meeting = meeting;
            return this;
        }

        public Builder setFullState(LocusState fullState) {
            locus.fullState = fullState;
            return this;
        }

    }
}
