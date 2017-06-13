package com.cisco.spark.android.meetings;

import android.provider.CalendarContract;


public enum CalendarInstancesEntry {
    _ID(CalendarContract.Instances._ID),
    EVENT_ID(CalendarContract.Instances.EVENT_ID),
    CALENDAR_ID(CalendarContract.Instances.CALENDAR_ID),
    BEGIN(CalendarContract.Instances.BEGIN),
    END(CalendarContract.Instances.END),
    TITLE(CalendarContract.Instances.TITLE),
    DESCRIPTION(CalendarContract.Instances.DESCRIPTION),
    ORGANIZER(CalendarContract.Instances.ORGANIZER),
    EVENT_LOCATION(CalendarContract.Instances.EVENT_LOCATION),
    STATUS(CalendarContract.Instances.STATUS),
    SELF_ATTENDEE_STATUS(CalendarContract.Instances.SELF_ATTENDEE_STATUS);

    private String name;

    CalendarInstancesEntry(String name) {
        this.name = name;
    }

    public static String[] getProjection() {
        CalendarInstancesEntry[] calendarInstancesEntries = values();
        int length = calendarInstancesEntries.length;
        String[] projection = new String[length];
        for (int i = 0; i < length; i++) {
            projection[i] = calendarInstancesEntries[i].name;
        }
        return projection;
    }
}

