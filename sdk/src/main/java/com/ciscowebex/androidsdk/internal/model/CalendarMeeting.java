package com.ciscowebex.androidsdk.internal.model;

import android.net.Uri;

import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.helloworld.utils.Checker;

public class CalendarMeeting {
    private String id;
    private String seriesId;
    @SerializedName("start")
    private Date startTime;
    private int durationMinutes;
    private String organizer;
    private Uri encryptionKeyUrl;
    @SerializedName("encryptedSubject")
    private String subject;
    private boolean isRecurring;

    private List<CalendarMeetingLink> links = new ArrayList<>();
    @SerializedName("encryptedParticipants")
    private List<CalendarMeetingParticipant> participants = new ArrayList<>();
    private String meetingSensitivity;
    private boolean isDeleted;
    private Date lastModified;
    private boolean isReminderSupported;


    private ReminderSupportType reminderSupportType;

    public enum ReminderSupportType {
        NONE,
        SERVICE,
        CLIENT
    }

    public void decrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        if (!Checker.isEmpty(getSubject())){
            setSubject(CryptoUtils.decryptFromJwe(key, getSubject()));
        }
        if (!getParticipants().isEmpty()){
            List<CalendarMeetingParticipant> decryptedParticipants = new ArrayList<>();
            CalendarMeetingParticipant decryptedParticipant;
            for (CalendarMeetingParticipant participant : getParticipants()) {
                decryptedParticipant = participant.clone();
                if (decryptedParticipant != null) {
                    decryptedParticipant.decrypt(key);
                    decryptedParticipants.add(decryptedParticipant);
                }
            }
            setParticipants(decryptedParticipants);
        }
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setParticipants(List<CalendarMeetingParticipant> participants) {
        this.participants = participants;
    }

    public String getId() {
        return id;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getOrganizer() {
        return organizer;
    }

    public Uri getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public List<CalendarMeetingLink> getLinks() {
        return links;
    }

    public List<CalendarMeetingParticipant> getParticipants() {
        return participants;
    }

    public String getMeetingSensitivity() {
        return meetingSensitivity;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public boolean isReminderSupported() {
        return isReminderSupported;
    }

    public ReminderSupportType getReminderSupportType() {
        return reminderSupportType;
    }
}
