package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlMeetingControlsExpelEvent extends CallControlMeetingControlsStatusEvent {
    public CallControlMeetingControlsExpelEvent(LocusKey locuskey, boolean status) {
        super(locuskey, null, status);
    }
}
