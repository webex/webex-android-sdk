package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlMeetingControlsEvent {

    public enum Actor {
        SELF("self"),
        MODERATOR("moderator");

        String actorType;

        Actor(String actorType) {
            this.actorType = actorType;
        }

        public String getActor() {
            return actorType;
        }
    }

    private Actor actor;
    private final LocusKey locusKey;

    public CallControlMeetingControlsEvent() {
        actor = null;
        locusKey = null;
    }

    public CallControlMeetingControlsEvent(LocusKey locusKey, Actor actor) {
        this.actor = actor;
        this.locusKey = locusKey;
    }

    public Actor getActor() {
        return actor;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
