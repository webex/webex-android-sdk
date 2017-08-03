package com.cisco.spark.android.events;


import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.model.CalendarMeeting;

public class ObtpShowEvent {
    private Locus locus;
    private CalendarMeeting calendarMeeting;

    public ObtpShowEvent(Locus locus, CalendarMeeting calendarMeeting) {
        this.locus = locus;
        this.calendarMeeting = calendarMeeting;
    }

    public Locus getLocus() {
        return locus;
    }

    public CalendarMeeting getCalendarMeeting() {
        return calendarMeeting;
    }
}
