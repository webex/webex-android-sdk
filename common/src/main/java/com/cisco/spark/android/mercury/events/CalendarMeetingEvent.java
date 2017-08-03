package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.model.CalendarMeeting;
import com.google.gson.annotations.SerializedName;

public class CalendarMeetingEvent extends MercuryData {
    @SerializedName("calendarMeetingExternal")
    private CalendarMeeting calendarMeeting;

    public CalendarMeetingEvent(MercuryEventType eventType) {
        super(eventType);
    }

    public CalendarMeeting getCalendarMeeting() {
        return calendarMeeting;
    }

    public void setCalendarMeeting(CalendarMeeting calendarMeeting) {
        this.calendarMeeting = calendarMeeting;
    }
}
