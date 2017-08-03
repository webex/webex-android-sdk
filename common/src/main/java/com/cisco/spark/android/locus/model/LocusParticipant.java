package com.cisco.spark.android.locus.model;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocusParticipant {
    // Fields set by gson
    protected UUID id;
    protected Uri url;
    protected boolean isCreator;
    protected LocusParticipantInfo person;
    protected State state = State.IDLE;
    protected Uri deviceUrl;
    protected Type type;
    protected Reason reason;
    protected boolean guest;
    protected Uri invitedBy;
    protected List<LocusParticipantDevice> devices = new ArrayList<LocusParticipantDevice>();
    protected LocusParticipantStatus status;
    protected List<Intent> intents;
    protected LocusParticipantControls controls;
    protected List<SuggestedMedia> suggestedMedia;
    protected boolean moderator;
    protected boolean resourceGuest;
    private boolean removed;

    public LocusParticipant() {
    }

    public LocusParticipant(UUID id, State state) {
        this.id = id;
        this.state = state;
    }

    public enum Type {
        USER, ANONYMOUS, RESOURCE_ROOM, MEETING_BRIDGE
    }

    public enum IntentType {
        HELD,
        HOLDING,
        JOIN,
        LEAVE,
        MOVE_MEDIA,
        NONE,
        OBSERVE,
        WAIT };

    public static class Intent {
        private UUID id;
        private IntentType type;
        private String associatedWith;

        public Intent(UUID id, IntentType type, String associatedWith) {
            this.id = id;
            this.type = type;
            this.associatedWith = associatedWith;
        }

        public UUID getId() {
            return id;
        }

        public IntentType getType() {
            return type;
        }

        public String getAssociatedWith() {
            return associatedWith;
        }
    }


    public enum State {
        /* the participant is not in the locus */
        IDLE,

        /* one of more devices of the participant are alerting */
        NOTIFIED,

        /* the participant is in the locus */
        JOINED,

        /* the participant is not in the locus */
        LEFT,

        /* the participant declined to join */
        DECLINED,

        /* local client state indicating we're in process of leaving locus */
        LEAVING
    }

    public enum Reason {
        REMOTE,
        RESOURCE_BUSY,
        ANSWERED_ELSEWHERE,
        INVALID_PIN,
        MOVED,
        RESOURCE_TIMED_OUT,
        NOT_FOUND,
        FORCED,
        INVALID_JOIN_TIME,
        INTENT_CANCELED,
        MEDIA_RESOURCE_RECONNECT,
        INTENT_EXPIRED,
        JOURNAL_TOO_LARGE,
        UNKNOWN,
        INACTIVE,
        NOT_ACCEPTABLE,
        REPLACED,
        MEETING_FULL,
        UNREACHABLE,
        UNANSWERED,
        RESOURCE_ERROR,
        BUSY,
        DND,
        LOBBY_EXPIRED,
        GARBAGE_COLLECTED,
        LOGIN_REQUIRED,
        MEETING_LOCKED,
        CALL_MAX_DURATION,
        MEETING_ENDED,
        RESOURCE_DECLINED
    }

    public Uri getUrl() {
        return url;
    }

    public Uri getDeviceUrl() {
        return this.deviceUrl;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public UUID getId() {
        return this.id;
    }

    public boolean isCreator() {
        return this.isCreator;
    }

    public LocusParticipantInfo getPerson() {
        return this.person;
    }

    public Type getType() {
        return type;
    }

    public List<LocusParticipantDevice> getDevices() {
        return devices;
    }

    public LocusParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(LocusParticipantStatus newStatus) {
        status = newStatus;
    }

    public boolean isGuest() {
        return guest;
    }

    public Uri getInvitedBy() {
        return invitedBy;
    }

    public boolean isRoom() {
        return Type.RESOURCE_ROOM == type;
    }

    public boolean isJoined() {
        return State.JOINED == state;
    }

    public boolean isNotified() {
        return getState() == LocusParticipant.State.NOTIFIED;
    }

    public LocusParticipantControls getControls() {
        return controls;
    }

    public void setControls(LocusParticipantControls controls) {
        this.controls = controls;
    }

    public boolean isModerator() {
        return moderator;
    }

    public void setModerator(boolean moderator) {
        this.moderator = moderator;
    }

    public boolean isResourceGuest() {
        return resourceGuest;
    }

    public boolean isRemoved() {
        return removed;
    }

    /**
     * Get an unique reference for a room resource
     * Currently the assumption is that emails are not unique so we should use the UUID.
     *
     * @return room id
     */
    @Nullable
    public String getRoomIdentifier() {
        LocusParticipantInfo person = getPerson();
        if (person != null) {
            return person.getId();
        }
        return null;
    }

    public boolean isValidCiUser() {
        return (getType() == LocusParticipant.Type.USER && !getPerson().isExternal()) ||
               getType() == LocusParticipant.Type.RESOURCE_ROOM;
    }

    public boolean isVideoMuted() {
        return MediaDirection.RECVONLY.equals(getStatus().getVideoStatus());
    }

    public boolean isAudioMuted() {
        return MediaDirection.RECVONLY.equals(getStatus().getAudioStatus());
    }

    public List<SuggestedMedia> getSuggestedMedia() {
        return suggestedMedia;
    }


    public List<Intent> getIntents() {
        return intents;
    }

    public boolean isWaiting() {
        return hasIntent(IntentType.WAIT);
    }

    public boolean isObserving() {
        return hasIntent(IntentType.OBSERVE);
    }

    private boolean hasIntent(IntentType intentType) {
        for (LocusParticipantDevice device : devices) {
            if (device.getIntent() != null && device.getIntent().getType() != null &&
                device.getIntent().getType().equals(intentType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInLobby() {
        return state.equals(State.IDLE) && isWaiting();
    }

    public boolean isMySelf(UUID myId) {
        return getId().equals(myId);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof LocusParticipant) {
            LocusParticipant that = (LocusParticipant) object;
            return this.id.equals(that.id);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class Builder {
        private LocusParticipant participant;

        public Builder() {
            participant = new LocusParticipant();
            participant.status = new LocusParticipantStatus(MediaDirection.SENDRECV, MediaDirection.SENDRECV, null);
        }

        public Builder setId(UUID id) {
            participant.id = id;
            return this;
        }

        /**
         * Plain UnitTests cannot use the android.net.Uri field so use caution, and only set this
         * field in Robolectric tests.
         */
        public Builder setUrl(String uri) {
            participant.url = Uri.parse(uri);
            return this;
        }

        public Builder setPerson(LocusParticipantInfo person) {
            participant.person = person;
            return this;
        }

        public Builder setState(State state) {
            participant.state = state;
            return this;
        }

        public Builder setReason(Reason reason) {
            participant.reason = reason;
            return this;
        }

        public Builder setType(Type type) {
            participant.type = type;
            return this;
        }

        public Builder setMediaDirection(MediaDirection audioDirection, MediaDirection videoDirection) {
            participant.status = new LocusParticipantStatus(audioDirection, videoDirection, null);
            return this;
        }

        public Builder setParticipantControls(LocusParticipantControls controls) {
            participant.controls = controls;
            return this;
        }

        public Builder setDeviceUrl(Uri deviceUrl) {
            participant.deviceUrl = deviceUrl;
            return this;
        }

        public Builder setStatus(LocusParticipantStatus status) {
            participant.status = status;
            return this;
        }

        public Builder addIntent(Intent intent) {
            if (participant.intents == null) {
                participant.intents = new ArrayList<>();
            }
            participant.intents.add(intent);
            return this;
        }

        public Builder addDevice(LocusParticipantDevice device) {
            if (participant.devices == null) {
                participant.devices = new ArrayList<>();
            }
            participant.devices.add(device);
            return this;
        }

        public Builder setCreator(boolean isCreator) {
            participant.isCreator = isCreator;
            return this;
        }

        public Builder setControls(LocusParticipantControls controls) {
            participant.controls = controls;
            return this;
        }

        public Builder setGuest(boolean isGuest) {
            participant.guest = isGuest;
            return this;
        }

        public Builder setResourceGuest(boolean isResourceGuest) {
            participant.resourceGuest = isResourceGuest;
            return this;
        }

        public Builder setModerator(boolean isModerator) {
            participant.moderator = isModerator;
            return this;
        }

        public LocusParticipant build() {
            return participant;
        }
    }
}
