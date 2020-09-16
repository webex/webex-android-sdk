package com.ciscowebex.androidsdk.internal.mercury;

import com.ciscowebex.androidsdk.internal.model.CalendarMeeting;
import com.google.gson.annotations.SerializedName;

public class MercuryMeetingEvent extends MercuryEvent {
    @SerializedName("calendarMeetingExternal")
    private CalendarMeeting calendarMeeting;

    public CalendarMeeting getCalendarMeeting() {
        return calendarMeeting;
    }
}
