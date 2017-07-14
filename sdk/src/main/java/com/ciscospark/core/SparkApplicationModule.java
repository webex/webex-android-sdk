package com.ciscospark.core;


import android.content.Context;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.CallNotification;
import com.cisco.spark.android.callcontrol.CallUi;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.log.UploadLogsService;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSessionEngine;
import com.cisco.spark.android.meetings.LocusMeetingInfoProvider;
import com.cisco.spark.android.metrics.CallAnalyzerReporter;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.util.LinusReachabilityService;
import com.cisco.spark.android.util.Toaster;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscospark.phone.Phone;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Provider;
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
    private RefWatcher refWatcher;

    @Provides
    @Singleton
    public RefWatcher provideRefWatcher() {
        return refWatcher;
    }

    public void setRefWatcher(RefWatcher refWatcher) {
        this.refWatcher = refWatcher;
    }

    @Provides
    @Singleton
    MediaEngine provideMediaEngine(LogFilePrint log,
                                   EventBus bus,
                                   Settings settings,
                                   Context context,
                                   DeviceRegistration deviceRegistration,
                                   Gson gson,
                                   Ln.Context lnContext) {
        return new MediaSessionEngine(log, bus, settings, context, BuildConfig.GIT_COMMIT_SHA, deviceRegistration, gson, lnContext);
    }

    @Provides
    @Singleton
    CallControlService provideCallControlService(LocusService locusService,
                                                 MediaEngine mediaEngine,
                                                 CallMetricsReporter callMetricsReporter,
                                                 EventBus bus,
                                                 Context context,
                                                 TrackingIdGenerator trackingIdGenerator,
                                                 DeviceRegistration deviceRegistration,
                                                 LogFilePrint logFilePrint,
                                                 Gson gson,
                                                 UploadLogsService uploadLogsService,
                                                 CallNotification callNotification,
                                                 LocusDataCache locusDataCache,
                                                 Settings settings,
                                                 Provider<Batch> batchProvider,
                                                 com.github.benoitdion.ln.Ln.Context lnContext,
                                                 CallUi callUi,
                                                 LinusReachabilityService linusReachabilityService,
                                                 SdkClient sdkClient,
                                                 CallAnalyzerReporter callAnalyzerReporter,
                                                 Toaster toaster,
                                                 CoreFeatures coreFeatures,
                                                 LocusMeetingInfoProvider locusMeetingInfoProvider) {
        return new CallControlService(locusService, mediaEngine, callMetricsReporter, bus, context, trackingIdGenerator,
                deviceRegistration, logFilePrint, gson, uploadLogsService, callNotification, locusDataCache,
                settings, batchProvider, lnContext, callUi, linusReachabilityService, sdkClient,
                callAnalyzerReporter, toaster, coreFeatures, locusMeetingInfoProvider);
    }
}