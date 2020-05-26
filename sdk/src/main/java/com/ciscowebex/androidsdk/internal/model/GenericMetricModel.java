/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import android.support.annotation.Nullable;
import com.cisco.wx2.diagnostic_events.ClientEvent;
import com.cisco.wx2.diagnostic_events.Event;
import com.ciscowebex.androidsdk.internal.ErrorDetail;
import com.ciscowebex.androidsdk.internal.ServiceReqeust;
import com.ciscowebex.androidsdk.utils.NetworkUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.*;

public class GenericMetricModel {

    public enum Tag {
        METRIC_TAG_ACTION("action"),
        METRIC_TAG_ACTION_TYPE("action_type"),
        METRIC_TAG_ACTIVITY_TYPE_TAG("activityType"),
        METRIC_TAG_CLIENT_SECURITY_POLICY("clientSecurityPolicy"),
        METRIC_TAG_CONTENT_TYPE("content_type"),
        METRIC_TAG_CONVERSATION_TYPE("conv_type"),
        METRIC_TAG_DATA_TYPE("data_type"),
        METRIC_TAG_DIR_SYNC("orgHasDirectorySync"),
        METRIC_TAG_ECM_PROVIDER("ecm_provider"),
        METRIC_TAG_ERROR_CODE("error_code"),
        METRIC_TAG_FILE_SHARE_CONTROLS("mobileFileShareControls"),
        METRIC_TAG_GRAPH_ENDPOINT("graphEndpoint"),
        METRIC_TAG_HAS_ANDROID_LOCKSCREEN("hasAndroidLockscreen"),
        METRIC_TAG_HAS_PASSWORD("hasPassword"),
        METRIC_TAG_HTTP_STATUS("http_status"),
        METRIC_TAG_INTERSTITIAL_SCREEN_AUDIO_SELECTION("interstitialScreenAudioSelection"),
        METRIC_TAG_INTERSTITIAL_SCREEN_HAS_VIDEO("interstitialScreenHasVideo"),
        METRIC_TAG_INTERSTITIAL_SCREEN_MEETING_TYPE("interstitialScreenMeetingType"),
        METRIC_TAG_IS_BLOCK_EXTERNAL_COMMUNICATION_CREATOR("blockExternalCommunicationCreator"),
        METRIC_TAG_IS_BLOCK_EXTERNAL_COMMUNICATION_PARTICIPANT("blockExternalCommunicationParticipant"),
        METRIC_TAG_IS_ROOTED("isRooted"),
        METRIC_TAG_LOW_MEMORY("lowMem"),
        METRIC_TAG_MEMORY_WARNING_LEVEL("memoryWarningLevel"),
        METRIC_TAG_REMOTE_PRESENCE("remotePresence"),
        METRIC_TAG_PROXY_AUTO_DISCOVERY("proxy_auto_discovery_status"),
        METRIC_TAG_PROXY_AUTHENTICATION_USED("proxy_authentication_used"),
        METRIC_TAG_PROXY_DISCOVERY_TYPE("proxy_discovery_type"),
        METRIC_TAG_SAFETYNET_ATTESTATION_CHECK("safetyNet_attestation_check_result"),
        METRIC_TAG_SELF_PRESENCE("selfPresence"),
        METRIC_TAG_SSO("isSSOEnabled"),
        METRIC_TAG_SUCCESS_TAG("success"),
        METRIC_TAG_TEST_USER("testuser"),
        METRIC_TAG_USER_CREATED("wasUserCreated"),
        METRIC_TAG_USER_SIGNED_UP("isSignUp"),
        METRIC_TAG_VERIFICATION_EMAIL_TRIGGERED("wasVerificationEmailTriggered"),
        METRIC_TAG_WAS_SUCCESSFUL("network.wasSuccessful"),
        METRIC_TAG_WHITEBOARD_ANNOTATIONS_SHARE_CONTROLS("whiteboardShareControls"),
        METRIC_TAG_CLIENT_MESSAGING_GIPHY("clientMessagingGiphy"),
        METRIC_TAG_CLIENT_MESSAGING_LINK_PREVIEW("clientMessagingLinkPreview");

        private String tagName;

        Tag(String tagName) {
            this.tagName = tagName;
        }

