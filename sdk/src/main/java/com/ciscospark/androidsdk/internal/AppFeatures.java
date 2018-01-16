/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.internal;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.util.UIUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import static com.cisco.spark.android.wdm.Features.*;

public class AppFeatures implements CoreFeatures {


    private final DeviceRegistration deviceRegistration;

    public AppFeatures(DeviceRegistration deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    /**************
     * APP FEATURES
     *************/

    public boolean isFilesActivityEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_FILES_SPACEBALL, false);
    }

    public boolean isAnnotationFileEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_BOARD_ANNOTATION_FILE, false);
    }

    public boolean isAnnotationPresentationEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_BOARD_ANNOTATION_PRESENTATION, false);
    }

    public boolean isAnnotationAccelerateEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_ANNOTATION_ACCELERATE, false);
    }

    public boolean isAlwaysOnProximityEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ALWAYS_ON_PROXIMITY, false);
    }

    public boolean isFiltersUnreadIndicatorEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_UNREAD_FILTER_INDICATOR, false);
    }

    public boolean isFilterSwipingEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(FILTER_SWIPE, false);
    }

    public boolean hasTabsNavigation() {
        return false;
    }

    public boolean isNewProximityAndBindingCoachmarkEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_NEW_PROXIMITY_AND_BINDING_COACHMARKS_SPARKANS, ANDROID_NEW_PROXIMITY_AND_BINDING_COACHMARKS);
    }

    public boolean isMeetingHubEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_MEETING_HUB, false);
    }

    public boolean isHidePairedDeviceEnable() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_HIDE_PAIRED_DEVICE, false);
    }

    public boolean isShowRosterEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_INCALL_ROSTER_RELEASE, ANDROID_INCALL_ROSTER);
    }

    public boolean isSparkRoomURLEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_SPARK_ROOM_URL, false);
    }

    public boolean isMeetUrlOneOnOneEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(MEET_URL_ONE_ON_ONE, false);
    }

    public boolean isRoomDeviceVolumeControlEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(LYRA_VOLUME_CONTROL_RELEASE, LYRA_VOLUME_CONTROL);
    }

    public boolean isRecordMeetingsControlEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(RECORD_MEETINGS_CONTROL_ENABLED, false);
    }

    @Override
    public boolean isCalendarSyncEnabled() {
        boolean hasSyncEnabled = deviceRegistration.getFeatures().isDeveloperFeatureEnabled(CALENDAR_SYNC_ENABLED, true);
        boolean hasAnyCalEntitlement = deviceRegistration.getFeatures().isEntitlementFeatureEnabled(HYBRID_CALENDAR_ENTITLEMENT, false)
                || deviceRegistration.getFeatures().isEntitlementFeatureEnabled(HYBRID_GCALENDAR_ENTITLEMENT, false);
        Ln.i("isCalendarSyncEnabled ? %b syncEnabled ? %b hasEntitlements ? %b", hasSyncEnabled && hasAnyCalEntitlement, hasSyncEnabled, hasAnyCalEntitlement);
        return hasSyncEnabled && hasAnyCalEntitlement;
    }

    public boolean isMultiCallEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(MULTI_CALL, FeatureType.DEVELOPER, false);
    }

    public boolean isRoomModerationEnabled() {
        return deviceRegistration.getFeatures().isEntitlementFeatureEnabled(ROOM_MODERATION_ENTITLEMENT, false);
    }

    public boolean isMoveRoomToTeamEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(MOVE_ROOM_TO_TEAM, FeatureType.DEVELOPER, false);
    }

    public boolean hasEscalateOneToOneEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(ANDROID_ESCALATE_ONE_TO_ONE, FeatureType.DEVELOPER, false);
    }

    public boolean isRemoveRoomFromTeamEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(REMOVE_ROOM_FROM_TEAM, FeatureType.DEVELOPER, false);
    }

    public boolean isAddIntegrationAndBotsEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ADD_INTEGRATIONS_AND_BOTS, false);
    }

    public boolean isRoomOwnershipAndRetentionFeatureEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ROOM_OWNERSHIP_AND_RETENTION, false);
    }

    public boolean isSparkSpaceSipUriEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_SPARK_SPACE_SIPURI, false);
    }

    public boolean isWirelessDesktopShareDetectionEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_DESKTOPSHARE_EARLYADOPTERS, ANDROID_DESKTOPSHARE);
    }

    public boolean isDeepLinkingMeetEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_DEEP_LINKING_MEET, false);
    }

    public boolean isDeepLinkingSpacesEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_DEEP_LINKING_SPACES, true);
    }

    public boolean isDeepLinkingTeamsEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_DEEP_LINKING_TEAMS, true);
    }

    public boolean isShortTermSpaceMeetingLinkEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_SHORT_TERM_SPACE_MEETING_LINK, false);
    }

    public boolean isContactCardEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_CONTACT_CARD, false);
    }

    public boolean isAndroidPmrSettingsEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_PMR_SETTINGS, false);
    }

    public boolean isDevConsoleEnabled() {
        // Debug build defaults to true except during tests
        return false;
    }

    public Boolean isUserToggleGroupMessageNotificationsEnabled() {
        return deviceRegistration.getFeatures().isUserFeatureEnabled(USER_TOGGLE_GROUP_MESSAGE_NOTIFICATIONS);
    }

    public Boolean isUserToggleMentionNotificationsEnabled() {
        return deviceRegistration.getFeatures().isUserFeatureEnabled(USER_TOGGLE_MENTION_NOTIFICATIONS);
    }

    public boolean isVectorClockProcessingEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_VECTOR_CLOCK_PROCESSING, false);
    }

    public boolean isAutoJoin() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_AUTO_JOIN, false);
    }

    public boolean isFeedbackViaEmailEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(FEEDBACK_VIA_EMAIL, false);
    }

    public boolean isEmojiEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(EMOJI, true);
    }

    public boolean isLockMeetingFeatureEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_MEETING_LOCK, false);
    }

    public boolean isAudioOnlyCallsEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(AUDIO_ONLY_CALLS_ENABLED, true);
    }

    public boolean isCallEmptyUxEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_CALL_EMPTYUX, false);
    }

    public boolean isLockWhiteboardToLandscapeEnabled() {
        return false;
    }

    public int getMeetUrlMaxConvParticipants() {
        final int defaultMeetUrlMaxConvParticipants = getMaxBridgeSize();
        return deviceRegistration.getFeatures().getFeatureInt(MEET_MAX_CONV_PARTICIPATNS, FeatureType.DEVELOPER, defaultMeetUrlMaxConvParticipants);
    }

    public boolean isActivityPruningEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_ACTIVITY_PRUNING, false);
    }

    public boolean isProximityMeasurementEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_PROXIMITY_MEASUREMENT, false);
    }

    public boolean isPresenceVisualizationEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_PRESENCE_VISUALIZATION, false);
    }

    public boolean isAddGuestEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(ADD_GUEST, FeatureType.DEVELOPER, false);
    }

    public boolean isUltraSoundProximityNotificationsEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(ULTRASOUND_PROXIMITY_NOTIFICATIONS, FeatureType.DEVELOPER, false);
    }

    public boolean isAddTeamMembersWhenMovingRoomEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(MOVE_ROOM_ADD_MEMBERS, FeatureType.DEVELOPER, false);
    }

    public boolean isQuietTimeEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(USER_PRESENCE_QUIET_TIME, FeatureType.DEVELOPER, false);
    }

    public boolean hasDirectSharesEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(DIRECT_SHARE, FeatureType.DEVELOPER, false);
    }

    public int getDialTimeoutSeconds() {
        // Default value is set in RingbackNotification (but overridden for testing in MockRingbackNotification)
        return deviceRegistration.getFeatures().getFeatureInt(DIAL_TIMEOUT_SECONDS, FeatureType.DEVELOPER, 0);
    }

    public boolean isAvoidOtherPairingCloudBerryCallEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(LYRA_AVOID_OTHER_PAIRING_CLOUD_BERRY_CALL, FeatureType.DEVELOPER, false);
    }

    public boolean isCallOutSuggestMatchesEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_CALL_OUT_SUGGEST_MATCHES, false);
    }

    public boolean isSyncingIndicatorEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_SYNCING_INDICATOR, false);
    }

    public boolean isLyraRoomServiceEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(LYRA_ROOM_SERVICE, false);
    }

    /**************
     * SDK FEATURES
     **************/
    // Call Features
    @Override
    public boolean isSparkPMREnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_SPARK_PMR, false);
    }

    @Override
    public boolean isNotifyFailedSendsEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_NOTIFY_FAILED_SENDS, false);
    }

    @Override
    public boolean isScheduledMeetingV2Enabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(SCHEDULED_MEETING_V2, false);
    }

    @Override
    public boolean isScheduledMeetingV3Enabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(SCHEDULED_MEETING_V3, false);
    }


    // Conversation Features
    @Override
    public boolean isRoomBindingEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ROOM_BINDING_RELEASE, ROOM_BINDING);
    }

    @Override
    public boolean isTeamMember() {
        return deviceRegistration.getFeatures().isEntitlementFeatureEnabled(TEAM_MEMBER_ENTITLEMENT, false);
    }

    @Override
    public boolean isUserPresenceEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(USER_PRESENCE, FeatureType.DEVELOPER, false);
    }

    @Override
    public boolean isAndroidUserPresenceEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(ANDROID_USER_PRESENCE, FeatureType.DEVELOPER, false);
    }

    @Override
    public boolean isPersonalUserPresenceEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(USER_PRESENCE_ENABLED, FeatureType.USER, true);
    }

    @Override
    public int getPostCommentTimeoutSeconds() {
        return Math.max(5, deviceRegistration.getFeatures().getFeatureInt(POST_COMMENT_TIMEOUT_SECONDS, FeatureType.DEVELOPER, 30));
    }

    @Override
    public boolean isContentSearchEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(CONTENT_SEARCH, false);
    }

    @Override
    public boolean isMessageSearchEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(MESSAGE_SEARCH, false);
    }


    // Whiteboard Features
    @Override
    public boolean isWhiteboardEnabled() {
        return true;
    }

    @Override
    public boolean isNativeWhiteboardEnabled() {
        return true;
    }

    @Override
    public boolean isSendWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_SEND_WHITEBOARD_EARLYADOPTERS,
                ANDROID_SEND_WHITEBOARD);
    }

    @Override
    public boolean isDeleteWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_DELETE_WHITEBOARD);
    }

    @Override
    public boolean isLockWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_LOCK_WHITEBOARD);
    }

    @Override
    public boolean isWhiteBoardAddGuestAclEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_WHITEBOARD_ADD_GUEST_ACL, false);
    }

    // Call / media

    @Override
    public boolean isRoapEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ROAP_ENABLED, false);
    }

    @Override
    public int getMaxRosterSize() {
        final int defaultMaxRosterSize = getMaxBridgeSize();
        return deviceRegistration.getFeatures().getFeatureInt(MAX_ROSTER_SIZE, FeatureType.DEVELOPER, defaultMaxRosterSize);
    }

    @Override
    public int getMaxBridgeSize() {
        final int defaultMaxBridgeSize = 12;
        return deviceRegistration.getFeatures().getFeatureInt(MAX_BRIDGE_SIZE, FeatureType.DEVELOPER, defaultMaxBridgeSize);
    }

    @Override
    public boolean uploadCallLogs() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(UPLOAD_CALL_LOGS, false);
    }

    @Override
    public boolean isNumericDialingEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(NUMERIC_DIALING_ENABLED, FeatureType.DEVELOPER, false);
    }

    @Override
    public boolean isCallSpinnerEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_CALL_SPINNER, false);
    }

    @Override
    public boolean isDeltaEventEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_LOCUS_DELTA_EVENT, false);
    }

    @Override
    public boolean isScreenSharingEnabled() {
        return UIUtils.hasLollipop() && deviceRegistration.getFeatures().isAnyToggleEnabled(SCREEN_SHARING, SCREEN_SHARING_SPARKANS);
    }

    @Override
    public boolean isActiveSpeakerViewEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_ACTIVE_SPARKER_VIEWER, false);
    }

    @Override
    public boolean isMultistreamEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(CALL_FILMSTRIP, false);
    }

    @Override
    public boolean isHardwareCodecEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_ENABLE_HARDWARE_CODEC, false);
    }

    @Override
    public boolean isCallLossRecordAudioEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(CALL_LOSS_RECORD_AUDIO, false);
    }

    @Override
    public boolean isCallLossRecordVideoEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(CALL_LOSS_RECORD_VIDEO, false);
    }

    @Override
    public boolean isCallFecVideoEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(CALL_FEC_VIDEO, false);
    }

    @Override
    public boolean isMediaAudioAllCodecsEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(MEDIA_ENABLE_AUDIO_ALL_CODECS, false);
    }

    @Override
    public boolean isTcAecEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_TC_AEC, false);
    }

    @Override
    public boolean useBridgeTestV2() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(BRIDGE_TEST_V2, true);
    }

    @Override
    public boolean isBufferedMercuryEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(BUFFERED_MERCURY, false);
    }

    @Override
    public int getMediaReconnectTimeout() {
        return deviceRegistration.getFeatures().getFeatureInt(MEDIA_RECONNECT_TIMEOUT, FeatureType.DEVELOPER, 90);
    }

    @Override
    public boolean isImplicitBindingForCallEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_IMPLICIT_BINDING_WHEN_CALLING_RELEASE, ANDROID_IMPLICIT_BINDING_WHEN_CALLING);
    }


    // Proximity

    @Override
    public boolean isProximityImprovementEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_PROXIMITY_IMPROVEMENT, false);
    }

    @Override
    public boolean enableListeningTokenUsingWme() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(LISTENING_TOKEN_USING_WME, false);
    }

    // Metrics

    @Override
    public boolean isSegmentMetricsEnabled() {
        return true;
    }

    @Override
    public boolean isHidePathIpAddressEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(HIDE_PATH_IP_ADDRESS, false);
    }

    @Override
    public boolean isExceptionAlertEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_EXCEPTION_ALERT, false);
    }

    @Override
    public boolean isLocationSharingEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(SHARE_LOCATION, FeatureType.DEVELOPER, false);
    }

    @Override
    public boolean hasDisabledPinEnforcement() {
        return deviceRegistration.getFeatures().isFeatureEnabled(DISABLE_PIN_ENFORCEMENT, FeatureType.DEVELOPER, false);
    }

    @Override
    public boolean hasUserDisabledProximityFeatures() {
        return deviceRegistration.getFeatures().isFeatureEnabled(ULTRASOUND_PROXIMITY_DISABLE_OVERRIDE, FeatureType.USER, false);
    }

    @Override
    public boolean isAnalyticsUserAliased() {
        return deviceRegistration.getFeatures().isFeatureEnabled(ANALYTICS_USER_ALIASED, FeatureType.USER, false);
    }

    @Override
    public boolean isVoicemailV1Enabled() {
        // 6/30/2017: Disabling VM integration until a CI entitlement solution for test users is found.
        // Currently, test and production users have sufficient entitlements to user VM, but the entitlement
        // needed on a machine account in the VoicemailTest IT to set test data is not possible.
        //
        //return isDeveloperFeatureEnabled(VOICEMAIL_ENABLED_FOR_ORG, false) && isDeveloperFeatureEnabled(VOICEMAIL_V1, false);
        return false;
    }

    @Override
    public boolean isParticipatingOrgInfoEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(PARTICIPATING_ORG_INFO, false);
    }
}
