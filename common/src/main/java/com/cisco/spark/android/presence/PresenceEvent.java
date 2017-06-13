package com.cisco.spark.android.presence;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PresenceEvent {
    private String subject;
    private PresenceStatus eventType;
    private int ttl;
    private Date startTime;

    public PresenceEvent(String subject, PresenceStatus eventType) {
        this(subject, eventType, (int) TimeUnit.MINUTES.toSeconds(10), (eventType == PresenceStatus.PRESENCE_STATUS_ACTIVE) ? null : new Date());
    }

    public PresenceEvent(String subject, PresenceStatus eventType, int ttl, Date startTime) {
        this.subject = subject;
        this.eventType = eventType;
        this.ttl = ttl;
        this.startTime = startTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String value) {
        this.subject = value;
    }

    public PresenceStatus getEventType() {
        return eventType;
    }

    public void setEventType(PresenceStatus value) {
        this.eventType = value;
    }

    public int getTtl() {
        return ttl;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date value) {
        this.startTime = value;
    }
}
