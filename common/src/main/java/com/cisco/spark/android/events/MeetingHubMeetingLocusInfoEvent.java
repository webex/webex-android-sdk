package com.cisco.spark.android.events;


import com.cisco.spark.android.locus.model.LocusMeetingInfo;

public class MeetingHubMeetingLocusInfoEvent {
    private LocusMeetingInfo locusMeetingInfo;

    public MeetingHubMeetingLocusInfoEvent(LocusMeetingInfo locusMeetingInfo) {
        this.locusMeetingInfo = locusMeetingInfo;
    }

    public LocusMeetingInfo getLocusMeetingInfo() {
        return locusMeetingInfo;
    }
}
