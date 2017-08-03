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
    METRIC_FIELD_ANDROID_HARDWARE_BRAND("androidHardwareBrand"),
    METRIC_FIELD_ANDROID_HARDWARE_MANUFACTURER("androidHardwareManufacturer"),
    METRIC_FIELD_ANDROID_HARDWARE_MODEL("androidHardwareModel"),
    METRIC_FIELD_AVAIlABLE_MEMORY("availableMem"),
    METRIC_FIELD_CONTENT_SIZE("content_size_bytes"),
    METRIC_FIELD_DID_UPLOAD_AVATAR("didUploadAvatar"),
    METRIC_FIELD_DID_ACK("didAcknowledge"),
    METRIC_FIELD_DID_EDIT("didEdit"),
    METRIC_FIELD_DURATION_FROM_FIRST_RECORD_TO_END("cdn_duration_from_first_record_to_end"),
    METRIC_FIELD_DURATION_TO_FIRST_RECORD("cdn_duration_to_first_record"),
    METRIC_FIELD_EMAIL_COUNT("emailCount"),
    METRIC_FIELD_ERROR_DESCRIPTION("error_description"),
    METRIC_FIELD_ESTIMATED_TRANSFER_SPEED("estimated_transfer_speed_kbps"),
    METRIC_FIELD_FAILURE_CODE("network.failureCode"),
    METRIC_FIELD_FAILURE_REASON("network.failureReason"),
    METRIC_FIELD_FILE_SIZE("fileSizeInBytes"),
    METRIC_FIELD_FINAL_SECURITY_ALERT_COUNT("numberOfTimesShownFinalAlert"),
    METRIC_FIELD_INVITE_COUNT("inviteCount"),
    METRIC_FIELD_MIME_TYPE("fileMimeType"),
    METRIC_FIELD_MEMORY_THRESHOLD("memoryThreshold"),
    METRIC_FIELD_NETWORK_TYPE("network_type"),
    METRIC_FIELD_PERCIEVED_DURATION("perceivedDurationInMillis"),
    METRIC_FIELD_RETRY_COUNT("retry_count"),
    METRIC_FIELD_SECURITY_ALERT_OPTION("optionChosen"),
    METRIC_FIELD_SECURITY_ALERT_ORG_ID("securityAlertOrgId"),
    METRIC_FIELD_SECURITY_ALERT_SELECT_SETTINGS_COUNT("numberOfTimesSettingsSelected"),
    METRIC_FIELD_SECURITY_ALERT_TIME_BETWEEN("elapsedTimeBetweenAlerts"),
    METRIC_FIELD_SECURITY_ALERT_USER_ID("securityAlertUserId"),
    METRIC_FIELD_SERVER_INTERACTION_DURATION("server_interaction_duration"),
    METRIC_FIELD_SOURCE("source"),
    METRIC_FIELD_STORAGE_TRADCKING_ID("storage_tracking_id"),
    METRIC_FIELD_TEAM_NAME_WORD_COUNT("teamNameWordCount"),
    METRIC_FIELD_TEAM_NAME_CHARACTER_COUNT("teamNameCharacterCount"),
    METRIC_FIELD_TRACKING_ID("tracking_id"),
    METRIC_FIELD_TOTAL_MEMORY("totalMem"),
    METRIC_FIELD_UUID_COUNT("uuidCount"),
    METRIC_FIELD_VM_FREE_MEM("vmFreeMem"),
    METRIC_FIELD_VM_MAX_MEM("vmMaxMem"),
    METRIC_FIELD_VM_TOTAL_MEM("vmTotalMem"),
    METRIC_FIELD_VM_USED_MEM("vmUsedMem"),
    METRIC_FIELD_WAS_SUCCESSFUL("wasSuccessful"),
    METRIC_FIELD_ANDROID_ISKEYGUARDSECURE("androidSecureKeyguard"),
    METRIC_FIELD_ANDROID_ISDEVICESECURE("androidSecureDevice");


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