        public String getTagName() {
            return this.tagName;
        }

        public String toString() {
            return this.tagName;
        }
    }

    public interface TagValue {
        String getValue();
    }

    public enum ConversationTypeMetricTagValue implements TagValue {
        CONVERSATION_TYPE_GROUP("Group"),
        CONVERSATION_TYPE_ONE_TO_ONE("One2One"),
        CONVERSATION_TYPE_TEAM_SPACE("TeamSpace"),
        CONVERSATION_TYPE_TEAM("Team");

        private String value;

        ConversationTypeMetricTagValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public enum DataTypeMetricTagValue implements TagValue {
        DATA_TYPE_FILE("file"),
        DATA_TYPE_THUMBNAIL("thumbnail"),
        DATA_TYPE_AVATAR("avatar"),
        DATA_TYPE_CONVERSATION_AVATAR("conversation_avatar"),
        DATA_TYPE_PREVIEW("preview"),
        DATA_TYPE_ANNOTATION("annotation"),
        DATA_TYPE_MSECM_THUMBNAIL("msecm_thumbnail"),
        DATA_TYPE_UNKNOWN("unknown");

        private String value;

        DataTypeMetricTagValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum InterstitialScreenAudioSelectionType implements TagValue {
        INTERSTITIAL_SCREEN_AUDIO_PAIRED_SELECTIONN_AUDIO_PAIRED("paired"),
        INTERSTITIAL_SCREEN_AUDIO_PHONE_SELECTION("phone"),
        INTERSTITIAL_SCREEN_AUDIO_PSTN_CALL_IN_SELECTION("pstn_call_in"),
        INTERSTITIAL_SCREEN_AUDIO_PSTN_CALL_ME_SELECTION("pstn_call_me"),
        INTERSTITIAL_SCREEN_AUDIO_NONE_SELECTION("none");

        private String value;

        InterstitialScreenAudioSelectionType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public enum InterstitialScreenMeetingType implements TagValue {
        INTERSTITIAL_SCREEN_AD_HOC_MEETING_TYPE("ad_hoc_meeting"),
        INTERSTITIAL_SCREEN_AD_HOC_ONE_ONE_ONE_MEETING_TYPE("ad_hoc_one_on_one"),
        INTERSTITIAL_SCREEN_OTHER_MEETING_TYPE("other"),
        INTERSTITIAL_SCREEN_PMR_MEETING_TYPE("pmr"),
        INTERSTITIAL_SCREEN_SCHEDULED_MEETING_TYPE("scheduled_meeting"),
        INTERSTITIAL_SCREEN_SCHEDULED_ONE_ON_ONE_TYPE("scheduled_one_on_one");

        private String value;

        InterstitialScreenMeetingType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public enum SpaceActionType implements TagValue {
        ADD_PARTICIPANT("AddParticipant"),
        ASSIGN_MODERATOR("AssignModerator"),
        FAVORITE("Favorite"),
        LEAVE("Leave"),
        MODERATE("Moderate"),
        MUTE("Mute"),
        REMOVE_PARTICIPANT("RemoveParticipant"),
        UNASSIGN_MODERATOR("UnassignModerator"),
        UNFAVORITE("Unfavorite"),
        UNMODERATE("Unmoderate"),
        UNMUTE("Unmute");

        private String value;

        SpaceActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_ACTION_TYPE
     */
    public enum TeamSpaceActionType implements TagValue {
        ARCHIVE_TEAM("ArchiveTeam"),
        ARCHIVE_TEAM_ROOM("ArchiveTeamRoom"),
        JOIN_TEAM_ROOM("JoinTeamRoom"),
        LEAVE_TEAM("LeaveTeam"),
        MOVE("Move"),
        REMOVE("Remove"),
        SET_TEAM_INFO("SetTeamInfo"),
        UNARCHIVE_TEAM("UnarchiveTeam"),
        UNARCHIVE_TEAM_ROOM("UnarchiveTeamRoom");

        private String value;

        TeamSpaceActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_ACTION_TYPE
     */
    public enum MessageActionType implements TagValue {
        DELETE("Delete"),
        CANCEL("Cancel");

        private String value;

        MessageActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_ACTION_TYPE
     */
    public enum MercuryConnectionActionType implements TagValue {
        CONNECTED("connected"),
        DISCONNECTED("disconnected"),
        CONNECT_FAILED("connect_failed");

        private String value;

        MercuryConnectionActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_PROXY_DISCOVERY_TYPE
     */
    public enum ProxyAuthenticationType implements TagValue {
        PROXY_AUTHENTICATION_BASIC("basic"),
        PROXY_AUTHENATINCATION_DIGEST("digest"),
        PROXY_AUTHENTICATION_KERBEROS("kerberos"),
        PROXY_AUTHENTICATION_NEGOTIATE("negotiate"),
        PROXY_AUTHENTICATION_NONE("none"),
        PROXY_AUTHENTICATION_NTLM("ntlm"),
        PROXY_AUTHENTICATION_UNKNOWN("unknown");

        private String value;

        ProxyAuthenticationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_ACTION
     */
    public enum MetricActionType implements TagValue {
        METRIC_ACTION_LOGIN("login"),
        METRIC_ACTION_LOGOUT("logout");

        private String value;

        MetricActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.Metric_TAG_ECM_PROVIDER
     */
    public enum EcmProviderType implements TagValue {
        ECM_PROVIDER_ONEDRIVE_BUSINESS("MsOneDriveBuisness"),
        ECM_PROVIDER_ONEDRIVE_PERSONAL("MsOneDrivePersonal"),
        ECM_PROVIDER_SHAREPOINT("MsSharePoint"),
        ECM_PROVIDER_UNKNOWN("Unknown");

        private String value;

        EcmProviderType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        public static EcmProviderType fromDriveType(String driveType) {
            if ("personal".equals(driveType)) {
                return ECM_PROVIDER_ONEDRIVE_PERSONAL;
            } else if ("business".equals(driveType)) {
                return ECM_PROVIDER_ONEDRIVE_BUSINESS;
            } else if ("documentLibrary".equals(driveType)) {
                return ECM_PROVIDER_SHAREPOINT;
            } else {
                return ECM_PROVIDER_UNKNOWN;
            }
        }
    }

    /**
     * Use these values with ClientMetricTag.Metric_TAG_GRAPH_ENDPOINT
     */
    public enum GraphEndpointType implements TagValue {
        GRAPH_ENDPOINT_SELF_INFO("getSelfInfo"),
        GRAPH_ENDPOINT_TYPE_DRIVE_INFO("getSelfDriveInfo");

        private String value;

        GraphEndpointType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_CONTENT_TYPE
     */
    public enum ContentType implements TagValue {
        CONTENT_TYPE_FILE("File"),
        CONTENT_TYPE_IMAGE("Image"),
        CONTENT_TYPE_ECM_FILE("EcmFile"),
        CONTENT_TYPE_WHITEBOARD("Whiteboard"),
        CONTENT_TYPE_GIPHY("Giphy");

        private String value;

        ContentType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public enum Field {
        METRIC_FIELD_ACTOR_ROLE("actor_role"),
        METRIC_FIELD_ADDED_EMAILS("addedMoreEmails"),
        METRIC_FIELD_ALLOWED_APP_OPEN("allowedAppOpen"),
        METRIC_FIELD_ALLOWED_CALENDAR("allowedCalendar"),
        METRIC_FIELD_ALLOWED_CAMERA("allowedCamera"),
        METRIC_FIELD_CODE("code"),
        METRIC_FIELD_ALLOWED_CONTACTS("allowedContacts"),
        METRIC_FIELD_ALLOWED_MIC("allowedMic"),
        METRIC_FIELD_ALLOWED_NOTIFICATIONS("allowedNotification"),
        METRIC_FIELD_ANDROID_HARDWARE_BRAND("androidHardwareBrand"),
        METRIC_FIELD_ANDROID_HARDWARE_MANUFACTURER("androidHardwareManufacturer"),
        METRIC_FIELD_ANDROID_HARDWARE_MODEL("androidHardwareModel"),
        METRIC_FIELD_ANDROID_ISDEVICESECURE("androidSecureDevice"),
        METRIC_FIELD_ANDROID_ISKEYGUARDSECURE("androidSecureKeyguard"),
        METRIC_FIELD_ATTACHMENT_COUNT("attachment_count"),
        METRIC_FIELD_AVAIlABLE_MEMORY("availableMem"),
        METRIC_FIELD_BYPASS_PROXY_CONFIGURED("bypass_proxy_configured"),
        METRIC_FIELD_CONTENT_SIZE("content_size_bytes"),
        METRIC_FIELD_CREATED("created"),
        METRIC_FIELD_CROSS_LAUNCHED("cross_launched"),
        METRIC_FIELD_DID_UPLOAD_AVATAR("didUploadAvatar"),
        METRIC_FIELD_DID_ACK("didAcknowledge"),
        METRIC_FIELD_DID_EDIT("didEdit"),
        METRIC_FIELD_DISCONNECTION_CODE("disconnection_code"),
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
        METRIC_FIELD_IS_FLAGGED("is_flagged"),
        METRIC_FIELD_IS_MODERATED_SPACE("is_moderated_space"),
        METRIC_FIELD_MAIN_CONNECTION("main_connection"),
        METRIC_FIELD_MESSAGE("message"),
        METRIC_FIELD_MESSAGE_TYPE("message_type"),
        METRIC_FIELD_MIME_TYPE("fileMimeType"),
        METRIC_FIELD_MERCURY_HOST("mercury_host"),
        METRIC_FIELD_MEMORY_THRESHOLD("memoryThreshold"),
        METRIC_FIELD_NETWORK_TYPE("network_type"),
        METRIC_FIELD_PERCIEVED_DURATION("perceivedDurationInMillis"),
        METRIC_FIELD_PRIORITY("priority"),
        METRIC_FIELD_PROXY_COUNT("proxy_count"),
        METRIC_FIELD_PROXY_BYPASS("proxy_bypassed"),
        METRIC_FIELD_RETRY_COUNT("retry_count"),
        METRIC_FIELD_SECURITY_ALERT_OPTION("optionChosen"),
        METRIC_FIELD_SECURITY_ALERT_ORG_ID("securityAlertOrgId"),
        METRIC_FIELD_SECURITY_ALERT_SELECT_SETTINGS_COUNT("numberOfTimesSettingsSelected"),
        METRIC_FIELD_SECURITY_ALERT_TIME_BETWEEN("elapsedTimeBetweenAlerts"),
        METRIC_FIELD_SECURITY_ALERT_USER_ID("securityAlertUserId"),
        METRIC_FIELD_SENDER_IS_SELF("sender_is_self"),
        METRIC_FIELD_SENDER_TYPE("sender_type"),
        METRIC_FIELD_SERVER_INTERACTION_DURATION("server_interaction_duration"),
        METRIC_FIELD_SOURCE("source"),
        METRIC_FIELD_SPACE_ID("space_id"),
        METRIC_FIELD_STORAGE_TRADCKING_ID("storage_tracking_id"),
        METRIC_FIELD_TEAM_ID("team_id"),
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
        METRIC_FIELD_WHITEBOARD_COUNT("whiteboard_count"),
        METRIC_FIELD_GIPHY_COUNT("giphy_count"),
        METRIC_FIELD_GIPHY_CATEGORY_COUNT("giphy_from_category_count"),
        METRIC_FIELD_GIPHY_SEARCH_COUNT("giphy_from_search_count");

        private String fieldName;

        Field(String name) {
            this.fieldName = name;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String toString() {
            return fieldName;
        }
    }

    public interface FieldValue {
        String getValue();
    }

    /**
     * Use these values with ClientMetricField.METRIC_FIELD_ACTOR_ROLE
     */
    public enum ActorRole implements FieldValue {
        MODERATOR("Moderator"),
        MEMBER("Member"),
        GUEST("Guest");

        private String value;

        ActorRole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Use these values with ClientMetricField.METRIC_FIELD_SENDER_TYPE
     */
    public enum SenderType implements FieldValue {
        USER("User"),
        BOT("Bot"),
        ROBOT("Robot"),
        PROVIDER("Provider"),
        UNKNOWN("Unknown");

        private String value;

        SenderType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // This should be used for cases where the send failed, but is not due to an infrastructure or client
    // issue. Examples of this would be network issues or bad user-entered parameters
    public static final String USER_FAILURE_STATUS = "user_false";
    private static final int MAX_METRICS_CACHE = 100;
    private static final ArrayList<GenericMetricModel> metricsCache = new ArrayList<>(MAX_METRICS_CACHE);
    private static final Object syncObj = new Object();
    private static final String BEHAVIORAL_TYPE = "behavioral";
    private static final String OPERATIONAL_TYPE = "operational";
    private static final String DIAGNOSTIC_TYPE = "diagnostic-event";
    private static final String NETWORK_FAILURE_DESCRIPTION = "Device has no network";

    private String metricName;
    private Long timestamp;
    private Map<String, Object> tags;
    private Map<String, Object> fields;
    protected Set<String> type;
    private Event eventPayload;

    public static GenericMetricModel buildBehavioralMetric(String metricName) {
        GenericMetricModel metric = new GenericMetricModel(metricName).withType(BEHAVIORAL_TYPE);
        cacheMetric(metric);
        return metric;
    }

    public static GenericMetricModel buildOperationalMetric(String metricName) {
        GenericMetricModel metric = new GenericMetricModel(metricName).withType(OPERATIONAL_TYPE).withTimestamp(DateTime.now(DateTimeZone.UTC));
        cacheMetric(metric);
        return metric;
    }

    public static GenericMetricModel buildBehavioralAndOperationalMetric(String metricName) {
        GenericMetricModel metric = new GenericMetricModel(metricName).withType(OPERATIONAL_TYPE).withType(BEHAVIORAL_TYPE);
        cacheMetric(metric);
        return metric;
    }

    public static GenericMetricModel buildDiagnosticMetric(Event eventPayload) {
        return new GenericMetricModel(eventPayload).withType(DIAGNOSTIC_TYPE);
    }

    public static GenericMetricModel[] getMetricsCache() {
        synchronized (syncObj) {
            return Arrays.copyOf(metricsCache.toArray(), metricsCache.size(), GenericMetricModel[].class);
        }
    }

    private static void cacheMetric(GenericMetricModel metric) {
        synchronized (syncObj) {
            metricsCache.add(metric);

            if (metricsCache.size() > MAX_METRICS_CACHE) {
                metricsCache.remove(0);
            }
        }
    }

    private GenericMetricModel(String metricName) {
        this.metricName = metricName;
        tags = new HashMap<>();
        fields = new HashMap<>();
        type = new HashSet<>();
        eventPayload = null;
    }

    private GenericMetricModel(Event eventPayload) {
        this.eventPayload = eventPayload;
        type = new HashSet<>();
        metricName = null;
        tags = null;
        fields = null;
    }

    public boolean isDiagnosticEvent() {
        return type.contains(DIAGNOSTIC_TYPE);
    }

    public void addTag(Tag tag, Object tagValue) {
        this.tags.put(tag.getTagName(), tagValue != null ? tagValue.toString() : null);
    }

    public void addTag(Tag tag, String tagValue) {
        this.tags.put(tag.getTagName(), tagValue);
    }

    public void addTag(Tag tag, boolean tagValue) {
        this.tags.put(tag.getTagName(), Boolean.toString(tagValue));
    }

    public void addTags(Map<String, Object> tags) {
        this.tags.putAll(tags);
    }

    public void addTag(Tag tag, TagValue value) {
        if (value != null) {
            this.tags.put(tag.getTagName(), value.getValue());
        }
    }

    public void addTagSet(Tag tag, EnumSet<? extends TagValue> tagSet) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        if (tagSet == null) {
            return;
        }
        for (TagValue value : tagSet) {
            if (!first) {
                builder.append("|");
            } else {
                first = false;
            }
            builder.append(value.getValue());
        }
        addTag(tag, builder.toString());
    }

    public void addField(Field field, Object fieldValue) {
        this.fields.put(field.getFieldName(), fieldValue);
    }

    public void addField(Field field, String fieldValue) {
        this.fields.put(field.getFieldName(), fieldValue);
    }

    public void addField(Field field, boolean fieldValue) {
        this.fields.put(field.getFieldName(), Boolean.toString(fieldValue));
    }

    public void addField(Field field, FieldValue value) {
        if (value != null) {
            this.fields.put(field.getFieldName(), value.getValue());
        }
    }

    public void addFields(Map<String, Object> fields) {
        this.fields.putAll(fields);
    }

    public Object getTag(Tag metricTag) {
        return getTag(metricTag.getTagName());
    }

    public Object getTag(String metricTag) {
        return this.tags.get(metricTag);
    }

    public int getTagSize() {
        return this.tags.size();
    }

    public Set<String> getTagKeySet() {
        return this.tags.keySet();
    }

    public Object getField(Field metricField) {
        return getField(metricField.getFieldName());
    }

    public Object getField(String fieldName) {
        return this.fields.get(fieldName);
    }

    public int getFieldSize() {
        return this.fields.size();
    }

    public Set<String> getFieldKeySet() {
        return this.fields.keySet();
    }

    public String getMetricName() {
        return metricName;
    }

    public String toString() {
        if (metricName != null) {
            return "GenericMetric metricName " + metricName;
        } else if (eventPayload != null) {
            if (eventPayload.getEvent() instanceof ClientEvent) {
                return "GenericMetric event " + ((ClientEvent) eventPayload.getEvent()).getName().toString();
            } else {
                return "GenericMetric event " + eventPayload.getEventId().toString();
            }
        } else {
            return "GenericMetric";
        }
    }

    /**
     * Use this to add network response information for all types of metrics. This method will assign
     * the correct properties on the metric based on the metric type Set and the various response fields.
     *
     * @param response The Response object from which the metric information will be populated. If this is null,
     *                 it signals that there was no network available
     * @param errorDetail The ErrorDetail object that is parsed from the associated response. This processing is
     *                    not done within this method because that parsing consumes the information, so anyone
     *                    creating the metric must parse it themselves using the methods in ResponseUtils
     */
    public void addNetworkStatus(@Nullable okhttp3.Response response, @Nullable ErrorDetail errorDetail) {
        if (this.type.contains(OPERATIONAL_TYPE)) {
            if (response == null) {
                addTag(Tag.METRIC_TAG_SUCCESS_TAG, USER_FAILURE_STATUS);
                addField(Field.METRIC_FIELD_ERROR_DESCRIPTION, NETWORK_FAILURE_DESCRIPTION);
            } else  {
                addTag(Tag.METRIC_TAG_SUCCESS_TAG, NetworkUtils.isResponseSuccessful(response));
                addTag(Tag.METRIC_TAG_HTTP_STATUS, response.code());
                addField(Field.METRIC_FIELD_TRACKING_ID, response.headers().get(ServiceReqeust.HEADER_TRACKING_ID));
                if (!NetworkUtils.isResponseSuccessful(response) && errorDetail != null) {
                    addTag(Tag.METRIC_TAG_ERROR_CODE, errorDetail.getErrorCode());
                    addField(Field.METRIC_FIELD_ERROR_DESCRIPTION, errorDetail.getMessage());
                }
            }
        }

        if (this.type.contains(BEHAVIORAL_TYPE)) {
            if (response == null) {
                addTag(Tag.METRIC_TAG_WAS_SUCCESSFUL, USER_FAILURE_STATUS);
                addField(Field.METRIC_FIELD_FAILURE_REASON, NETWORK_FAILURE_DESCRIPTION);
            } else {
                addTag(Tag.METRIC_TAG_WAS_SUCCESSFUL, NetworkUtils.isResponseSuccessful(response));

                if (!NetworkUtils.isResponseSuccessful(response)) {
                    addField(Field.METRIC_FIELD_FAILURE_CODE, response.code());
                    addField(Field.METRIC_FIELD_FAILURE_REASON, (errorDetail != null) ? errorDetail.getErrorCode() + errorDetail.getMessage() : response.message());
                }
            }
        }
    }

    private GenericMetricModel withType(String typeToAdd) {
        type.add(typeToAdd);
        return this;
    }

    public GenericMetricModel withTimestamp(DateTime timestamp) {
        this.timestamp = timestamp.getMillis();
        return this;
    }

    public void onInstantBeforeSend() {
        if (eventPayload != null) {
            eventPayload.getOriginTime().setSent(Instant.now());
        }
    }
}
