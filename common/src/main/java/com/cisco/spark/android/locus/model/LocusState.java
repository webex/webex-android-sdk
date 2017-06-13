package com.cisco.spark.android.locus.model;

import java.util.Date;

public class LocusState {
    // fields set by gson
    private boolean active;
    private int count;
    private Date lastActive;
    private State state;
    private boolean startingSoon;
    private Type type;

    public enum State {
        TERMINATING,
        INACTIVE,
        INITIALIZING,
        ACTIVE
    }

    public enum Type {
        CALL,
        MEETING,
        SIP_BRIDGE
    }

    public LocusState(boolean active, int count, State state) {
        this.active = active;
        this.count = count;
        this.state = state;
    }

    public LocusState(boolean active, int count, State state, Type type) {
        this(active, count, state);
        this.type = type;
    }

    /**
     * This is set to true if the locus is active
     */
    public boolean isActive() {
        return active || (state != null && state.equals(State.INITIALIZING));
    }


    public State getState() {
        return state;
    }

    /**
     * returns the current number of participants in the locus. This number could be larger than the number of
     * participants described in the <tt>LocusRoster</tt> in situations where the locus is really large
     */
    public int getCount() {
        return count;
    }

    /**
     * The last time the locus was Active
     * This value is as follows:
     * If the locus is currently active, the time when it went active
     * If the locus is currently not active, the time when the locus went inactive
     */
    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    /**
     *
     * @return boolean value representing if the associated meeting is starting soon or not
     */
    public boolean isStartingSoon() {
        return startingSoon;
    }

    public Type getType() {
        return type;
    }
}
