package com.cisco.spark.android.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.CALENDAR_MEETING_ID;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.CALL_URI;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.CONVERSATION_ID;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.DURATION_MINUTES;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.ENCRYPTION_KEY_URL;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.IS_DELETED;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.IS_ENCRYPTED;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.IS_RECURRING;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.LAST_MODIFIED_TIME;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.LINKS;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.LOCATION;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.MEETING_SENSITIVITY;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.NOTES;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.ORGANIZER_ID;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.PARTICIPANTS;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.SERIES_ID;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.SPACE_MEET_URL;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.SPACE_URI;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.SPACE_URL;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.START_TIME;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.SUBJECT;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.WEBEX_URI;
import static com.cisco.spark.android.sync.ConversationContract.CalendarMeetingInfoEntry.WEBEX_URL;

public class CalendarMeeting {

    public enum MeetingTag {
        SPARK("@spark"),
        WEBEX("@webex");

        private final String tag;

        MeetingTag(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }
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
    private String callURI;

    private List<CalendarMeetingLink> links = new ArrayList<>();
    @SerializedName("encryptedNotes")
    private String notes;
    @SerializedName("encryptedLocation")
    private String location;
    @SerializedName("encryptedParticipants")
    private List<CalendarMeetingParticipant> participants = new ArrayList<>();
    private String meetingSensitivity;

    // eventId maps calendar meetings to local calendar eventId;
    private long eventId;

    private boolean isEncrypted = true;

    // Spark & WebEx PMR meeting field
    private String spaceURI;

    // Spark space meeting fields
    private String conversationId;
    private String spaceURL;
    private String spaceMeetURL;

    // WebEx PMR meeting fields
    private String webexURI;
    private String webexURL;
    private boolean isDeleted;
    private Date lastModified;

