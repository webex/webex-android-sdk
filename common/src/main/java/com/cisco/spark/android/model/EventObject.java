package com.cisco.spark.android.model;

import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Specific activity object type that is used to refer to an event.
 *
 * @see ObjectType#event
 */
public class EventObject extends ActivityObject {
    public EventObject() {
        super(ObjectType.event);
    }

    public enum RecurrenceFrequency { HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY }

    /**
     * The date and time that the event begins represented as a String confirming to the "date-time" production in RFC 3339.
     */
    protected Date startTime;

    /**
     * The date and time that the event ends represented as a String conforming to the "date-time" production in [RFC3339].
     */
    protected Date endTime;

    /**
     * The organizer for the event
     */
    protected User organizer;

    /**
     * Event Location
     */
    protected String location;

    /**
     * Flag to indicate if the meeting is a recurring/non-recurring
     */
    protected boolean isRecurring;

    /**
     * Meeting recurrence frequency
     */
    protected RecurrenceFrequency recurrenceFrequency;

    /**
     * A collection object as defined in Section 3.5 of the JSON Activity Streams specification that provides information about
     * entities that attended the event.
     */
    protected ItemCollection<ActivityObject> attendedBy;

    /**
     * A collection object as defined in Section 3.5 of the JSON Activity Streams specification that provides information about
     * entities that intend to attend the event.
     */
    protected ItemCollection<ActivityObject> attending;

    /**
     * A collection object as defined in Section 3.5 of the JSON Activity Streams specification that provides information about
     * entities that have been invited to the event.
     */
    protected ItemCollection<ActivityObject> invited;

    /**
     * A collection object as defined in Section 3.5 of the JSON Activity Streams specification that provides information about
     * entities that possibly may attend the event.
     */
    protected ItemCollection<ActivityObject> maybeAttending;

    /**
     * A collection object as defined in Section 3.5 of the JSON Activity Streams specification that provides information about
     * entities that did not attend the event.
     */
    protected ItemCollection<ActivityObject> notAttendedBy;

    /**
     * A collection object as defined in Section 3.5 of the JSON Activity Streams specification that provides information about
     * entities that do not intend to attend the event.
     */
    protected ItemCollection<ActivityObject> notAttending;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(final Date endTime) {
        this.endTime = endTime;
    }

    public User getOrganizer() {
        return organizer;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public RecurrenceFrequency getRecurrenceFrequency() {
        return recurrenceFrequency;
    }

    public ItemCollection<ActivityObject> getAttendedBy() {
        return attendedBy;
    }

    public void setAttendedBy(final ItemCollection<ActivityObject> attendedBy) {
        this.attendedBy = attendedBy;
    }

    public ItemCollection<ActivityObject> getAttending() {
        return attending;
    }

    public void setAttending(final ItemCollection<ActivityObject> attending) {
        this.attending = attending;
    }

    public ItemCollection<ActivityObject> getInvited() {
        return invited;
    }

    public void setInvited(final ItemCollection<ActivityObject> invited) {
        this.invited = invited;
    }

    public ItemCollection<ActivityObject> getMaybeAttending() {
        return maybeAttending;
    }

    public void setMaybeAttending(final ItemCollection<ActivityObject> maybeAttending) {
        this.maybeAttending = maybeAttending;
    }

    public ItemCollection<ActivityObject> getNotAttendedBy() {
        return notAttendedBy;
    }

    public void setNotAttendedBy(final ItemCollection<ActivityObject> notAttendedBy) {
        this.notAttendedBy = notAttendedBy;
    }

    public ItemCollection<ActivityObject> getNotAttending() {
        return notAttending;
    }

    public void setNotAttending(final ItemCollection<ActivityObject> notAttending) {
        this.notAttending = notAttending;
    }


    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
        }

        if (Strings.notEmpty(getLocation())) {
            setLocation(CryptoUtils.encryptToJwe(key, getLocation()));
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }

        if (Strings.notEmpty(getLocation())) {
            setLocation(CryptoUtils.decryptFromJwe(key, getLocation()));
        }
    }
}
