package com.ciscowebex.androidsdk.conversation;

import com.ciscowebex.androidsdk.message.internal.WebexId;

import java.io.UnsupportedEncodingException;

public class Actor extends ActivityObject {

    protected String entryUUID;
    protected String emailAddress;
    protected String type;

    protected RoomProperties roomProperties;

    public RoomProperties getRoomProperties() {
        return roomProperties;
    }

    public void setRoomProperties(RoomProperties roomProperties) {
        this.roomProperties = roomProperties;
    }

    Actor(String entryUUID) {
        this.entryUUID = entryUUID;
    }

    public String getEntryUUID() {
        return entryUUID;
    }

    public String getPersonId()  { return entryUUID!=null ? new WebexId(WebexId.Type.PEOPLE_ID,entryUUID).toHydraId() : null; }

    public void setEntryUUID(String entryUUID) {
        this.entryUUID = entryUUID;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
