package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlMeetingControlsLockEvent extends CallControlMeetingControlsStatusEvent {

    // Locked or unlocked - true means locked and false means unlocked.
    private boolean locked;

    public CallControlMeetingControlsLockEvent(LocusKey locuskey, boolean status, boolean locked) {
        super(locuskey, null, status);
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }
}
