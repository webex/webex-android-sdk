package com.cisco.spark.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.cisco.spark.android.R;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.notification.Gcm;
import com.cisco.spark.android.notification.SnoozeStore;
import com.cisco.spark.android.util.CollectionUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.google.gson.Gson;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Settings implements SnoozeStore {
    public static final String WEBEX_MEETINGS_PACKAGE_NAME = "com.cisco.webex.meetings";
    private static final String AUTHENTICATED_USER = "AUTHENTICATED_USER";

    private final SharedPreferences preferences;
    private final Gson gson;
    private final SharedPreferences defaultSharedPreferences;
    private final Context context;
    private boolean useCommit;
    private String emailHasPassword;

    public Settings(SharedPreferences userPreferences, Context context, Gson gson) {
        this.preferences = userPreferences;
        this.defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.context = context;
        this.gson = gson;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return gson.fromJson(preferences.getString(AUTHENTICATED_USER, null), AuthenticatedUser.class);
    }

    @SuppressLint("CommitPrefEdits")
    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        preferences.edit().putString(AUTHENTICATED_USER, authenticatedUser != null ? gson.toJson(authenticatedUser) : null)
                   .commit();
    }

    public boolean getLocationSharingEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_location_sharing_enabled, false);
    }

    public void setLocationSharingEnabled(boolean b) {
        put(defaultSharedPreferences, R.string.pref_key_location_sharing_enabled, b);
    }

    public String getLinusName() {
        return get(defaultSharedPreferences, R.string.pref_key_linus_name, "");
    }

    public String getCustomCallFeature() {
        return get(defaultSharedPreferences, R.string.pref_key_custom_call_feature, "");
    }

    public String getMediaOverrideIpAddress() {
        return get(defaultSharedPreferences, R.string.pref_key_media_override_ip_address, "").trim();
    }

    public boolean getUseResourceForCalls() {
        return get(defaultSharedPreferences, R.string.pref_key_use_resource_for_calls, false);
    }

    public void setResourceForCalls(String resourceForCalls) {
        put(defaultSharedPreferences, R.string.pref_key_resource_for_calls, resourceForCalls);
    }

    public String getResourceForCalls() {
        return get(defaultSharedPreferences, R.string.pref_key_resource_for_calls, "").trim();
    }


    public boolean getPstnEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_pstn_enabled, false);
    }

    public String getNetworkCongestion() {
        return get(defaultSharedPreferences, R.string.pref_key_network_congestion, "none");
    }

    public String getAudioCodec() {
        return get(defaultSharedPreferences, R.string.pref_key_audio_codec, "default");
    }

    public String getCustomWdmUrl() {
        return get(defaultSharedPreferences, R.string.pref_key_custom_wdm_url, "").trim();
    }

    public void setCustomWdmUrl(String url) {
        put(defaultSharedPreferences, R.string.pref_key_custom_wdm_url, url);
    }

    public boolean allowUnsecuredConnection() {
        return get(defaultSharedPreferences, R.string.pref_key_allow_unsecured_connection, false);
    }

    public void setAllowUnsecuredConnection(boolean allow) {
        put(defaultSharedPreferences, R.string.pref_key_allow_unsecured_connection, allow);
    }

    public boolean isEncryptedUIEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_encrypted_conversation_icons, false);
    }

    public void setEncryptedUIEnabled(boolean b) {
        put(defaultSharedPreferences, R.string.pref_key_encrypted_conversation_icons, b);
    }

    public void setEmail(String email) {
        put(defaultSharedPreferences, R.string.pref_key_email, email);
    }

    public String getEmail() {
        return get(defaultSharedPreferences, R.string.pref_key_email, (String) null);
    }

    public void setIsEmailEdited(boolean value) {
        put(defaultSharedPreferences, R.string.pref_key_edited, value);
    }

    public boolean isEmailEdited() {
        return get(defaultSharedPreferences, R.string.pref_key_edited, false);
    }

    public boolean isEmailVerified() {
        return get(defaultSharedPreferences, R.string.pref_key_is_email_verified, false);
    }

    public void setEmailVerified(boolean isVerified) {
        put(defaultSharedPreferences, R.string.pref_key_is_email_verified, isVerified);
    }

    public void setDeviceId(String deviceId) {
        put(defaultSharedPreferences, R.string.pref_key_device_id, deviceId);
    }

    public String getDeviceId() {
        return get(defaultSharedPreferences, R.string.pref_key_device_id, (String) null);
    }

    public void setMessageId(String messageId) {
        put(defaultSharedPreferences, R.string.pref_key_message_id, messageId);
    }

    public String getMessageId() {
        return get(defaultSharedPreferences, R.string.pref_key_message_id, (String) null);
    }

    public void setUserJustCreated(boolean value) {
        put(defaultSharedPreferences, R.string.pref_key_user_created, value);
    }

    public boolean getUserJustCreated() {
        return get(defaultSharedPreferences, R.string.pref_key_user_created, false);
    }

    public void setWebExBannerDismissed(boolean b) {
        put(defaultSharedPreferences, R.string.pref_key_show_webex_install_banner, b);
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        final PackageManager pm = context.getPackageManager();

        try {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }

    public boolean showWebExInstallBanner() {
        boolean bannerDismissed = get(defaultSharedPreferences, R.string.pref_key_show_webex_install_banner, false);
        boolean webexInstalled = isPackageInstalled(context, WEBEX_MEETINGS_PACKAGE_NAME);

        return !(webexInstalled || bannerDismissed);
    }

    public boolean showWebExInstallReminder() {
        boolean bannerDismissed = get(defaultSharedPreferences, R.string.pref_key_show_webex_install_banner, false);
        boolean webexInstalled = isPackageInstalled(context, WEBEX_MEETINGS_PACKAGE_NAME);

        return (!webexInstalled && bannerDismissed);
    }

    public boolean isUseDeskFeedback() {
        return get(defaultSharedPreferences, R.string.pref_key_use_desk_feedback, false);
    }

    public boolean isTcAecEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_tc_aec_enabled, false);
    }

    public void setTcAecEnabled(boolean enabled) {
        put(defaultSharedPreferences, R.string.pref_key_tc_aec_enabled, enabled);
    }

    public boolean isSrtpDisabled() {
        return get(defaultSharedPreferences, R.string.pref_key_srtp_disabled, false);
    }

    public boolean isPerformanceStatsEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_performance_statistics_enabled, false);
    }

    public boolean isForceHardwareCodecEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_hardware_codec_enabled, false);
    }

    public boolean isAlwaysOnProximityEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_always_on_proximity_enabled, false);
    }

    public boolean isAlwaysOnProximityStartOnBackgroundTransition() {
        return get(defaultSharedPreferences, R.string.pref_key_always_on_proximity_when_background, false);
    }

    public boolean isAlwaysOnProximityStartOnceOnBackgroundTransition() {
        return get(defaultSharedPreferences, R.string.pref_key_always_on_proximity_once_when_background, false);
    }

    public void setAlwaysOnProximityStartOnceOnBackgroundTransition(boolean b) {
        put(defaultSharedPreferences, R.string.pref_key_always_on_proximity_once_when_background, b);
    }

    public boolean isAlwaysOnProximityStartOnBoot() {
        return get(defaultSharedPreferences, R.string.pref_key_always_on_proximity_start_on_boot, false);
    }

    public boolean isAlwaysOnProximityWhenPackageUpdated() {
        return get(defaultSharedPreferences, R.string.pref_key_always_on_proximity_start_when_package_updated, false);
    }

    public boolean isNotificationSoundEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_notification_sound_enabled, true);
    }

    public boolean isNotificationVibrateEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_notification_vibrate_enabled, true);
    }

    public boolean isAutoReportEnabled() {
        return get(defaultSharedPreferences, R.string.pref_key_auto_report_enabled, false);
    }

    public void setAutoReportEnabled(boolean b) {
        put(defaultSharedPreferences, R.string.pref_key_auto_report_enabled, b);
    }

    @Override
    public long getSnoozeUntil() {
        return preferences.getLong(getPrefKey(R.string.pref_key_snooze_until), 0);
    }

    @Override
    public void setSnoozeUntil(long time) {
        preferences.edit().putLong(getPrefKey(R.string.pref_key_snooze_until), time).apply();
    }

    public boolean isSnoozeEnabled(boolean defaultSetting) {
        return get(defaultSharedPreferences, R.string.pref_key_snooze_enabled, defaultSetting);
    }

    public DeviceRegistration getDeviceRegistration() {
        String deviceRegistrationJson = get(preferences, R.string.pref_key_device_registration, "");
        if (Strings.isEmpty(deviceRegistrationJson)) {
            return new DeviceRegistration();
        } else {
            try {
                return gson.fromJson(deviceRegistrationJson, DeviceRegistration.class);
            } catch (Exception e) {
                return new DeviceRegistration();
            }
        }
    }

    public void setDeviceRegistration(DeviceRegistration registration) {
        registration.setFeaturesLoaded(true);
        put(preferences, R.string.pref_key_device_registration, registration.toJson(gson));
    }

    public boolean isContactSyncEnabled() {
        return get(preferences, R.string.pref_key_native_contacts, false);
    }

    public void setContactSyncEnabled(boolean value) {
        put(preferences, R.string.pref_key_native_contacts, value);
    }

    public boolean shouldUseCalendar() {
        return get(preferences, R.string.pref_key_use_calendar, false);
    }

    public void setUseCalendar(boolean value) {
        put(preferences, R.string.pref_key_use_calendar, value);
    }

    public Set<String> getCalendarsToUse() {
        return get(defaultSharedPreferences, R.string.pref_key_calendars_to_display, (Set<String>) null);
    }

    public void setCalendarsToUse(Set<String> calendars) {
        put(defaultSharedPreferences, R.string.pref_key_calendars_to_display, calendars);
    }

    public boolean shouldUseAlternateConversationIconColor() {
        return get(defaultSharedPreferences, R.string.pref_key_alt_conversation_color, false);
    }

    public boolean hasSeenPairedTour() {
        return get(defaultSharedPreferences, R.string.pref_key_paired_tour, false);
    }

    public void setHasSeenPairedTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_paired_tour, seen);
    }

    public boolean hasSeenSparkBoardTour() {
        return get(defaultSharedPreferences, R.string.pref_key_spark_board_tour, false);
    }

    public void setHasSeenSparkBoardTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_spark_board_tour, seen);
    }

    public boolean hasSeenSparkVideoSystemTour() {
        return get(defaultSharedPreferences, R.string.pref_key_spark_video_system_tour, false);
    }

    public void setHasSeenSparkVideoSystemTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_spark_video_system_tour, seen);
    }

    public boolean hasSeenRoomTour() {
        return get(defaultSharedPreferences, R.string.pref_key_room_tour, false);
    }

    public void setHasSeenRoomTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_room_tour, seen);
    }

    public boolean hasSeenHomeTour() {
        return get(defaultSharedPreferences, R.string.pref_key_home_tour, false);
    }

    public void setHasSeenHomeTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_home_tour, seen);
    }

    public boolean hasSeenNewRoomTour() {
        return get(defaultSharedPreferences, R.string.pref_key_new_room_tour, false);
    }

    public void setHasSeenNewRoomTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_new_room_tour, seen);
    }

    public boolean hasSeenTeamRoomTour() {
        return get(defaultSharedPreferences, R.string.pref_key_team_room_tour, false);
    }

    public void setHasSeenTeamRoomTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_team_room_tour, seen);
    }

    public void setCurrentFilter(int filter) {
        put(defaultSharedPreferences, R.string.pref_key_current_filter, filter);
    }

    public int getCurrentFilter() {
        return get(defaultSharedPreferences, R.string.pref_key_current_filter, 0);
    }

    public void setNumSecurityEnforcementChecks(int numChecks) {
        put(defaultSharedPreferences, R.string.pref_key_num_security_enforcement_checks, numChecks);
    }

    public int getNumSecurityEnforcementChecks() {
        return get(defaultSharedPreferences, R.string.pref_key_num_security_enforcement_checks, 0);
    }

    public void setNumSecurityAlertSettingsSelected(int timesSelected) {
        put(defaultSharedPreferences, R.string.pref_key_num_security_alert_settings_selected, timesSelected);
    }

    public int getNumSecurityAlertSettingsSelected() {
        return get(defaultSharedPreferences, R.string.pref_key_num_security_alert_settings_selected, 0);
    }

    public void setLastSecurityAlertTime(long time) {
        put(defaultSharedPreferences, R.string.pref_key_last_security_alert_time, time);
    }

    public long getLastSecurityAlertTime() {
        return get(defaultSharedPreferences, R.string.pref_key_last_security_alert_time, 0L);
    }

    public void setCallOutLastInputTypeUsed(int inputType) {
        put(defaultSharedPreferences, R.string.pref_key_call_out_last_input_type_used, inputType);
    }

    public int getCallOutLastInputTypeUsed() {
        return get(defaultSharedPreferences, R.string.pref_key_call_out_last_input_type_used, 0);
    }

    public boolean hasLoadedStickyPack() {
        return get(defaultSharedPreferences, R.string.pref_key_has_loaded_sticky_pack, false);
    }

    public void setHasLoadedStickyPack(boolean hasLoadedStickyPack) {
        put(defaultSharedPreferences, R.string.pref_key_has_loaded_sticky_pack, hasLoadedStickyPack);
    }

    public boolean hasLoadedMentions() {
        return get(defaultSharedPreferences, R.string.pref_key_has_loaded_mentions, false);
    }

    public void setHasLoadedMentions(boolean hasLoadedMentions) {
        put(defaultSharedPreferences, R.string.pref_key_has_loaded_mentions, hasLoadedMentions);
    }

    public boolean shouldSuppressCoachmarks() {
        return get(defaultSharedPreferences, R.string.pref_key_suppress_coachmarks, false);
    }

    public void setSuppressCoachmarks(boolean suppressCoachmarks) {
        put(defaultSharedPreferences, R.string.pref_key_suppress_coachmarks, suppressCoachmarks);
    }

    public boolean shouldIgnorePairingPermissions() {
        return get(defaultSharedPreferences, R.string.pref_key_ignore_pairing_permissions, false);
    }

    public void setIgnorePairingPermissions(boolean ignorePairingPermissins) {
        put(defaultSharedPreferences, R.string.pref_key_ignore_pairing_permissions, ignorePairingPermissins);
    }

    public void putOnboardingTeam(Team team) {
        put(defaultSharedPreferences, R.string.pref_key_onboarding_team, gson.toJson(team));
    }

    public Team getOnboardingTeam() {
        String json = defaultSharedPreferences.getString(context.getResources().getString(R.string.pref_key_onboarding_team), null);
        if (json != null) {
            return gson.fromJson(json, Team.class);
        }
        return null;
    }

    public void setHasPassword(boolean hasPassword) {
        put(defaultSharedPreferences, R.string.pref_key_email_has_password, hasPassword);
    }

    public boolean hasPassword() {
        return get(defaultSharedPreferences, R.string.pref_key_email_has_password, false);
    }

    public void setHasExplainedPhoneStatePermission(boolean hasExplained) {
        put(defaultSharedPreferences, R.string.pref_key_has_explained_phone_state_permissions, hasExplained);
    }

    public boolean hasExplainedPhoneStatePermission() {
        return get(defaultSharedPreferences, R.string.pref_key_has_explained_phone_state_permissions, false);
    }

    public void clear() {
        Set<String> keysToPersist = CollectionUtils.asSet(
                getPrefKey(R.string.pref_key_custom_wdm_url),
                getPrefKey(R.string.pref_key_allow_unsecured_connection),
                getPrefKey(R.string.pref_key_home_tour),
                getPrefKey(R.string.pref_key_room_tour),
                getPrefKey(R.string.pref_key_num_security_enforcement_checks),
                getPrefKey(R.string.pref_key_num_security_alert_settings_selected),
                getPrefKey(R.string.pref_key_last_security_alert_time),
                Gcm.PROPERTY_BUILD_TIME,
                Gcm.PROPERTY_REG_ID
        );

        SharedPreferences[] prefs = new SharedPreferences[]{preferences, defaultSharedPreferences};
        for (SharedPreferences pref : prefs) {
            final SharedPreferences.Editor edit = pref.edit();
            for (String key : pref.getAll().keySet()) {
                if (!keysToPersist.contains(key))
                    edit.remove(key);
            }
            edit.apply();
        }
    }

    public boolean needsSecurityCheck() {
        long currentTimestamp = new Date().getTime();
        long lastCheckTimestamp = get(defaultSharedPreferences, R.string.pref_key_last_security_check, 0L);
        return (currentTimestamp - lastCheckTimestamp) >= TimeUnit.DAYS.toMillis(7);
    }

    public void setLastSecurityCheck() {
        long currentTimestamp = new Date().getTime();
        put(defaultSharedPreferences, R.string.pref_key_last_security_check, currentTimestamp);
    }

    public String getPreloginUserId() {
        String uuid = get(defaultSharedPreferences, R.string.pref_key_prelogin_userid, (String) null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            put(defaultSharedPreferences, R.string.pref_key_prelogin_userid, uuid);
        }

        return uuid;
    }

    public void removePreloginUserId() {
        put(defaultSharedPreferences, R.string.pref_key_prelogin_userid, (String) null);
    }

    public boolean shouldAllowScreenshots() {
        return get(defaultSharedPreferences, R.string.pref_key_allow_screenshots, false);
    }

    // FIXME: This is the wrong way to expose the shared preferences. Should be injected by dagger instead.
    public SharedPreferences getPreferences() {
        return preferences;
    }

    public String getPrefKey(int id) {
        return context.getResources().getString(id);
    }

    private void put(SharedPreferences prefs, int id, boolean value) {
        apply(prefs.edit().putBoolean(getPrefKey(id), value));
    }

    private void put(SharedPreferences prefs, int id, String value) {
        apply(prefs.edit().putString(getPrefKey(id), value));
    }

    private void put(SharedPreferences prefs, int id, int value) {
        apply(prefs.edit().putInt(getPrefKey(id), value));
    }

    private void put(SharedPreferences prefs, int id, long value) {
        apply(prefs.edit().putLong(getPrefKey(id), value));
    }

    private void put(SharedPreferences prefs, int id, final Set<String> value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        apply(editor.putStringSet(getPrefKey(id), value));
    }

    private void apply(SharedPreferences.Editor editor) {
        if (useCommit) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    private boolean get(SharedPreferences prefs, int id, boolean valueIfAbsent) {
        return prefs.getBoolean(getPrefKey(id), valueIfAbsent);
    }

    private String get(SharedPreferences prefs, int id, String valueIfAbsent) {
        return prefs.getString(getPrefKey(id), valueIfAbsent);
    }

    private int get(SharedPreferences prefs, int id, int valueIfAbsent) {
        return prefs.getInt(getPrefKey(id), valueIfAbsent);
    }

    private long get(SharedPreferences prefs, int id, long valueIfAbsent) {
        return prefs.getLong(getPrefKey(id), valueIfAbsent);
    }

    private Set<String> get(SharedPreferences prefs, int id, Set set) {
        return prefs.getStringSet(getPrefKey(id), set);
    }

    /**
     * Synchronously commit settings to preferences. Espresso doesn't seem to monitor the asynchronous apply() and can end up
     * not finding view that are only visible when certain preferences are set.
     */
    public void useCommitInsteadOfApply(boolean useCommit) {
        this.useCommit = useCommit;
    }
}
