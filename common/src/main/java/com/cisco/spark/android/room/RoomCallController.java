package com.cisco.spark.android.room;

import com.cisco.spark.android.room.model.RoomState;
import com.github.benoitdion.ln.Ln;

/**
 * Maintains the state of the paired room call
 */
public class RoomCallController {

    private Self selfState;

    private Call callState;

    private Room roomState;

    // This is set if we initiated a call with the paired room
    private boolean callUsingRoom;

    // Used if we have lost pairing so we know which room we used to call with
    private String callUsingRoomUuid;
    private String callUsingRoomName;

    // Previous
    private boolean previousCallWasConnected;

    enum Call {
        NONE,
        NOTIFYING,
        CONNECTED,
        DISCONNECTED
    }

    enum Self {
        NONE,
        CONNECTED,
        DISCONNECTED
    }

    // This is probably overkill - but experimenting to find the appropriate states needed
    enum Room {
        NONE,
        JOINED_CALL,
        LEFT_CALL
    }

    public RoomCallController() {
        clearState();
    }

    /**
     * This is the initial action taken on a new call
     */
    public void setCallingUsingRoom(boolean callUsingRoom, RoomState roomState) {
        clearState();
        Ln.d("setCallingUsingRoom = %s", callUsingRoom);
        this.callUsingRoom = callUsingRoom;
        if (callUsingRoom && roomState != null) {
            this.callUsingRoomUuid = roomState.getRoomIdentity().toString();
            this.callUsingRoomName = roomState.getRoomName();
        }
    }

    public void setCallNotifying() {
        Ln.d("setNotifying call is now notifying - consider self connected");
        callState = Call.NOTIFYING;
        selfState = Self.CONNECTED;
    }

    public boolean isRoomInCall() {
        return roomState == Room.JOINED_CALL;
    }

    public void setRoomLeft() {
        Ln.d("setRoomLeft");
        roomState = Room.LEFT_CALL;

        if (!isSelfConnected()) { // Self left, and room is not in call -> call disconnected
            Ln.d("setRoomLeft - both self and room left - call disconnected");
            callState = Call.DISCONNECTED;
        }
    }

    private boolean isSelfConnected() {
        return selfState == Self.CONNECTED;
    }

    public void setRoomJoined() {
        Ln.d("setRoomJoined");
        roomState = Room.JOINED_CALL;
    }

    public void setCallConnected() {
        Ln.d("setCallConnected");
        callState = Call.CONNECTED;
    }

    public boolean isCallConnected() {
        return callState == Call.CONNECTED;
    }

    public boolean wasPreviousCallConnected() {
        return previousCallWasConnected;
    }

    public void setSelfLeftCall() {

        Ln.d("setSelfLeftCall");

        // This stores the state so we can query it for call rating purposes
        if (isCallConnected()) {
            this.previousCallWasConnected = true;
        }

        if (!isRoomInCall()) { // Self left, and room is not in call, clear call state.
            Ln.d("setSelfLeftCall - both self and room left - call disconnected");
            callState = Call.DISCONNECTED;
        }

        selfState = Self.DISCONNECTED;
    }

    public boolean isCallingUsingRoom() {
        Ln.d("isCallingUsingRoom = %s", callUsingRoom);
        return callUsingRoom;
    }

    public String getCallingUsingRoomUuid() {
        return callUsingRoomUuid;
    }

    public String getCallingUsingRoomName() {
        return  callUsingRoomName;
    }

    private void clearState() {
        Ln.d("clearState");

        callState = Call.NONE;
        selfState = Self.NONE;
        roomState = Room.NONE;

        callUsingRoom = false;
        callUsingRoomUuid = null;

        previousCallWasConnected = false;

    }

}
