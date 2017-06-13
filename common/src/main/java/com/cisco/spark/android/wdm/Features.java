package com.cisco.spark.android.wdm;

import android.os.Build;
import android.support.annotation.NonNull;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.TestUtils;
import com.github.benoitdion.ln.Ln;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Features {
    public static final String CALL_INITIATION_FEATURE_KEY = "call-initiation";
    public static final String DEV_CONSOLE_FEATURE_KEY = "console";
    public static final String TEAM_MEMBER_ENTITLEMENT = "team-member";
    public static final String UPLOAD_CALL_LOGS = "upload-call-logs";
    public static final String MAX_BRIDGE_SIZE = "bridge-max-conv-participants";
    public static final String MEET_MAX_CONV_PARTICIPATNS = "meet-url-max-conv-participants";
    public static final String MEET_URL_ONE_ON_ONE = "meet-url-one-on-one-enabled";
    public static final String MAX_ROSTER_SIZE = "locus-max-roster-participants";
    public static final String CALL_FEC_VIDEO = "media-mari-fec-video-enabled";
    public static final String CALL_LOSS_RECORD_AUDIO = "media-mari-loss-record-audio-enabled";
    public static final String CALL_LOSS_RECORD_VIDEO = "media-mari-loss-record-video-enabled";
    public static final String CALL_FILMSTRIP = "media-enable-filmstrip-android";
    public static final String ANDROID_TC_AEC = "android-tc-aec";
    public static final String ANDROID_ENABLE_HARDWARE_CODEC = "android-enable-hardware-codec";
    public static final String HIDE_PATH_IP_ADDRESS = "hide-path-ip-addresses";
    public static final String ROOM_MODERATION_ENTITLEMENT = "room-moderation";
    public static final String ADD_GUEST = "android-add-guest-release";
    public static final String ANDROID_DIRECT_UPLOAD = "android-direct-upload";
    public static final String CONTENT_SEARCH = "android-search-v2";
    public static final String SMART_SHRUNKEN_HEADS = "smart-shrunken-heads";
    public static final String MESSAGE_SEARCH = "android-message-search";
    public static final String MARKDOWN = "markdown";
    public static final String ULTRASOUND_PROXIMITY_NOTIFICATIONS = "android-ultrasound-notifications";
    public static final String NUMERIC_DIALING_ENABLED = "numeric-dialing";
    public static final String MULTI_CALL = "android-multi-call";
    public static final String STICKIES_SEND = "stickies.send";
    public static final String SHARE_LOCATION = "android-share-location";
    public static final String COACHMARK_IMPORTANT_FILTERS = "android-important-coachmark-filters";
    public static final String COACHMARK_IMPORTANT_SWITCH = "android-important-coachmark-switch";
    public static final String TEAMS = "android-teams";
    public static final String TEAMS_V2 = "android-teams-v2";
    public static final String ULTRASOUND_PROXIMITY_DISABLE_OVERRIDE = "android-ultrasound-proximity-disable-override";
    public static final String MODIFIERS_SEARCH = "android-search-v3";
    public static final String MODIFIERS_SEARCH_V2 = "android-search-v4";
    public static final String IMPORTANT_FILTER = "android-important-filter";
    public static final String USER_PRESENCE = "user-presence";
    public static final String ANDROID_USER_PRESENCE = "android-user-presence";
    public static final String EMOJI = "android-emoji";
    public static final String SCREEN_SHARING = "android-screen-sharing";
    public static final String MOVE_ROOM_TO_TEAM = "android-move-room-to-team";
    public static final String MEDIA_ENABLE_AUDIO_ALL_CODECS = "media-enable-audio-all-codecs";
    public static final String ANDROID_ESCALATE_ONE_TO_ONE = "android-escalate-one-to-one";
    public static final String CUSTOM_NOTIFICATIONS = "android-custom-notifications";
    public static final String ANDROID_FLAG = "android-flag";
    public static final String USER_TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS = "direct-message-notifications";
    public static final String USER_TOGGLE_GROUP_MESSAGE_NOTIFICATIONS = "group-message-notifications";
    public static final String USER_TOGGLE_MENTION_NOTIFICATIONS = "mention-notifications";
    public static final String LOCAL_CONTACTS_INTEGRATION = "android-local-contacts-integration";
    public static final String CUSTOM_ROOM_AVATARS = "android-custom-room-avatars";
    public static final String DIRECT_SHARE = "android-direct-share";
    public static final String USER_PRESENCE_QUIET_TIME = "android-quiet-time";
    public static final String DISABLE_PIN_ENFORCEMENT = "android-disable-pin-enforcement";
    public static final String NATIVE_CLIENT_LOBBY = "android-native-client-lobby";
    public static final String FEEDBACK_VIA_EMAIL = "feedback-via-email";
    public static final String GLOBAL_CUSTOM_NOTIFICATIONS_SETTINGS = "android-custom-notifications-global-settings";
    public static final String ADD_INTEGRATIONS_AND_BOTS = "android-add-integrations-and-bots";

    // Whiteboard features

    public static final String ANDROID_WHITEBOARD = "android-whiteboard";
    public static final String ANDROID_WHITEBOARD_EARLYADOPTERS = "android-whiteboard-ea1";

    public static final String ANDROID_WHITEBOARD_WITH_ACL = "android-whiteboard-with-acl";
    public static final String ANDROID_WHITEBOARD_WITH_ACL_RELEASE = "android-whiteboard-with-acl-release";

    public static final String ANDROID_SEND_WHITEBOARD = "android-send-whiteboard";
    public static final String ANDROID_SEND_WHITEBOARD_EARLYADOPTERS = "android-send-whiteboard-ea1";

    // This locks the whiteboard activity to landscape as a workaround for configuration changes in web whiteboard
    public static final String ANDROID_WHITEBOARDING_LANDSCAPE = "android-whiteboarding-landscape";
    public static final String ANDROID_WHITEBOARDING_LANDSCAPE_EARLYADOPTERS = "android-whiteboarding-landscape-ea1";

    public static final String ANDROID_USE_SHARED_WEBSOCKET = "android-use-shared-web-socket";

    public static final String ANDROID_NATIVE_WHITEBOARD = "android-native-whiteboard";
    public static final String ANDROID_NATIVE_WHITEBOARD_SPARKANS = "android-native-whiteboard-sparkans1";


    // Spark board support
    public static final String ROOM_BINDING = "android-room-binding";
    public static final String ROOM_BINDING_RELEASE = "android-room-binding-release";

    public static final String ANDROID_IMPLICIT_BINDING_WHEN_CALLING = "android-implicit-binding-for-call";
    public static final String ANDROID_IMPLICIT_BINDING_WHEN_CALLING_RELEASE = "android-implicit-binding-for-call-release";

    // Spaceballs / Spark 2.0 features
    public static final String ANDROID_SPACEBALLS = "android-spaceballs";
    public static final String ANDROID_SPACEBALLS_RELEASE = "android-spaceballs-release";
    public static final String ANDROID_SPACEBALLS_FILES = "android-spaceballs-files";

    public static final String ANDROID_INCALL_ROSTER = "android-show-roster-enabled";
    public static final String ANDROID_INCALL_ROSTER_RELEASE = "android-show-roster-enabled-release";

    public static final String ANDROID_DESKTOPSHARE = "android-desktop-share";
    public static final String ANDROID_DESKTOPSHARE_EARLYADOPTERS = "android-desktop-share-ea1";
    public static final String ANDROID_SPARK_SPACE_SIPURI = "android-spark-space-sipuri";

    // Proximity features
    public static final String ANDROID_NEW_PROXIMITY_AND_BINDING_COACHMARKS = "android-proximity-fte";
    public static final String ANDROID_NEW_PROXIMITY_AND_BINDING_COACHMARKS_SPARKANS = "android-proximity-fte-sparkans1";
    public static final String LYRA_VOLUME_CONTROL = "android-volume-control";
    public static final String LYRA_VOLUME_CONTROL_RELEASE = "android-volume-control-release";

    public static final String REMOVE_ROOM_FROM_TEAM = "android-remove-room-from-team";
    public static final String CUSTOM_NOTIFICATIONS_SHOW_ACTIVE_COUNT = "android-custom-notifications-show-active-count";
    public static final String CUSTOM_NOTIFICATIONS_SHOW_ACTIVE = "android-custom-notifications-show-active";
    public static final String ANDROID_SPARK_PMR = "android-spark-pmr";
    public static final String ALWAYS_ON_PROXIMITY = "android-always-on-proximity";
    public static final String RECORD_MEETINGS_CONTROL_ENABLED = "android-record-meetings-control-enabled";
    public static final String DIAL_TIMEOUT_SECONDS = "dial-timeout-seconds";
    public static final String MEDIA_RECONNECT_TIMEOUT = "media-reconnect-timeout";
    public static final String MOVE_ROOM_ADD_MEMBERS = "android-move-room-add-members";
    public static final String SILHOUETTE_REPLACEMENT = "replace-default-avatar";
    public static final String ROOM_OWNERSHIP_AND_RETENTION = "android-room-retention";
    public static final String CALLIOPE_DISCOVERY_FEATURE = "android-calliope-discovery";
    public static final String ANDROID_DELTA_EVENT = "android-delta-event";
    public static final String USER_PRESENCE_ENABLED = "user-presence-enabled";
    public static final String ANDROID_SPARK_ROOM_URL = "android-spark-room-url";
    public static final String AUDIO_ONLY_CALLS_ENABLED = "android-audio-only-calls";
    public static final String ANDROID_DEEP_LINKING_MEET = "android-cross-launch";
    public static final String ANDROID_DEEP_LINKING_SPACES = "android-deep-linking-rooms";
    public static final String ANDROID_DEEP_LINKING_TEAMS = "android-deep-linking-teams";
    public static final String ANDROID_SHORT_TERM_SPACE_MEETING_LINK = "android-short-term-space-meeting-link";
    public static final String LOCUS_ALLOW_SPARK_NEO_MEETING = "locus-allow-spark-neo-meeting";
    public static final String LOCUS_MEETING_LINK = "locus-meeting-link";
    public static final String ANALYTICS_USER_ALIASED = "analytics-user-aliased";
    public static final String ANDROID_PRESENCE_VISUALIZATION = "android-presence-visuals";
    public static final String ANDROID_WHITEBOARD_ADD_GUEST_ACL = "android-whiteboard-add-guest-acl";
    public static final String ANDROID_CALENDAR_SERVICE_MEETINGS = "android-calendar-service-meetings";

    // Filters
    public static final String ANDROID_UNREAD_FILTER_INDICATOR = "android-filters-unread-indicator";
    private static final String FILTER_SWIPE = "android-filter-swipe";

    public static final String ANDROID_MEETING_LOCK = "android-meeting-lock";

    // In Call features
    public static final String ANDROID_ROUND_AVATAR_FILMSTRIP = "android-spaceballs-filmstrip";

    // Set by GSON during deserialization
    private Set<FeatureToggle> entitlement = null;
    private Set<FeatureToggle> developer = null;
    private Set<FeatureToggle> user = null;

    // Will be false until we load the features from the server
    private boolean loaded;

    private transient Map<String, FeatureToggle> entitlementFeatureMap;
    private transient Map<String, FeatureToggle> developerFeatureMap;
    private transient Map<String, FeatureToggle> userFeatureMap;

    private static transient Map<String, FeatureToggle> deviceOverrides;

    private final transient Object sync = new Object();

    public Collection<FeatureToggle> getDeveloperFeatures() {
        return developer;
    }

    public Collection<FeatureToggle> getUserFeatures() {
        return user;
    }

    public void clear() {
        if (entitlement != null)
            entitlement.clear();
        if (developer != null)
            developer.clear();
        if (user != null)
            user.clear();
        if (entitlementFeatureMap != null)
            entitlementFeatureMap.clear();
        if (developerFeatureMap != null)
            developerFeatureMap.clear();
        if (userFeatureMap != null)
            userFeatureMap.clear();
    }

    public boolean isAvatarReplacementEnabled() {
        return isDeveloperFeatureEnabled(SILHOUETTE_REPLACEMENT, false);
    }

    public boolean isMarkdownEnabled() {
        return isDeveloperFeatureEnabled(MARKDOWN, isTeamMember());
    }

    public boolean isCustomNotificationsEnabled() {
        return isDeveloperFeatureEnabled(CUSTOM_NOTIFICATIONS, false);
    }

    public boolean areGlobalCustomNotificationSettingsEnabled() {
        return isDeveloperFeatureEnabled(GLOBAL_CUSTOM_NOTIFICATIONS_SETTINGS, false);
    }

    public boolean isNativeWhiteboardEnabled() {
        return isAnyToggleEnabled(ANDROID_NATIVE_WHITEBOARD_SPARKANS, ANDROID_NATIVE_WHITEBOARD);
    }

    public boolean isScreenSharingEnabled() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        return isDeveloperFeatureEnabled(SCREEN_SHARING, false);
    }

    public boolean isCustomNotificationsShowActiveCounter() {
        return isDeveloperFeatureEnabled(CUSTOM_NOTIFICATIONS_SHOW_ACTIVE_COUNT, false);
    }

    public boolean isAlwaysOnProximityEnabled() {
        return isDeveloperFeatureEnabled(ALWAYS_ON_PROXIMITY, false);
    }

    public boolean isLockWhiteboardToLandscapeEnabled() {
        return isAnyToggleEnabled(ANDROID_WHITEBOARDING_LANDSCAPE_EARLYADOPTERS, ANDROID_WHITEBOARDING_LANDSCAPE);
    }

    public boolean isFiltersUnreadIndicatorEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_UNREAD_FILTER_INDICATOR, false);
    }

    public boolean isFilterSwipingEnabled() {
        return isDeveloperFeatureEnabled(FILTER_SWIPE, false);
    }

    public boolean hasTabsNavigation() {
        return false;
    }

    public enum FeatureType { ENTITLEMENT, DEVELOPER, USER }

    //TODO: Why do we have 2 different enums to represent the same thing? We can probably delete one but
    //      would have to do some more digging to make sure there's no serialization that's dependent on one
    //      over the other
    public static FeatureToggleType featureTypeToFeatureToggleType(FeatureType type) {
        switch (type) {
            case ENTITLEMENT:
                return FeatureToggleType.ENTITLEMENT;
            case USER:
                return FeatureToggleType.USER;
            case DEVELOPER:
            default:
                return FeatureToggleType.DEV;
        }
    }

    public boolean isCallInitiationEnabled() {
        return isEntitlementFeatureEnabled(CALL_INITIATION_FEATURE_KEY, false);
    }

    public boolean isDevConsoleEnabled() {
        // Debug build defaults to true except during tests
        return isDeveloperFeatureEnabled(DEV_CONSOLE_FEATURE_KEY, BuildConfig.DEBUG && !TestUtils.isInstrumentation());
    }

    public boolean isTeamMember() {
        return isEntitlementFeatureEnabled(TEAM_MEMBER_ENTITLEMENT, false);
    }

    public boolean isRoomModerationEnabled() {
        return isEntitlementFeatureEnabled(ROOM_MODERATION_ENTITLEMENT, false);
    }

    public boolean uploadCallLogs() {
        return isDeveloperFeatureEnabled(UPLOAD_CALL_LOGS, false);
    }

    public boolean isCallFecVideoEnabled() {
        return isDeveloperFeatureEnabled(CALL_FEC_VIDEO, false);
    }

    public boolean isFeedbackViaEmailEnabled() {
        return isDeveloperFeatureEnabled(FEEDBACK_VIA_EMAIL, false);
    }

    public boolean isCallLossRecordAudioEnabled() {
        return isDeveloperFeatureEnabled(CALL_LOSS_RECORD_AUDIO, false);
    }

    public boolean isCallLossRecordVideoEnabled() {
        return isDeveloperFeatureEnabled(CALL_LOSS_RECORD_VIDEO, false);
    }

    public boolean isMultistreamEnabled() {
        return isDeveloperFeatureEnabled(CALL_FILMSTRIP, false);
    }

    public boolean isEmojiEnabled() {
        return isDeveloperFeatureEnabled(EMOJI, true);
    }

    public boolean isMediaAudioAllCodecsEnabled() {
        return isDeveloperFeatureEnabled(MEDIA_ENABLE_AUDIO_ALL_CODECS, false);
    }

    public boolean isTcAecEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_TC_AEC, false);
    }

    public boolean isHardwareCodecEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_ENABLE_HARDWARE_CODEC, false);
    }

    public boolean isHidePathIpAddressEnabled() {
        return isDeveloperFeatureEnabled(HIDE_PATH_IP_ADDRESS, false);
    }

    public boolean isContentSearchEnabled() {
        return isDeveloperFeatureEnabled(CONTENT_SEARCH, false);
    }

    public boolean isMessageSearchEnabled() {
        return isDeveloperFeatureEnabled(MESSAGE_SEARCH, false);
    }

    public boolean isSearchModifiersEnabled() {
        return isDeveloperFeatureEnabled(MODIFIERS_SEARCH, false);
    }

    public boolean isSearchModifierV2FeaturesEnabled() {
        return isDeveloperFeatureEnabled(MODIFIERS_SEARCH_V2, false);
    }

    public boolean isShowRosterEnabled() {
        return isAnyToggleEnabled(ANDROID_INCALL_ROSTER_RELEASE, ANDROID_INCALL_ROSTER);
    }

    public boolean isLockMeetingFeatureEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_MEETING_LOCK, false);
    }

    public boolean isSparkRoomURLEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_SPARK_ROOM_URL, false);
    }

    public boolean isSparkSpaceSipUriEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_SPARK_SPACE_SIPURI, false);
    }

    public boolean isAudioOnlyCallsEnabled() {
        return isDeveloperFeatureEnabled(AUDIO_ONLY_CALLS_ENABLED, true);
    }

    public boolean isNewProximityAndBindingCoachmarkEnabled() {
        return isAnyToggleEnabled(ANDROID_NEW_PROXIMITY_AND_BINDING_COACHMARKS_SPARKANS, ANDROID_NEW_PROXIMITY_AND_BINDING_COACHMARKS);
    }

    public boolean isRoomBindingEnabled() {
        return isAnyToggleEnabled(ROOM_BINDING_RELEASE, ROOM_BINDING);
    }

    public boolean isNativeClientLobbyEnabled() {
        return isDeveloperFeatureEnabled(NATIVE_CLIENT_LOBBY, false);
    }

    public int getMaxBridgeSize() {
        final int defaultMaxBridgeSize = 12;
        return getFeatureInt(MAX_BRIDGE_SIZE, FeatureType.DEVELOPER, defaultMaxBridgeSize);
    }

    public int getMeetUrlMaxConvParticipants() {
        final int defaultMeetUrlMaxConvParticipants = 0;
        return getFeatureInt(MEET_MAX_CONV_PARTICIPATNS, FeatureType.DEVELOPER, defaultMeetUrlMaxConvParticipants);
    }

    public boolean isMeetUrlOneOnOneEnabled() {
        return isDeveloperFeatureEnabled(MEET_URL_ONE_ON_ONE, false);
    }

    public int getMaxRosterSize() {
        final int defaultMaxRosterSize = getMaxBridgeSize();
        return getFeatureInt(MAX_ROSTER_SIZE, FeatureType.DEVELOPER, defaultMaxRosterSize);
    }

    public boolean isEntitlementFeatureEnabled(String key, boolean defaultValue) {
        return isFeatureEnabled(key, FeatureType.ENTITLEMENT, defaultValue);
    }

    public boolean isDeveloperFeatureEnabled(String key, boolean defaultValue) {
        return isFeatureEnabled(key, FeatureType.DEVELOPER, defaultValue);
    }

    public Boolean isUserFeatureEnabled(String key) {
        return isFeatureEnabled(key, FeatureType.USER);
    }

    public boolean isAddGuestEnabled() {
        return isFeatureEnabled(ADD_GUEST, FeatureType.DEVELOPER, false);
    }

    public boolean isStickiesEnabled() {
        return isFeatureEnabled(STICKIES_SEND, FeatureType.DEVELOPER, false);
    }

    public boolean isUltraSoundProximityNotificationsEnabled() {
        return isFeatureEnabled(ULTRASOUND_PROXIMITY_NOTIFICATIONS, FeatureType.DEVELOPER, false);
    }

    public boolean isNumericDialingEnabled() {
        return isFeatureEnabled(NUMERIC_DIALING_ENABLED, FeatureType.DEVELOPER, false);
    }

    public boolean isMultiCallEnabled() {
        return isFeatureEnabled(MULTI_CALL, FeatureType.DEVELOPER, false);
    }

    public boolean isSmartShrunkenHeadsEnabled() {
        return isFeatureEnabled(SMART_SHRUNKEN_HEADS, FeatureType.DEVELOPER, false);
    }

    public boolean isLocationSharingEnabled() {
        return isFeatureEnabled(SHARE_LOCATION, FeatureType.DEVELOPER, false);
    }

    public boolean hasSeenFiltersCoachmark() {
        return isFeatureEnabled(COACHMARK_IMPORTANT_FILTERS, FeatureType.USER, false);
    }

    public boolean hasSeenImportantSwitchCoachmark() {
        return isFeatureEnabled(COACHMARK_IMPORTANT_SWITCH, FeatureType.USER, false);
    }

    public boolean areTeamsEnabled() {
        return isFeatureEnabled(TEAMS, FeatureType.DEVELOPER, true) || isFeatureEnabled(TEAMS_V2, FeatureType.DEVELOPER, true);
    }

    public boolean isMoveRoomToTeamEnabled() {
        return isFeatureEnabled(MOVE_ROOM_TO_TEAM, FeatureType.DEVELOPER, false);
    }

    public boolean isAddTeamMembersWhenMovingRoomEnabled() {
        return isFeatureEnabled(MOVE_ROOM_ADD_MEMBERS, FeatureType.DEVELOPER, false);
    }

    public boolean isRemoveRoomFromTeamEnabled() {
        return isFeatureEnabled(REMOVE_ROOM_FROM_TEAM, FeatureType.DEVELOPER, false);
    }

    public boolean isUserPresenceEnabled() {
        return isFeatureEnabled(USER_PRESENCE, FeatureType.DEVELOPER, false);
    }

    public boolean isAndroidUserPresenceEnabled() {
        return isFeatureEnabled(ANDROID_USER_PRESENCE, FeatureType.DEVELOPER, false);
    }

    public boolean isPersonalUserPresenceEnabled() {
        return isFeatureEnabled(USER_PRESENCE_ENABLED, FeatureType.USER, true);
    }

    public boolean isQuietTimeEnabled() {
        return isFeatureEnabled(USER_PRESENCE_QUIET_TIME, FeatureType.DEVELOPER, false);
    }

    public boolean isImportantFilterEnabled() {
        return isFeatureEnabled(IMPORTANT_FILTER, FeatureType.DEVELOPER, false);
    }

    public boolean hasEscalateOneToOneEnabled() {
        return isFeatureEnabled(ANDROID_ESCALATE_ONE_TO_ONE, FeatureType.DEVELOPER, false);
    }

    public boolean hasCustomRoomAvatarsEnabled() {
        return isFeatureEnabled(CUSTOM_ROOM_AVATARS, FeatureType.DEVELOPER, false);
    }

    public boolean hasDirectSharesEnabled() {
        return isFeatureEnabled(DIRECT_SHARE, FeatureType.DEVELOPER, false);
    }

    public boolean hasDisabledPinEnforcement() {
        return isFeatureEnabled(DISABLE_PIN_ENFORCEMENT, FeatureType.DEVELOPER, false);
    }

    public boolean hasUserDisabledProximityFeatures() {
        return isFeatureEnabled(ULTRASOUND_PROXIMITY_DISABLE_OVERRIDE, FeatureType.USER, false);
    }

    public boolean isNativeContactsIntegrationEnabled() {
        return isFeatureEnabled(LOCAL_CONTACTS_INTEGRATION, FeatureType.DEVELOPER, false);
    }

    public boolean isFlaggingEnabled() {
        return isFeatureEnabled(ANDROID_FLAG, FeatureType.DEVELOPER, false);
    }

    public boolean isWhiteboardEnabled() {
        return isAnyToggleEnabled(ANDROID_WHITEBOARD_EARLYADOPTERS, ANDROID_WHITEBOARD);
    }

    public boolean isSendWhiteboardEnabled() {
        return isAnyToggleEnabled(ANDROID_SEND_WHITEBOARD_EARLYADOPTERS, ANDROID_SEND_WHITEBOARD);
    }

    public boolean isShowRecordMeetingsControlEnabled() {
        return isDeveloperFeatureEnabled(RECORD_MEETINGS_CONTROL_ENABLED, false);
    }

    public boolean isAddIntegrationAndBotsEnabled() {
        return isDeveloperFeatureEnabled(ADD_INTEGRATIONS_AND_BOTS, false);
    }

    public boolean isSpaceballsEnabled() {
        return isAnyToggleEnabled(ANDROID_SPACEBALLS, ANDROID_SPACEBALLS_RELEASE);
    }

    public boolean isFilesActivityEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_SPACEBALLS_FILES, false);
    }

    public boolean isRoomOwnershipAndRetentionFeatureEnabled() {
        return isDeveloperFeatureEnabled(ROOM_OWNERSHIP_AND_RETENTION, false);
    }

    public int getDialTimeoutSeconds() {
        // Default value is set in RingbackNotification (but overridden for testing in MockRingbackNotification)
        return getFeatureInt(DIAL_TIMEOUT_SECONDS, FeatureType.DEVELOPER, 0);
    }

    public int getMediaReconnectTimeout() {
        return getFeatureInt(MEDIA_RECONNECT_TIMEOUT, FeatureType.DEVELOPER, 90);
    }

    public boolean isWirelessDesktopShareDetectionEnabled() {
        return isAnyToggleEnabled(ANDROID_DESKTOPSHARE_EARLYADOPTERS, ANDROID_DESKTOPSHARE);
    }

    public boolean isRoomDeviceVolumeControlEnabled() {
        return isAnyToggleEnabled(LYRA_VOLUME_CONTROL_RELEASE, LYRA_VOLUME_CONTROL);
    }

    public boolean isImplicitBindingForCallEnabled() {
        return isAnyToggleEnabled(ANDROID_IMPLICIT_BINDING_WHEN_CALLING_RELEASE, ANDROID_IMPLICIT_BINDING_WHEN_CALLING);
    }

    public boolean isUseSharedFeatureToggleEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_USE_SHARED_WEBSOCKET, false);
    }

    public boolean isAnalyticsUserAliased() {
        return isFeatureEnabled(ANALYTICS_USER_ALIASED, FeatureType.USER, false);
    }

    public boolean isDeepLinkingMeetEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_DEEP_LINKING_MEET, false);
    }

    public boolean isDeepLinkingSpacesEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_DEEP_LINKING_SPACES, true);
    }

    public boolean isDeepLinkingTeamsEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_DEEP_LINKING_TEAMS, true);
    }

    public boolean isShortTermSpaceMeetingLinkEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_SHORT_TERM_SPACE_MEETING_LINK, false);
    }

    public boolean isWhiteBoardAddGuestAclEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_WHITEBOARD_ADD_GUEST_ACL, false);
    }

    public void setFeaturesLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean areLoaded() {
        return loaded;
    }

    private boolean isFeatureEnabled(String key, FeatureType featureType, boolean defaultValue) {

        Boolean override = getOverridenFeature(key, defaultValue);
        if (override != null) {
            return override;
        }

        FeatureToggle feature = getFeatureMap(featureType).get(key);

        if (feature != null) {
            return feature.getBooleanVal();
        } else {
            return defaultValue;
        }
    }

    public boolean isSparkPMREnabled() {
        return isDeveloperFeatureEnabled(ANDROID_SPARK_PMR, false);
    }

    public boolean isCalendarServiceMeetingsEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_CALENDAR_SERVICE_MEETINGS, false);
    }

    public boolean isDeltaEventEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_DELTA_EVENT, false);
    }

    public boolean isWhiteboardWithAclEnabled() {
        return isAnyToggleEnabled(ANDROID_WHITEBOARD_WITH_ACL_RELEASE, ANDROID_WHITEBOARD_WITH_ACL);
    }

    public boolean isPresenceVisualizationEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_PRESENCE_VISUALIZATION, false);
    }

    public boolean isRoundAvatarFilmstripEnabled() {
        return isDeveloperFeatureEnabled(ANDROID_ROUND_AVATAR_FILMSTRIP, false);
    }

    /**
     * For flexibility in graduating features, and to avoid graduating a feature to public that has
     * been in available in earlier version in an unfinished capacity, this is used to check
     * if either the pre production toggle or the release toggle is enabled, and enable the feature
     * if any is on.
     * <p>
     * This makes sure that whoever already have the feature toggle from a previous graduation keep
     * the feature, and that the public only can access the feature in the correct state.
     *
     * @return true if any of the toggles are true
     */
    public boolean isAnyToggleEnabled(@NonNull String... toggles) {
        for (String toggle : toggles) {
            boolean enabled = isFeatureEnabled(toggle, FeatureType.DEVELOPER, false);
            if (enabled) {
                return true;
            } else {
            }
        }
        return false;
    }


    /**
     * Tri-state variant of isFeatureEnabled, does not use defaultValue
     *
     * @return Will return tri-state value, true, false or null if not set
     */
    private Boolean isFeatureEnabled(String key, FeatureType featureType) {

        Boolean override = getOverridenFeature(key, false);
        if (override != null) {
            return override;
        }

        FeatureToggle feature = getFeatureMap(featureType).get(key);

        if (feature != null) {
            return feature.getBooleanVal();
        } else {
            return null;
        }
    }

    private int getFeatureInt(String key, FeatureType featureType, int defaultValue) {
        FeatureToggle feature = getFeatureMap(featureType).get(key);
        if (feature != null) {
            return feature.getIntVal();
        } else {
            return defaultValue;
        }
    }

    private Map<String, FeatureToggle> getFeatureMap(FeatureType featureType) {
        synchronized (sync) {
            if (featureType == FeatureType.ENTITLEMENT) {
                if (entitlementFeatureMap == null) {
                    entitlementFeatureMap = featureSetToMap(entitlement);
                }
                return entitlementFeatureMap;
            } else if (featureType == FeatureType.DEVELOPER) {
                if (developerFeatureMap == null) {
                    developerFeatureMap = featureSetToMap(developer);
                }
                return developerFeatureMap;
            } else {
                if (userFeatureMap == null) {
                    userFeatureMap = featureSetToMap(user);
                }
                return userFeatureMap;
            }
        }
    }

    private Map<String, FeatureToggle> featureSetToMap(Set<FeatureToggle> set) {
        Map<String, FeatureToggle> map = new HashMap<>();
        if (set != null) {
            for (FeatureToggle toggle : set) {
                map.put(toggle.getKey(), toggle);
            }
        }
        return map;
    }

    public FeatureToggle getFeature(FeatureType featureType, String key) {
        return getFeatureMap(featureType).get(key);
    }

    public void setDeveloperFeature(FeatureToggle toggle) {
        synchronized (sync) {

            if (developer == null) {
                developer = new HashSet<>();
            }

            developer.remove(toggle);
            developer.add(toggle);
            if (developerFeatureMap != null)
                developerFeatureMap.put(toggle.getKey(), toggle);
        }
    }

    public void setUserFeature(FeatureToggle toggle) {
        synchronized (sync) {

            user.remove(toggle);
            user.add(toggle);
            if (userFeatureMap != null)
                userFeatureMap.put(toggle.getKey(), toggle);
        }
    }

    public void unsetUserFeature(String key) {
        synchronized (sync) {
            Ln.v("unsetUserFeature feature key: %s", key);

            FeatureToggle feature = findFeatureByKey(key);
            if (feature != null) {
                Ln.v("unsetUserFeature found feature by key: %s", feature);
                user.remove(feature);
            }
            if (userFeatureMap != null) {
                userFeatureMap.remove(key);
            }
        }
    }

    private FeatureToggle findFeatureByKey(String key) {
        for (FeatureToggle featureToggle : user) {
            if (featureToggle.getKey().equals(key)) {
                return featureToggle;
            }
        }
        return null;
    }


    public void print(PrintWriter writer) {
        printSet("Entitlement toggles", entitlement, writer);
        printSet("Developer toggles", developer, writer);
        printSet("User toggles", user, writer);

        if (deviceOverrides == null) {
            writer.println("No device overrides");
        } else {
            printSet("Device overrides", new HashSet<>(deviceOverrides.values()), writer);
        }
    }

    private void printSet(String title, Set<FeatureToggle> toggles, PrintWriter writer) {
        if (toggles == null)
            return;
        writer.println(String.format(Locale.US, "%s:", title));
        writer.println(Strings.repeat("-", title.length() + 1));
        for (FeatureToggle toggle : toggles) {
            writer.println(toggle.getKey() + ": " + toggle.getVal());
        }
        writer.println("");
    }

    Object getSyncObject() {
        return sync;
    }

    // Device-speicfic overrides

    public static void setDeviceOverrides(Map<String, FeatureToggle> overrides) {
        deviceOverrides = overrides;
    }

    public Boolean getOverridenFeature(String key, boolean defaultValue) {

        if (deviceOverrides == null) {
            return null;
        }

        FeatureToggle override = deviceOverrides.get(key);

        if (override != null) {
            return override.getBooleanVal();
        } else {
            return defaultValue;
        }
    }
}
