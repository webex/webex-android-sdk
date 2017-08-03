package com.cisco.spark.android.meetings;

import android.text.TextUtils;

public class WhistlerRequestMetaData {

    // Required
    private String emailAddress;

    // Use for Scheduled WebEx Meeting. Not needed for PMR
    private String startTime;
    private int meetingDuration;
    private String meetingTitle;

    public String getEmailAddress() {
        return emailAddress;
    }

    public WhistlerRequestMetaData(String emailAddress) {
        if (TextUtils.isEmpty(emailAddress)) {
            throw new IllegalArgumentException("Must supply a non-null email address.");
        }
        this.emailAddress = emailAddress;
    }

    // In pattern of "yyyy-mm-ddTHH:mm:ss.SSS'Z'"
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    // In minutes. Whistler defaults to 30 minutes
    public void setMeetingDuration(int meetingDuration) {
        this.meetingDuration = meetingDuration;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    @Override
    public String toString() {
        return "WhistlerRequestMetaData{" +
                "emailAddress='" + emailAddress + '\'' +
                ", startTime='" + startTime + '\'' +
                ", meetingDuration=" + meetingDuration +
                ", meetingTitle='" + meetingTitle + '\'' +
                '}';
    }
}
