package com.cisco.spark.android.meetings;

public class WhistlerMeetingResponse {

    // Fields set by gson
    private WhistlerMeetingInfo meeting;

    public WhistlerMeetingInfo getMeeting() {
        return meeting;
    }

    public class WhistlerMeetingInfo {

        private boolean hostPresent;

        public boolean isHostPresent() {
            return hostPresent;
        }
    }
}
