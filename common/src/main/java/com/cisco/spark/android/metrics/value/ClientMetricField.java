package com.cisco.spark.android.metrics.value;

/*
 * NOTE: Please add new fields in alphabetical order!
 * Doing this will help reduce merge conflicts.
 */
public enum ClientMetricField {
    METRIC_FIELD_ADDED_EMAILS("addedMoreEmails"),
    METRIC_FIELD_ALLOWED_APP_OPEN("allowedAppOpen"),
    METRIC_FIELD_ALLOWED_CALENDAR("allowedCalendar"),
    METRIC_FIELD_ALLOWED_CAMERA("allowedCamera"),
    METRIC_FIELD_ALLOWED_CONTACTS("allowedContacts"),
    METRIC_FIELD_ALLOWED_MIC("allowedMic"),
    METRIC_FIELD_ALLOWED_NOTIFICATIONS("allowedNotification"),
    METRIC_FIELD_CLEARED_EMAIL_COUNT("clearedEmailCount"),
    METRIC_FIELD_DID_UPLOAD_AVATAR("didUploadAvatar"),
    METRIC_FIELD_DID_ACK("didAcknowledge"),
    METRIC_FIELD_DID_EDIT("didEdit"),
    METRIC_FIELD_FAILURE_CODE("failureCode"),
    METRIC_FIELD_FAILURE_REASON("failureReason"),
    METRIC_FIELD_FILE_SIZE("fileSizeInBytes"),
    METRIC_FIELD_INVITE_COUNT("inviteCount"),
    METRIC_FIELD_MIME_TYPE("fileMimeType"),
    METRIC_FIELD_PERCIEVED_DURATION("perceivedDurationInMillis"),
    METRIC_FIELD_SOURCE("source"),
    METRIC_FIELD_TEAM_NAME_WORD_COUNT("teamNameWordCount"),
    METRIC_FIELD_TEAM_NAME_CHARACTER_COUNT("teamNameCharacterCount"),
    METRIC_FIELD_WAS_SUCCESSFUL("wasSuccessful");

    private String fieldName;

    ClientMetricField(String name) {
        this.fieldName = name;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String toString() {
        return fieldName;
    }
}
