package com.cisco.spark.android.presence;


import java.util.ArrayList;
import java.util.Date;

public class PresenceUserEvents {
    private ArrayList<Event> events;

    public Event getEventForSubject(String userId) {
        for (Event event : events) {
            if (event.getSubject().equals(userId)) {
                return event;
            }
        }

        return null;
    }

    public static class Event {
        private String subject;
        private PresenceStatus eventType;
        private int ttl;
        private Date startTime;
        private Date received;
        private Date expiration;

        public String getSubject() {
            return subject;
        }

        public PresenceStatus getEventType() {
            return eventType;
        }

        public int getTtl() {
            return ttl;
        }

        public Date getStartTime() {
            return startTime;
        }

        public Date getReceived() {
            return received;
        }

        public Date getExpiration() {
            return expiration;
        }
    }
}
