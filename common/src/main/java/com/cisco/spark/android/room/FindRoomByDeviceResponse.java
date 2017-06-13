package com.cisco.spark.android.room;

import com.cisco.spark.android.room.model.RoomState;

public class FindRoomByDeviceResponse {
    // Set by gson
    protected boolean inRoom;
    protected RoomState roomState;

    public boolean isInRoom() {
        return inRoom;
    }

    public RoomState getRoomState() {
        return roomState;
    }

    public static class Builder {

        private final FindRoomByDeviceResponse object;

        public Builder() {
            object = new FindRoomByDeviceResponse();
            object.inRoom = false;
            object.roomState = null;
        }

        public Builder inRoom() {
            object.inRoom = true;
            return this;
        }

        public Builder withRoomState(RoomState state) {
            object.roomState = state;
            return this;
        }

        public FindRoomByDeviceResponse build() {
            return object;
        }

    }
}
