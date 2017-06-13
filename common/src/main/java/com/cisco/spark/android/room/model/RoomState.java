package com.cisco.spark.android.room.model;

import android.net.Uri;

import java.util.List;
import java.util.UUID;

public class RoomState {

    private Uri roomUrl;
    private Uri callUri;
    private Uri bringRoomIntoCallUrl;
    private Uri usersUrl;
    private String roomName;
    private int maxResponseValidityInSeconds;
    private List<RoomUser> users;
    private boolean availableForCall;
    private UUID roomIdentity;
    private Status status;

    public Uri getRoomUrl() {
        return roomUrl;
    }

    public Uri getCallUri() {
        return callUri;
    }

    public Uri getUsersUrl() {
        return usersUrl;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomType() {
        return "Cloudberry";
    }

    public int getMaxResponseValidityInSeconds() {
        return maxResponseValidityInSeconds;
    }

    public List<RoomUser> getUsers() {
        return users;
    }

    public Uri getBringRoomIntoCallUrl() {
        return bringRoomIntoCallUrl;
    }

    public boolean isAvailableForCall() {
        return availableForCall;
    }

    public void setRoomName(String name) {
        this.roomName = name;
    }

    public void setUsers(List<RoomUser> users) {
        this.users = users;
    }

    public void setAvailableForCall(boolean available) {
        this.availableForCall = available;
    }

    public UUID getRoomIdentity() {
        return this.roomIdentity;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setRoomIdentity(UUID roomIdentity) {
        this.roomIdentity = roomIdentity;
    }

    public void setRoomUrl(Uri uri) {
        this.roomUrl = uri;
    }

    public class Status {
        private boolean availableForCall;
        private boolean availableForOutOfCallShare;
        private boolean ongoingCall;
        private boolean ongoingShare;

        public boolean getAvailableForCall() {
            return  availableForCall;
        }
        public boolean getAvailableForOutOfCallShare() {
            return  availableForOutOfCallShare;
        }
        public boolean getOngoingCall() {
            return  ongoingCall;
        }
        public boolean getOngoingShare() {
            return  ongoingShare;
        }

        public void setAvailableForCall(boolean availableForCall) {
            this.availableForCall = availableForCall;
        }

        public void setAvailableForOutOfCallShare(boolean availableForOutOfCallShare) {
            this.availableForOutOfCallShare = availableForOutOfCallShare;
        }

        public void setOngoingCall(boolean ongoingCall) {
            this.ongoingCall = ongoingCall;
        }

        public void setOngoingShare(boolean ongoingShare) {
            this.ongoingShare = ongoingShare;
        }
    }
}