    public static CalendarMeeting buildFromContentResolver(ContentResolver contentResolver, Gson gson, String id) {
        CalendarMeeting calendarMeeting = null;

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(Uri.withAppendedPath(CalendarMeetingInfoEntry.CONTENT_URI, id),
                    CalendarMeetingInfoEntry.DEFAULT_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                calendarMeeting = buildFromCursor(cursor, gson);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return calendarMeeting;
    }

    public static CalendarMeeting buildFromCursor(Cursor cursor, Gson gson) {
        CalendarMeeting calendarMeeting = new CalendarMeeting();
        calendarMeeting.id = cursor.getString(CALENDAR_MEETING_ID.ordinal());
        calendarMeeting.seriesId = cursor.getString(SERIES_ID.ordinal());
        calendarMeeting.startTime = new Date(cursor.getLong(START_TIME.ordinal()));
        calendarMeeting.durationMinutes = cursor.getInt(DURATION_MINUTES.ordinal());
        calendarMeeting.organizer = cursor.getString(ORGANIZER_ID.ordinal());
        calendarMeeting.encryptionKeyUrl = UriUtils.parseIfNotNull(cursor.getString(ENCRYPTION_KEY_URL.ordinal()));
        calendarMeeting.subject = cursor.getString(SUBJECT.ordinal());
        calendarMeeting.isRecurring = cursor.getInt(IS_RECURRING.ordinal()) == 1;
        calendarMeeting.callURI = cursor.getString(CALL_URI.ordinal());
        calendarMeeting.notes = cursor.getString(NOTES.ordinal());
        calendarMeeting.location = cursor.getString(LOCATION.ordinal());
        calendarMeeting.links = parseCalendarMeetingLinks(gson, cursor.getString(LINKS.ordinal()));
        calendarMeeting.participants = parseCalendarMeetingParticipants(gson, cursor.getString(PARTICIPANTS.ordinal()));
        calendarMeeting.meetingSensitivity = cursor.getString(MEETING_SENSITIVITY.ordinal());
        calendarMeeting.conversationId = cursor.getString(CONVERSATION_ID.ordinal());
        calendarMeeting.spaceURI = cursor.getString(SPACE_URI.ordinal());
        calendarMeeting.spaceURL = cursor.getString(SPACE_URL.ordinal());
        calendarMeeting.spaceMeetURL = cursor.getString(SPACE_MEET_URL.ordinal());
        calendarMeeting.webexURI = cursor.getString(WEBEX_URI.ordinal());
        calendarMeeting.webexURL = cursor.getString(WEBEX_URL.ordinal());
        calendarMeeting.lastModified = new Date(cursor.getLong(LAST_MODIFIED_TIME.ordinal()));
        calendarMeeting.isDeleted = cursor.getInt(IS_DELETED.ordinal()) == 1;
        calendarMeeting.isEncrypted = cursor.getInt(IS_ENCRYPTED.ordinal()) == 1;
        return calendarMeeting;
    }

    public static List<CalendarMeetingParticipant> parseCalendarMeetingParticipants(Gson gson, String participantJson) {
        if (Strings.isEmpty(participantJson)) {
            return new ArrayList<>();
        }
        Type participantListType = new TypeToken<ArrayList<CalendarMeetingParticipant>>() {
        }.getType();
        return gson.fromJson(participantJson, participantListType);
    }

    private static List<CalendarMeetingLink> parseCalendarMeetingLinks(Gson gson, String linksJson) {
        if (Strings.isEmpty(linksJson)) {
            return new ArrayList<>();
        }
        Type linkListType = new TypeToken<ArrayList<CalendarMeetingLink>>() {
        }.getType();
        return gson.fromJson(linksJson, linkListType);
    }

    public void decrypt(KeyObject key) {
        if (!isEncrypted) {
            return;
        }

        try {
            String plainSubject = CryptoUtils.decryptFromJwe(key, subject);
            String plainNotes = CryptoUtils.decryptFromJwe(key, notes);
            String plainLocation = CryptoUtils.decryptFromJwe(key, location);

            if (!participants.isEmpty()) {
                List<CalendarMeetingParticipant> plainParticipants = new ArrayList<>();
                CalendarMeetingParticipant plainParticipant;
                for (CalendarMeetingParticipant participant : participants) {
                    plainParticipant = participant.clone();
                    if (plainParticipant != null) {
                        participant.decrypt(key);
                        plainParticipants.add(plainParticipant);
                    }
                }
                participants.clear();
                participants.addAll(plainParticipants);
            }

            subject = plainSubject;
            notes = plainNotes;
            location = plainLocation;
            isEncrypted = false;
        } catch (Exception e) {
            Ln.e(e, "Unable to decrypt calendar meeting");
        }
    }

    public ContentProviderOperation.Builder getInsertOperation(Gson gson) {
        ContentValues cv = new ContentValues();
        if (id != null) {
            cv.put(CALENDAR_MEETING_ID.name(), id);
        } else {
            Ln.e("Calendar id field must be non-null.");
            return null;
        }
        cv.put(SERIES_ID.name(), seriesId);
        if (startTime != null) {
            cv.put(START_TIME.name(), startTime.getTime());
        }
        cv.put(DURATION_MINUTES.name(), durationMinutes);
        cv.put(ORGANIZER_ID.name(), organizer);
        if (encryptionKeyUrl != null) {
            cv.put(ENCRYPTION_KEY_URL.name(), encryptionKeyUrl.toString());
        }
        cv.put(SUBJECT.name(), subject);
        cv.put(IS_RECURRING.name(), isRecurring);
        cv.put(CALL_URI.name(), callURI);
        cv.put(NOTES.name(), notes);
        cv.put(LOCATION.name(), location);
        if (!links.isEmpty()) {
            cv.put(LINKS.name(), gson.toJson(links));
        }
        if (!participants.isEmpty()) {
            cv.put(PARTICIPANTS.name(), gson.toJson(participants));
        }
        cv.put(MEETING_SENSITIVITY.name(), meetingSensitivity);
        cv.put(CONVERSATION_ID.name(), conversationId);
        cv.put(SPACE_URI.name(), spaceURI);
        cv.put(SPACE_URL.name(), spaceURL);
        cv.put(SPACE_MEET_URL.name(), spaceMeetURL);
        cv.put(WEBEX_URI.name(), webexURI);
        cv.put(WEBEX_URL.name(), webexURL);

        if (lastModified != null) {
            cv.put(LAST_MODIFIED_TIME.name(), lastModified.getTime());
        }
        cv.put(IS_DELETED.name(), isDeleted);
        cv.put(IS_ENCRYPTED.name(), isEncrypted);

        return ContentProviderOperation.newInsert(CalendarMeetingInfoEntry.CONTENT_URI)
                .withValues(cv);
    }

    public ContentProviderOperation.Builder getDeleteOperation() {
        return ContentProviderOperation.newDelete(
                Uri.withAppendedPath(CalendarMeetingInfoEntry.CONTENT_URI, id));
    }

    public boolean isSparkMeeting() {
        return Strings.notEmpty(conversationId);
    }

    public boolean isWebExMeeting() {
        return Strings.notEmpty(webexURL);
    }

    public boolean isNormalMeeting() {
        return !isSparkMeeting() && !isWebExMeeting();
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

    public String getCallURI() {
        return callURI;
    }

    public List<CalendarMeetingLink> getLinks() {
        return links;
    }

    public String getNotes() {
        return notes;
    }

    public String getLocation() {
        return location;
    }

    public List<CalendarMeetingParticipant> getParticipants() {
        return participants;
    }

    public String getMeetingSensitivity() {
        return meetingSensitivity;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSpaceURI() {
        return spaceURI;
    }

    public String getSpaceURL() {
        return spaceURL;
    }

    public String getSpaceMeetURL() {
        return spaceMeetURL;
    }

    public String getWebexURI() {
        return webexURI;
    }

    public String getWebexURL() {
        return webexURL;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public void setEncryptionKeyUrl(Uri encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public void setCallURI(String callURI) {
        this.callURI = callURI;
    }

    public void setLinks(List<CalendarMeetingLink> links) {
        this.links = links;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setParticipants(List<CalendarMeetingParticipant> participants) {
        this.participants = participants;
    }

}
