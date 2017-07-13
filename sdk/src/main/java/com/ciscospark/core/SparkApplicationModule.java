package com.ciscospark.core;


import android.content.Context;
import android.os.Build;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSessionEngine;
import com.cisco.spark.android.media.MockMediaEngine;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscospark.phone.Phone;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module(
        complete = false,
        library = true,
        includes = {},
        injects = {
                SparkApplicationDelegate.class,
                Phone.class
        }
)
class SparkApplicationModule {

    @Provides
    @Singleton
    MediaEngine provideMediaEngine(LogFilePrint log, EventBus bus, Settings settings, Context context, DeviceRegistration deviceRegistration, Gson gson, Ln.Context lnContext) {
        if (Build.FINGERPRINT.contains("generic")) {
            // If running on an emulator, allow using the MockMediaEngine.
            return new MockMediaEngine(lnContext);
        } else {
            return new MediaSessionEngine(log, bus, settings, context, BuildConfig.GIT_COMMIT_SHA, deviceRegistration, gson, lnContext);
        }
    }
}