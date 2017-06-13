package com.cisco.spark.android.core;

import android.os.Build;

import com.cisco.spark.android.app.ActivityManager;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.room.BackgroundUltrasoundProximityDetector;
import com.cisco.spark.android.room.CloudProximityBackend;
import com.cisco.spark.android.room.MockProximityDetector;
import com.cisco.spark.android.room.ProximityBackend;
import com.cisco.spark.android.room.ProximityDetector;
import com.cisco.spark.android.room.UltrasoundProximityDetector;
import com.cisco.spark.android.wdm.DeviceRegistration;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module(
        complete = false,
        library = true,
        injects = {}
)

public class LiveProximityModule {

    @Provides
    @Singleton
    ProximityBackend provideProximityService(ApiClientProvider apiClientProvider, DeviceRegistration deviceRegistration) {
        return new CloudProximityBackend(apiClientProvider, deviceRegistration);
    }

    @Provides
    @Singleton
    ProximityDetector provideProximityDetector(MediaEngine mediaEngine, ActivityManager activityManager, EventBus eventBus) {
        if (Build.FINGERPRINT.contains("generic")) {
            return new MockProximityDetector();
        } else {
            return new UltrasoundProximityDetector(mediaEngine, activityManager, eventBus);
        }
    }

    @Provides
    @Singleton
    BackgroundUltrasoundProximityDetector provideBackgroundDetector() {
        return new BackgroundUltrasoundProximityDetector();
    }


}
