package com.cisco.spark.android.metrics.value;

/*
 * NOTE: Please add new fields in alphabetical order!
 * Doing this will help reduce merge conflicts.
 */
public enum ClientMetricTag {
    METRIC_TAG_ACTIVITY_TYPE_TAG("activityType"),
    METRIC_TAG_CLIENT_SECURITY_POLICY("clientSecurityPolicy"),
    METRIC_TAG_CONVERSATION_TYPE("convType"),
    METRIC_TAG_DATA_TYPE("data_type"),
    METRIC_TAG_DIR_SYNC("orgHasDirectorySync"),
    METRIC_TAG_ERROR_CODE("error_code"),
    METRIC_TAG_HAS_ANDROID_LOCKSCREEN("hasAndroidLockscreen"),
    METRIC_TAG_HAS_PASSWORD("hasPassword"),
    METRIC_TAG_HTTP_STATUS("http_status"),
    METRIC_TAG_IS_ROOTED("isRooted"),
    METRIC_TAG_LOW_MEMORY("lowMem"),
    METRIC_TAG_MEMORY_WARNING_LEVEL("memoryWarningLevel"),
    METRIC_TAG_REMOTE_PRESENCE("remotePresence"),
    METRIC_TAG_SELF_PRESENCE("selfPresence"),
    METRIC_TAG_SSO("isSSOEnabled"),
    METRIC_TAG_SUCCESS_TAG("success"),
    METRIC_TAG_USER_CREATED("wasUserCreated"),
    METRIC_TAG_VERIFICATION_EMAIL_TRIGGERED("wasVerificationEmailTriggered"),
    METRIC_TAG_WAS_SUCCESSFUL("network.wasSuccessful");

    private String tagName;

    ClientMetricTag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return this.tagName;
    }

    public String toString() {
        return this.tagName;
    }
}
