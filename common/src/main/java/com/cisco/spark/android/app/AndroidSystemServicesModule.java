package com.cisco.spark.android.app;

import android.content.Context;

import com.cisco.spark.android.notification.AndroidNotificationManager;

import dagger.Module;
import dagger.Provides;

/**
 * In many places we depend on Android services available using {@link android.content.Context#getSystemService(String)}.
 * Hard dependency on those services makes our app really difficult to test. This is where {@link AndroidSystemServicesModule} comes in.
 *
 * Interfaces of Android system services is extracted to an interfaces with the same name.
 * For example, {@link android.app.NotificationManager} maps to {@link com.cisco.spark.android.app.NotificationManager}.
 *
 * This module provides a default implementation of those interfaces that simply maps to the Android equivalent.
 * In some cases, convenience methods are also added.
 *
 * In tests, we can either inject or construct our objects with a different test/mock/stub implementation.
 */
@Module(
    complete = false,
    library = true
)
public class AndroidSystemServicesModule {
    @Provides
    SensorManager provideSensorManager(final android.hardware.SensorManager sensorManager) {
        return new AndroidSensorManager(sensorManager);
    }

    @Provides
    NotificationManager provideNotificationManager(final android.app.NotificationManager notificationManager) {
        return new AndroidNotificationManager(notificationManager);
    }

    @Provides
    KeyguardManager provideKeyguardManager(final android.app.KeyguardManager keyguardManager) {
        return new AndroidKeyguardManager(keyguardManager);
    }

    @Provides
    ActivityManager provideActivityManager(final Context context, final android.app.ActivityManager activityManager) {
        return new AndroidActivityManager(context, activityManager);
    }

    @Provides
    AudioManager provideAudioManager(final android.media.AudioManager audioManager) {
        return new AndroidAudioManager(audioManager);
    }

    @Provides
    PowerManager providePowerManager(final android.os.PowerManager powerManager) {
        return new AndroidPowerManager(powerManager);
    }

    @Provides
    AccountManager provideAccountManager(final android.accounts.AccountManager accountManager) {
        return new AndroidAccountManager(accountManager);
    }
}
