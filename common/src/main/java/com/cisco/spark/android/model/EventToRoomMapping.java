package com.cisco.spark.android.model;

import java.util.Date;
import java.util.UUID;

public class EventToRoomMapping {
    public String meetingId;
    public UUID conversationId;
    public Date endDate;

    public EventToRoomMapping(String eventId, UUID conversationId, Date endDate) {
        this.meetingId = eventId;
        this.conversationId = conversationId;
        this.endDate = endDate;
    }
}
