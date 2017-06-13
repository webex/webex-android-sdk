package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlMeetingControlsStatusEvent extends CallControlMeetingControlsEvent {

    // Status - true for success and false for failure.
    private boolean status;

    public CallControlMeetingControlsStatusEvent(LocusKey locuskey, Actor actor, boolean status) {
        super(locuskey, actor);
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }
}

