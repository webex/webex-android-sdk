package com.ciscowebex.androidsdk.conversation;

import com.ciscowebex.androidsdk.message.internal.WebexId;

import java.io.UnsupportedEncodingException;
import java.util.Date;

public class RoomProperties {

    private Date lastSeenActivityDate;
    private String lastSeenActivityUUID;
    private Date lastAckDate;
    private long lastSelfAckTimestamp;

    public Date getLastSeenActivityDate() {
        return lastSeenActivityDate;
    }

    public void setLastSeenActivityDate(Date lastSeenActivityDate) { this.lastSeenActivityDate = lastSeenActivityDate; }

    public String getLastSeenActivityUUID() {
        return lastSeenActivityUUID;
    }

    public String getLastSeenActivityID() { return this.lastSeenActivityUUID != null? new WebexId(WebexId.Type.MESSAGE_ID,lastSeenActivityUUID).toHydraId() : ""; }

    public void setLastSeenActivityUUID(String lastSeenActivityUUID) { this.lastSeenActivityUUID = lastSeenActivityUUID; }

    public Date getLastAckDate() { return lastAckDate; }

    public void setLastAckDate(Date lastAckDate) { this.lastAckDate = lastAckDate; }

    public long getLastSelfAckTimestamp() { return lastSelfAckTimestamp; }

    public void setLastSelfAckTimestamp(long lastSelfAckTimestamp) { this.lastSelfAckTimestamp = lastSelfAckTimestamp; }
}