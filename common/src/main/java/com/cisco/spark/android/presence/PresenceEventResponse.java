package com.cisco.spark.android.presence;

import java.util.Date;

public class PresenceEventResponse {
    private String subject;
    private String eventType;
    private Date expires;

    public String getSubject() {
        return subject;
    }

    public String getEventType() {
        return eventType;
    }

    public Date getExpires() {
        return expires;
    }
}
