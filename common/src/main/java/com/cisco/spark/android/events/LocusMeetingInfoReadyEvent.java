package com.cisco.spark.android.events;


import com.cisco.spark.android.locus.model.LocusMeetingInfo;

public class LocusMeetingInfoReadyEvent {

    public LocusMeetingInfo locusMeetingInfo;

    public LocusMeetingInfoReadyEvent(LocusMeetingInfo locusMeetingInfo) {
        this.locusMeetingInfo = locusMeetingInfo;
    }
}
