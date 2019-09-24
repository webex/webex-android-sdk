package com.ciscowebex.androidsdk.membership;

import com.cisco.spark.android.model.ParticipantRoomProperties;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.conversation.Conversation;
import com.ciscowebex.androidsdk.message.internal.WebexId;

import java.util.Date;

public class MembershipReadStatus {

    private Membership _membership;

    private String _lastSeenId;

    private Date _lastSeenDate;

    protected MembershipReadStatus(Conversation conversation, Person person) throws IllegalArgumentException {
        ParticipantRoomProperties roomProperties = person.getRoomProperties();
        if (roomProperties == null) {
            throw new IllegalArgumentException("RoomProperties is null");
        }
        _lastSeenId = new WebexId(WebexId.Type.MESSAGE_ID, roomProperties.getLastSeenActivityUUID()).toHydraId();
        _lastSeenDate = roomProperties.getLastSeenActivityDate();
        _membership = new Membership(conversation, person);
    }

    /**
     * The membership of the space
     * @return the membership of the space
     * @since 2.2.0
     */
    public Membership getMembership() {
        return _membership;
    }

    /**
     * The id of the last message which the member have seen
     * @return the id of the last message which the member have seen
     * @since 2.2.0
     */
    public String getLastSeenId() {
        return _lastSeenId;
    }

    /**
     * The published date of the last message that the member have seen
     * @return the published date of the last message that the member have seen
     * @since 2.2.0
     */
    public Date getLastSeenDate() {
        return _lastSeenDate;
    }
}