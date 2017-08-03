package com.cisco.spark.android.meetings;

import android.provider.CalendarContract;

public enum CalendarEventsEntry {

    EVENT_ID(CalendarContract.Events._ID),
    CALENDAR_ID(CalendarContract.Events.CALENDAR_ID),
    TITLE(CalendarContract.Events.TITLE),
    DESCRIPTION(CalendarContract.Events.DESCRIPTION),
    ORGANIZER(CalendarContract.Events.ORGANIZER),
    EVENT_LOCATION(CalendarContract.Events.EVENT_LOCATION),
    CALENDAR_COLOR(CalendarContract.Events.CALENDAR_COLOR);

    private String name;

    CalendarEventsEntry(String name) {
        this.name = name;
    }

    public static String[] getProjection() {
        CalendarEventsEntry[] calendarEventsEntries = values();
        int length = calendarEventsEntries.length;
        String[] projection = new String[length];
        for (int i = 0; i < length; i++) {
            projection[i] = calendarEventsEntries[i].name;
        }
        return projection;
    }
}
