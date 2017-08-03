package com.cisco.spark.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.cisco.spark.android.R;
import com.cisco.spark.android.notification.Gcm;
import com.cisco.spark.android.notification.SnoozeStore;
import com.cisco.spark.android.util.CollectionUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.google.gson.Gson;

import java.util.Date;
import java.util.HashSet;
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

    public void setInstallReferrer(String referrer) {
        put(defaultSharedPreferences, R.string.pref_key_install_referrer, referrer);
    }

    public String getInstallReferrer() {
        return get(defaultSharedPreferences, R.string.pref_key_install_referrer, "");
    }

    public void setOnboardingEmail(String email) {
        put(defaultSharedPreferences, R.string.pref_key_onboarding_email, email);
    }

    public String getOnboardingEmail() {
        return get(defaultSharedPreferences, R.string.pref_key_onboarding_email, (String) null);
    }

    public void setDeviceId(String deviceId) {
        put(defaultSharedPreferences, R.string.pref_key_device_id, deviceId);
    }

    public String getDeviceId() {
        return get(defaultSharedPreferences, R.string.pref_key_device_id, (String) null);
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
        boolean webexInstalled = isCiscoWebexMeetingsAppInstalled();

        return !(webexInstalled || bannerDismissed);
    }

    public boolean isCiscoWebexMeetingsAppInstalled() {
        return  isPackageInstalled(context, WEBEX_MEETINGS_PACKAGE_NAME);
    }

    public boolean showWebExInstallReminder() {
        boolean bannerDismissed = get(defaultSharedPreferences, R.string.pref_key_show_webex_install_banner, false);
        boolean webexInstalled = isCiscoWebexMeetingsAppInstalled();

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

    public boolean isSw720pEnabed() {
        return get(defaultSharedPreferences, R.string.pref_key_enable_sw720p, false);
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
        Set<String> tempSet = get(defaultSharedPreferences, R.string.pref_key_calendars_to_display, (Set<String>) null);
        if (tempSet != null) {
            return new HashSet<>(get(defaultSharedPreferences, R.string.pref_key_calendars_to_display, (Set<String>) null));
        } else {
            return null;
        }
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

    public boolean hasSeenSparkRoomDeviceTour() {
        return get(defaultSharedPreferences, R.string.pref_key_spark_room_device_tour, false);
    }

    public void setHasSeenSparkRoomDeviceTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_spark_room_device_tour, seen);
    }

    public boolean hasSeenSparkBrickletRemovalTour() {
        return get(defaultSharedPreferences, R.string.pref_key_spark_bricklet_removal_tour, false);
    }

    public void setHasSeenSparkBrickletRemovalTour(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_spark_bricklet_removal_tour, seen);
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

    public boolean hasSeenSpaceBallCoachMark() {
        return get(defaultSharedPreferences, R.string.pref_key_space_ball_coach_mark, false);
    }

    public void setHasSeenSpaceBallCoachMark(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_space_ball_coach_mark, seen);
    }

    public boolean hasSeenBindingCoachMark() {
        return get(defaultSharedPreferences, R.string.pref_key_binding_coach_mark, false);
    }

    public void setHasSeenBindingCoachMark(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_binding_coach_mark, seen);
    }

    public boolean hasSeenDeviceSelectorCoachMark() {
        return get(defaultSharedPreferences, R.string.pref_key_device_selector_coach_mark, false);
    }

    public void setHasSeenDeviceSelectorCoachMark(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_device_selector_coach_mark, seen);
    }

    public boolean hasSeenRemoteControlCoachMark() {
        return get(defaultSharedPreferences, R.string.pref_key_remote_control_coach_mark, false);
    }

    public void setHasSeenRemoteControlCoachMark(boolean seen) {
        put(defaultSharedPreferences, R.string.pref_key_remote_control_coach_mark, seen);
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
                getPrefKey(R.string.pref_key_spark_board_tour),
                getPrefKey(R.string.pref_key_spark_room_device_tour),
                getPrefKey(R.string.pref_key_spark_bricklet_removal_tour),
                getPrefKey(R.string.pref_key_space_ball_coach_mark),
                getPrefKey(R.string.pref_key_binding_coach_mark),
                getPrefKey(R.string.pref_key_device_selector_coach_mark),
                getPrefKey(R.string.pref_key_remote_control_coach_mark),
                getPrefKey(R.string.pref_key_num_security_enforcement_checks),
                getPrefKey(R.string.pref_key_num_security_alert_settings_selected),
                getPrefKey(R.string.pref_key_last_security_alert_time),
                getPrefKey(R.string.pref_key_onboarding_email),
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

    public boolean shouldAllowScreenshots() {
        return get(defaultSharedPreferences, R.string.pref_key_allow_screenshots, true);
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
        apply(prefs.edit().putStringSet(getPrefKey(id), value));
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
