package com.cisco.spark.android.room;

import java.util.Locale;

public class RoomUpdatedEvent {
    public enum RoomState {
        UNPAIRED,
        PAIRED,
        CONNECTING,
        CONNECTED
    }

    private String roomName;
    private String conversationTitle;
    private RoomState roomState;
    private int participantsCount;
    private boolean isLocalMedia;
    private boolean muted;
    private boolean available;

    public static class Builder {

        private final RoomUpdatedEvent roomUpdatedEvent;

        public Builder(String roomName) {
            roomUpdatedEvent = new RoomUpdatedEvent(roomName);
        }

        public Builder withAvailableForCall(boolean available) {
            roomUpdatedEvent.available = available;
            return this;
        }

        public Builder withOtherParticipantsCount(int count) {
            roomUpdatedEvent.participantsCount = count;
            return this;
        }

        public RoomUpdatedEvent build() {
            return roomUpdatedEvent;
        }
    }

    private RoomUpdatedEvent(String roomName) {
        this.roomName = roomName;
        this.roomState = RoomState.PAIRED;
    }

    public RoomUpdatedEvent(String roomName, RoomState roomState, String conversationTitle, boolean isLocalMedia, boolean muted) {
        this.roomName = roomName;
        this.conversationTitle = conversationTitle;
        this.roomState = roomState;
        this.isLocalMedia = isLocalMedia;
        this.muted = muted;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public RoomState getRoomState() {
        return roomState;
    }

    public boolean isLocalMedia() {
        return isLocalMedia;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "RoomUpdatedEvent isAvailable=%s othersCount=%d state=%s name=%s", available, participantsCount, roomState, roomName);
    }
}
