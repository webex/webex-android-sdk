package com.cisco.spark.android.events;

import java.util.Date;

public class MeetingHubDatePickerDateChangedEvent {
    public final Date date;

    public MeetingHubDatePickerDateChangedEvent(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }
}
