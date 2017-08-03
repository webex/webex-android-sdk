package com.ciscospark.core;


import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.app.NotificationManager;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.CallHistoryService;
import com.cisco.spark.android.callcontrol.CallNotification;
import com.cisco.spark.android.callcontrol.CallUi;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.core.AccessManager;
import com.cisco.spark.android.core.AccountUi;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.BackgroundCheck;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.core.StatusManager;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.log.UploadLogsService;
import com.cisco.spark.android.lyra.LyraService;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSessionEngine;
import com.cisco.spark.android.media.MockMediaEngine;
import com.cisco.spark.android.meetings.LocusMeetingInfoProvider;
import com.cisco.spark.android.meetings.ScheduledMeetingsService;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.metrics.CallAnalyzerReporter;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.notification.Gcm;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.reachability.UIServiceAvailability;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sdk.SparkAndroid;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.ui.call.VideoMultitaskComponent;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.CpuLogger;
import com.cisco.spark.android.util.LinusReachabilityService;
import com.cisco.spark.android.util.LocationManager;
import com.cisco.spark.android.util.Sanitizer;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.Toaster;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.voicemail.VoicemailService;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardListService;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.ciscospark.phone.Call;
import com.ciscospark.phone.Phone;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.squareup.leakcanary.RefWatcher;


import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;

@Module(
        complete = false,
        library = true,
        includes = {},
        injects = {
                VideoTestApplicationDelegate.class,
                Phone.class,
                Call.class
        }
)
class VideoTestApplicationModule {

    private RefWatcher refWatcher;


    public VideoTestApplicationModule() {
    }

    public void setRefWatcher(RefWatcher refWatcher) {
        this.refWatcher = refWatcher;
    }


    @Provides
    @Singleton
    ApplicationController provideApplicationController(final Context context, final ApiClientProvider clientProvider, final ApiTokenProvider tokenProvider,
                                                       final AuthenticatedUserProvider userProvider, final EventBus bus, final DeviceRegistration deviceRegistration,
                                                       final BackgroundCheck backgroundCheck, final Settings settings, final MediaEngine mediaEngine,
                                                       final ActorRecordProvider actorRecordProvider, final MetricsReporter metricsReporter,
                                                       final StatusManager statusManager, final LocationManager locationManager, MercuryClient mercuryClient,
                                                       final SearchManager searchManager, final LocusService locusService, final CallHistoryService callHistoryService,
                                                       final CpuLogger cpuLogger, final ConversationSyncQueue conversationSyncQueue, NotificationManager notificationManager, AccessManager accessManager,
                                                       final KeyManager keyManager, UIServiceAvailability uiServiceAvailability, OperationQueue operationQueue, Injector injector,
                                                       final VideoMultitaskComponent videoMultitaskComponent, final Ln.Context lnContext, final AccountUi accountUi, final LogFilePrint log, final LinusReachabilityService linusReachabilityService,
                                                       final WhiteboardService whiteboardService,
                                                       final LyraService lyraService, final
                                                       UrlProvider urlProvider, final SdkClient sdkClient, final WhiteboardCache whiteboardCache, final VoicemailService voicemailService, final ScheduledMeetingsService scheduledMeetingsService,
                                                       final WhiteboardListService whiteboardListService) {
        /*
        return new ApplicationController(context, clientProvider, tokenProvider, userProvider,
                bus, deviceRegistration, backgroundCheck, settings,
                mediaEngine, actorRecordProvider, metricsReporter, statusManager, locationManager, mercuryClient,
                searchManager, locusService, callHistoryService, cpuLogger, conversationSyncQueue, notificationManager,
                accessManager, keyManager, uiServiceAvailability, operationQueue, injector, videoMultitaskComponent,
                lnContext, accountUi, log, linusReachabilityService, whiteboardService, lyraService, urlProvider,
                sdkClient, whiteboardCache);
                */


        //2.0.3886 updated
        return new ApplicationController(context, clientProvider, tokenProvider, userProvider, bus, deviceRegistration, backgroundCheck, settings,
                mediaEngine, actorRecordProvider, metricsReporter, statusManager, locationManager, mercuryClient,
                searchManager, locusService, callHistoryService, cpuLogger, conversationSyncQueue, notificationManager,
                accessManager, keyManager, uiServiceAvailability, operationQueue, injector, videoMultitaskComponent,
                lnContext, accountUi, log, linusReachabilityService, whiteboardService, lyraService, urlProvider,
                sdkClient, whiteboardCache, voicemailService, scheduledMeetingsService,
                whiteboardListService);


    }


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


