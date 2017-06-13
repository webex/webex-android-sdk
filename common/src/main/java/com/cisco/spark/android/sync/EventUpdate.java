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

public class EventUpdate extends Message {

    private Date startTime;
    private Date endTime;
    private String eventId;
    private User organizer;
    private String location;
    private boolean isRecurring;
    private RecurrenceFrequency recurrenceFrequency;

    // Pertains to the ActivityStream verb "cancel"
    private boolean canceled = false;

    // Pertains to the ActivityStream verb "update"
    private boolean updated = false;

    public EventUpdate(final Activity activity) {
        super(activity);
    }

    public static EventUpdate fromActivity(Activity activity) {

        EventUpdate eventUpdate = null;
        if (activity.getObject().isEvent()) {
            EventObject eventObject = (EventObject) activity.getObject();

            eventUpdate = new EventUpdate(activity);
            eventUpdate.startTime = eventObject.getStartTime();
            eventUpdate.endTime = eventObject.getEndTime();
            eventUpdate.canceled = Verb.cancel.equals(activity.getVerb());
            eventUpdate.updated = Verb.update.equals(activity.getVerb());
            eventUpdate.eventId = eventObject.getId();
            eventUpdate.organizer = eventObject.getOrganizer();
            eventUpdate.location = eventObject.getLocation();
            eventUpdate.isRecurring = eventObject.isRecurring();
            eventUpdate.recurrenceFrequency = eventObject.getRecurrenceFrequency();
        }

        eventUpdate.setProvider(activity.getProvider());

        return eventUpdate;
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

    public boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean getUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
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
