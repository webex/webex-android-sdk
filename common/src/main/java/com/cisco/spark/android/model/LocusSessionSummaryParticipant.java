package com.cisco.spark.android.model;

public class LocusSessionSummaryParticipant {
    public enum State { IDLE, NOTIFIED, LEFT, DECLINED }

    private Person person;
    private boolean isInitiator;
    private long duration;
    private State state;

    public LocusSessionSummaryParticipant(Person person, boolean isInitiator, long duration, State state) {
        this.person = person;
        this.isInitiator = isInitiator;
        this.duration = duration;
        this.state = state;
    }

    public Person getPerson() {
        return person;
    }

    public boolean isInitiator() {
        return isInitiator;
    }

    public long getDuration() {
        return duration;
    }

    public State getState() {
        return state;
    }
}
