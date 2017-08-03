package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.EventObject;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import static com.cisco.spark.android.model.EventObject.RecurrenceFrequency;

public class SparkMeetingWidget extends Message {

    private Date startTime;
    private Date endTime;
    private String eventId;
    private User organizer;
    private String location;
    private boolean isRecurring;
    private RecurrenceFrequency recurrenceFrequency;
    public enum StatusType { SCHEDULE, UPDATE, DELETE };
    private StatusType status;

    public SparkMeetingWidget(final Activity activity) {
        super(activity);
    }

    public static SparkMeetingWidget fromActivity(Activity activity) {

        SparkMeetingWidget sparkMeetingWidget = null;
        if (activity.getObject().isEvent()) {
            EventObject eventObject = (EventObject) activity.getObject();

            sparkMeetingWidget = new SparkMeetingWidget(activity);
            sparkMeetingWidget.startTime = eventObject.getStartTime();
            sparkMeetingWidget.endTime = eventObject.getEndTime();
            switch (activity.getVerb()) {
                case Verb.schedule: sparkMeetingWidget.status = StatusType.SCHEDULE;
                    break;
                case Verb.update: sparkMeetingWidget.status = StatusType.UPDATE;
                    break;
                case Verb.delete: sparkMeetingWidget.status = StatusType.DELETE;
                    break;
            }
            sparkMeetingWidget.eventId = eventObject.getId();
            sparkMeetingWidget.organizer = eventObject.getOrganizer();
            sparkMeetingWidget.location = eventObject.getLocation();
            sparkMeetingWidget.isRecurring = eventObject.isRecurring();
            sparkMeetingWidget.recurrenceFrequency = eventObject.getRecurrenceFrequency();
        }

        sparkMeetingWidget.setProvider(activity.getProvider());

        return sparkMeetingWidget;
    }

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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public User getOrganizer() {
        return organizer;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public RecurrenceFrequency getRecurrenceFrequency() {
        return recurrenceFrequency;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(getLocation())) {
            setLocation(CryptoUtils.encryptToJwe(key, getLocation()));
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (Strings.notEmpty(getLocation())) {
            setLocation(CryptoUtils.decryptFromJwe(key, getLocation()));
        }
    }
}