    @Provides
    @Singleton
    CallControlService provideCallControlService(LocusService locusService, final MediaEngine mediaEngine, CallMetricsReporter callMetricsReporter,
                                                 EventBus bus, Context context, TrackingIdGenerator trackingIdGenerator,
                                                 DeviceRegistration deviceRegistration, LogFilePrint logFilePrint, Gson gson,
                                                 UploadLogsService uploadLogsService, CallNotification callNotification, LocusDataCache locusDataCache,
                                                 Settings settings, Provider<Batch> batchProvider, Ln.Context lnContext, com.cisco.spark.android.callcontrol.CallUi callUi,
                                                 LinusReachabilityService
                                                         linusReachabilityService, SdkClient sdkClient, CallAnalyzerReporter callAnalyzerReporter, Toaster toaster, CoreFeatures coreFeatures, LocusMeetingInfoProvider locusMeetingInfoProvider) {
        return new CallControlService(locusService, mediaEngine, callMetricsReporter, bus, context, trackingIdGenerator, deviceRegistration,
                logFilePrint, gson, uploadLogsService, callNotification, locusDataCache, settings, batchProvider,
                lnContext, callUi, linusReachabilityService, sdkClient,callAnalyzerReporter, toaster, coreFeatures, locusMeetingInfoProvider);
    }


    @Provides
    @Singleton
    RoomService provideRoomService() {
        return new RoomServiceImpl();
    }


    @Provides
    @Singleton
    CallNotification provideCallNotification(LocusDataCache calls) {
        return new VideoTestCallNotification(calls);
    }


    @Provides
    @Singleton
    CallUi provideCalUi() {
        return new VideoTestCallUI();
    }

    @Provides
    @Singleton
    SdkClient provideSdkClient() {
        return new SparkAndroid();
    }


    @Provides
    @Singleton
    public ActivityListener provideActivityListener(EventBus bus) {
        ActivityListener activityListener = new ActivityListener(bus);
        return activityListener;
    }


    @Provides
    @Singleton
    public RefWatcher provideRefWatcher() {
        return refWatcher;
    }


    @Provides
    public OkHttpClient.Builder provideOkHttpClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        return client;
    }


    @Provides
    @Singleton
    WhiteboardService provideWhiteboardService(ApiClientProvider apiClientProvider, Gson gson, EventBus eventBus,
                                               ApiTokenProvider apiTokenProvider, KeyManager keyManager,
                                               OperationQueue operationQueue, DeviceRegistration deviceRegistration,
                                               LocusDataCache locusDataCache, Settings settings, UserAgentProvider userAgentProvider,
                                               TrackingIdGenerator trackingIdGenerator, ActivityListener activityListener,
                                               Ln.Context lnContext, CallControlService callControlService, Context context,
                                               Injector injector, EncryptedConversationProcessor encryptedConversationProcessor,
                                               SdkClient sdkClient, ContentManager contentManager, SchedulerProvider schedulerProvider,
                                               LocusService locusService, AuthenticatedUserProvider authenticatedUserProvider,
                                               WhiteboardCache whiteboardCache, CoreFeatures coreFeatures, BitmapProvider bitmapProvider,
                                               FileLoader fileLoader, Clock clock, MetricsReporter metricsReporter,
                                               ContentResolver contentResolver, MediaEngine mediaEngine, Sanitizer sanitizer) {
        return new WhiteboardService(whiteboardCache, apiClientProvider, gson, eventBus, apiTokenProvider, keyManager, operationQueue,
                deviceRegistration, locusDataCache, settings, userAgentProvider, trackingIdGenerator,
                activityListener, lnContext, callControlService, sanitizer, context, injector,
                encryptedConversationProcessor, sdkClient, contentManager, locusService, schedulerProvider,
                bitmapProvider, fileLoader, mediaEngine, authenticatedUserProvider, coreFeatures, clock, metricsReporter, contentResolver);
    }

    @Provides
    @Singleton
    LocationManager provideLocationManager() {
        return new VideoTestLocationManager();
    }


    @Provides
    @Singleton
    AccountUi provideAccountUi() {
        return new VideoTestAccountUi();
    }



    @Provides
    @Singleton
    VideoMultitaskComponent provideVideoMultitasking() {
        return new VideoMultitaskComponentImpl();
    }


    @Provides
    @Singleton
    Gcm provideGcm() {
        return new VideoTestGcm();
    }

    @Provides
    @Singleton
    WhiteboardListService provideWhiteboardListService(EventBus bus, Lazy<WhiteboardService> whiteboardService, KeyManager keyManager,
                                                       SchedulerProvider schedulerProvider, ApiClientProvider apiClientProvider,
                                                       EncryptedConversationProcessor conversationProcessor, Injector injector, Context context, MetricsReporter metricsReporter) {
        return new WhiteboardListService(bus, whiteboardService, keyManager, schedulerProvider, apiClientProvider,
                conversationProcessor, injector, context, metricsReporter);
    }

    @Provides
    @Singleton
    CoreFeatures provideCoreFeatures(DeviceRegistration deviceRegistration) {
        return new AppFeatures(deviceRegistration);
    }

}