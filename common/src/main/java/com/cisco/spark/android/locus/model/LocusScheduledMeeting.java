package com.cisco.spark.android.locus.model;

import android.net.Uri;

import java.util.Date;
import java.util.UUID;

public class LocusScheduledMeeting {
    // Fields set by gson
    private UUID organizer;
    private String resourceType;
    private Uri resourceUrl;
    private Date startTime;
    private int durationMinutes;
    private String meetingSubject;
    private Uri decryptionKeyUrl;
    /**
     * equivalent to {@link com.cisco.spark.android.model.CalendarMeeting#id}
     */
    private String meetingId;

    /**
     * equivalent to {@link com.cisco.spark.android.model.CalendarMeeting#seriesId}
     */
    private String icalUid;

    public UUID getOrganizer() {
        return organizer;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Uri getResourceUrl() {
        return resourceUrl;
    }

    public Date getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getMeetingSubject() {
        return meetingSubject;
    }

    public Uri getDecryptionKeyUrl() {
        return decryptionKeyUrl;
    }

    public String getIcalUid() {
        return icalUid;
    }

    public String getMeetingId() {
        return meetingId;
    }

}

