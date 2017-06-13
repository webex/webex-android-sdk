package com.cisco.spark.android.core;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.inputmethod.InputMethodManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Module for all Android related dependencies.
 */
@Module(
    complete = false,
    library = true
)
public class AndroidModule {

    @Provides
    @Singleton
    Context provideAppContext() {
        return Application.getInstance().getApplicationContext();
    }

    @Provides
    @Singleton
    SecureDevice provideSecureDevice(Context context) {
        return new SecureDevice(context);
    }

    @Provides
    SharedPreferences provideDefaultSharedPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    PackageInfo providePackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Provides
    TelephonyManager provideTelephonyManager(Context context) {
        return getSystemService(context, Context.TELEPHONY_SERVICE);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSystemService(Context context, String serviceConstant) {
        return (T) context.getSystemService(serviceConstant);
    }

    @Provides
    InputMethodManager provideInputMethodManager(final Context context) {
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Provides
    ApplicationInfo provideApplicationInfo(final Context context) {
        return context.getApplicationInfo();
    }

    @Provides
    AccountManager provideAccountManager(final Context context) {
        return AccountManager.get(context);
    }

    @Provides
    ClassLoader provideClassLoader(final Context context) {
        return context.getClassLoader();
    }

    @Provides
    NotificationManager provideNotificationManager(final Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    android.app.Application provideApplication() {
        return Application.getInstance();
    }

    @Provides
    Resources provideResources(final Context context) {
        return context.getResources();
    }

    @Provides
    Vibrator provideVibrator(final Context context) {
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Provides
    AudioManager provideAudioManager(final Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Provides
    ConnectivityManager provideConnectivityManager(final Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    SensorManager provideSensorManager(Context context) {
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Provides
    KeyguardManager provideKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Provides
    ActivityManager provideActivityManager(Context context) {
        return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Provides
    PowerManager providePowerManager(Context context) {
        return (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }
}
