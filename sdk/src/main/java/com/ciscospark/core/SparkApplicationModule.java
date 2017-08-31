package com.ciscospark.core;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.app.NotificationManager;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.CallHistoryService;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.cisco.spark.android.callcontrol.CallNotification;
import com.cisco.spark.android.callcontrol.CallOptions;
import com.cisco.spark.android.callcontrol.CallUi;
import com.cisco.spark.android.callcontrol.NotificationActions;
import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.core.AccessManager;
import com.cisco.spark.android.core.AccountUi;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.AvatarProvider;
import com.cisco.spark.android.core.BackgroundCheck;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.PermissionsHelper;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.core.StatusManager;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.log.UploadLogsService;
import com.cisco.spark.android.lyra.LyraService;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSessionEngine;
import com.cisco.spark.android.media.MockMediaEngine;
import com.cisco.spark.android.meetings.GetMeetingInfoType;
import com.cisco.spark.android.meetings.LocusMeetingInfoProvider;
import com.cisco.spark.android.meetings.ScheduledMeetingsService;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.metrics.CallAnalyzerReporter;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.notification.Gcm;
import com.cisco.spark.android.notification.SnoozeStore;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.reachability.UIServiceAvailability;
import com.cisco.spark.android.room.ProximityBackend;
import com.cisco.spark.android.room.ProximityDetector;
import com.cisco.spark.android.room.RoomCallController;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;
import com.cisco.spark.android.room.model.RoomState;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sdk.SparkAndroid;
import com.cisco.spark.android.sproximity.SProximityPairingCallback;
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
import com.ciscospark.Spark;
import com.ciscospark.phone.Phone;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.squareup.leakcanary.RefWatcher;

import java.util.List;

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
        includes = {
                //ActivityProcessorModule.class
        },
        injects = {
                SparkApplicationDelegate.class, Spark.class, Phone.class, com.ciscospark.phone.Call.class
        }
)
class SparkApplicationModule {
    private RefWatcher refWatcher;

    public SparkApplicationModule() {
    }

    @Provides
    public OkHttpClient.Builder provideOkHttpClientBuilder() {
        return new OkHttpClient.Builder();
    }

    @Provides
    @Singleton
    public RefWatcher provideRefWatcher() {
        return refWatcher;
    }


    @Provides
    @Singleton
    ApplicationController provideApplicationController(final Context context, final ApiClientProvider clientProvider,
                                                       final ApiTokenProvider tokenProvider,
                                                       final AuthenticatedUserProvider userProvider, final EventBus bus,
                                                       final DeviceRegistration deviceRegistration,
                                                       final BackgroundCheck backgroundCheck, final Settings settings,
                                                       final MediaEngine mediaEngine,
                                                       final ActorRecordProvider actorRecordProvider, final MetricsReporter metricsReporter,
                                                       final StatusManager statusManager, final LocationManager locationManager,
                                                       MercuryClient mercuryClient, final SearchManager searchManager,
                                                       final LocusService locusService, final RoomService roomService,
                                                       final CallHistoryService callHistoryService,
                                                       final CpuLogger cpuLogger, final ConversationSyncQueue conversationSyncQueue,
                                                       NotificationManager notificationManager, AccessManager accessManager,
                                                       final KeyManager keyManager, UIServiceAvailability uiServiceAvailability,
                                                       OperationQueue operationQueue, Injector injector,
                                                       final VideoMultitaskComponent videoMultitaskComponent, final Ln.Context lnContext,
                                                       final com.cisco.spark.android.core.AccountUi accountUi, final LogFilePrint log,
                                                       final LinusReachabilityService linusReachabilityService,
                                                       final WhiteboardService whiteboardService, final LyraService lyraService,
                                                       final UrlProvider urlProvider, final SdkClient sdkClient, final WhiteboardCache whiteboardCache, final VoicemailService voicemailService,
                                                       final ScheduledMeetingsService scheduledMeetingsService,
                                                       final WhiteboardListService whiteboardListService) {
        return new ApplicationController(context, clientProvider, tokenProvider, userProvider, bus, deviceRegistration, backgroundCheck, settings,
                mediaEngine, actorRecordProvider, metricsReporter, statusManager, locationManager, mercuryClient,
                searchManager, locusService, callHistoryService, cpuLogger, conversationSyncQueue, notificationManager,
                accessManager, keyManager, uiServiceAvailability, operationQueue, injector, videoMultitaskComponent,
                lnContext, accountUi, log, linusReachabilityService, whiteboardService, lyraService, urlProvider,
                sdkClient, whiteboardCache, voicemailService, scheduledMeetingsService, whiteboardListService);
    }

    @Provides
    @Singleton
    CallNotification provideCallNotification(Context context, LocusDataCache locusDataCache, EventBus eventBus,
                                             BackgroundCheck backgroundCheck,
                                             CallOptions options, CallAnalyzerReporter callAnalyzerReporter) {
        return new CallNotification() {

            @Override
            public void notify(LocusKey locusKey, NotificationActions notificationActions) {

            }

            @Override
            public void dismiss(LocusKey locusKey) {

            }

            @Override
            public int getTimeout() {
                return 30;
            }
        };
    }


    @Provides
    @Singleton
    Gcm provideGcm(Context context, SharedPreferences preferences) {
        return new Gcm() {
            @Override
            public String register() {
                return null;
            }

            @Override
            public boolean checkAvailability(Activity activity) {
                return false;
            }

            @Override
            public void clear() {

            }
        };
    }

    @Provides
    SnoozeStore provideSnoozeStore(Settings settings) {
        return settings;
    }

    @Provides
    @Singleton
    VideoMultitaskComponent provideVideoMultitasking(Context context, LocusDataCache locusDataCache, EventBus bus,
                                                     CallControlService callControlService, MediaEngine mediaEngine,
                                                     AvatarProvider avatarProvider, StatusManager statusManager,
                                                     RoomService roomService, DeviceRegistration deviceRegistration,
                                                     Toaster toaster) {
        return new VideoMultitaskComponent() {
            @Override
            public void setApplicationController(ApplicationController controller) {
            }

            @Override
            public void transitionToFullscreen() {
            }

            @Override
            public boolean shouldStart() {
                return false;
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void startOverlay() {
            }

            @Override
            public void show(VideoMode videoMode, LocusKey locusKey) {
            }

            @Override
            public void hide() {
            }

            @Override
            public void endCall(LocusKey locusKey) {
            }

            @Override
            public void setScreenSharing(boolean isScreenSharing) {
            }

            @Override
            public void setMultitaskingMode(boolean multitasking) {
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public void bringOverlayToFront() {
            }

            @Override
            public void subdueOverlay() {
            }
        };
    }

    @Provides
    @Singleton
    CallUi provideCalUi(Context context, Settings settings, RoomService roomService,
                        UploadLogsService uploadLogsService, PermissionsHelper permissionsHelper, Toaster toaster) {
        return new CallUi() {

            @Override
            public void requestCallPermissions(CallContext callContext) {

            }

            @Override
            public void showInCallUi(CallContext callContext, boolean b) {

            }

            @Override
            public void showMeetingLandingUi(String s, GetMeetingInfoType getMeetingInfoType, CallInitiationOrigin callInitiationOrigin) {

            }

            @Override
            public void showMeetingLobbyUi(LocusKey locusKey) {

            }

            @Override
            public void dismissRingback(Call call) {

            }

            @Override
            public void startRingback(Call call) {

            }

            @Override
            public int getRingbackTimeout() {
                return 30;
            }

            @Override
            public void requestUserToUploadLogs(Call call) {

            }

            @Override
            public void reportIceFailure(Call call) {

            }

            @Override
            public void showMessage(@StringRes int i) {

            }
        };
    }

    @Provides
    @Singleton
    AccountUi provideAccountUi(NotificationManager notificationManager, BackgroundCheck backgroundCheck) {
        return new AccountUi() {
            @Override
            public void showHome(Activity activity) {

            }

            @Override
            public void logout(Context context, Activity activity, boolean b, boolean b1) {

            }
        };
    }

    @Provides
    @Singleton
    LocationManager provideLocationManager(final Context context, final Settings settings, EventBus bus, DeviceRegistration deviceRegistration) {
        return new LocationManager() {
            @Override
            public String getCoarseLocationName() {
                return null;
            }

            @Override
            public String getCoarseLocationISO6709Position() {
                return null;
            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void setApplicationController(ApplicationController applicationController) {

            }

            @Override
            public void clearLocationCache() {

            }

            @Override
            public boolean shouldStart() {
                return false;
            }

            @Override
            public void start() {

            }

            @Override
            public void stop() {

            }
        };
    }

    @Provides
    @Singleton
    RoomService provideRoomService(EventBus bus,
                                   Context context,
                                   ApiClientProvider apiClientProvider,
                                   DeviceRegistration deviceRegistration,
                                   ProximityDetector proximityDetector,
                                   ProximityBackend proximityBackend,
                                   MetricsReporter metricsReporter,
                                   android.app.Application application) {
        return new RoomService() {
            @Override
            public void setApplicationController(ApplicationController applicationController) {

            }

            @Override
            public void startRoomService(boolean b) {

            }

            @Override
            public void startStopRoomService(boolean b) {

            }

            @Override
            public String getRoomName() {
                return null;
            }

            @Override
            public boolean isInRoom() {
                return false;
            }

            @Override
            public boolean isPairedRoomAlreadyInThisLocus(@Nullable LocusData locusData) {
                return false;
            }

            @Override
            public LocusParticipant pickFirstRemoteJoinedParticipant(List<LocusParticipant> list, LocusSelfRepresentation locusSelfRepresentation) {
                return null;
            }

            @Override
            public boolean isRoomAvailable() {
                return false;
            }

            @Override
            public RoomState getRoomState() {
                return null;
            }

            @Override
            public boolean isStarted() {
                return false;
            }

            @Override
            public void setUseRoomsForMedia(boolean b) {

            }

            @Override
            public boolean useRoomsForMedia() {
                return false;
            }

            @Override
            public void leaveRoom() {

            }

            @Override
            public boolean canHasJoinWithRoom(LocusData locusData) {
                return false;
            }

            @Override
            public boolean wasRoomCallConnected() {
                return false;
            }

            @Override
            public RoomCallController getCallController() {
                return new RoomCallController();
            }

            @Override
            public void uploadRoomLogs(String s) {

            }

            @Override
            public void uploadRoomLogs(LocusKey locusKey, String s) {

            }

            @Override
            public void announceProximityWithToken(String s) {

            }

            @Override
            public void stopLocalDetectorHandoffToMediaEngine(boolean b) {

            }

            @Override
            public String getRoomStatusString(String s, String s1, String s2) {
                return null;
            }

            @Override
            public boolean shouldStart() {
                return false;
            }

            @Override
            public void start() {

            }

            @Override
            public void stop() {

            }

            @Override
            public void newTokenWithCallback(String s, UltrasoundMetrics ultrasoundMetrics, boolean b, SProximityPairingCallback sProximityPairingCallback) {

            }

            @Override
            public void newToken(String s, UltrasoundMetrics ultrasoundMetrics, boolean b) {

            }
        };
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
    WhiteboardService provideWhiteboardService(ApiClientProvider apiClientProvider, Gson gson, EventBus eventBus,
                                               ApiTokenProvider apiTokenProvider, KeyManager keyManager,
                                               OperationQueue operationQueue, DeviceRegistration deviceRegistration,
                                               LocusDataCache locusDataCache, Settings settings, UserAgentProvider userAgentProvider,
                                               TrackingIdGenerator trackingIdGenerator, ActivityListener activityListener,
                                               Ln.Context lnContext, CallControlService callControlService, Context context,
                                               Injector injector, EncryptedConversationProcessor conversationProcessor,
                                               SdkClient sdkClient, ContentManager contentManager, SchedulerProvider schedulerProvider,
                                               LocusService locusService, AuthenticatedUserProvider authenticatedUserProvider,
                                               WhiteboardCache whiteboardCache,
                                               CoreFeatures coreFeatures, BitmapProvider bitmapProvider, FileLoader fileLoader, Clock clock, MetricsReporter metricsReporter,
                                               ContentResolver contentResolver, MediaEngine mediaEngine, Sanitizer sanitizer) {
        return new WhiteboardService(whiteboardCache, apiClientProvider, gson, eventBus, apiTokenProvider, keyManager, operationQueue,
                deviceRegistration, locusDataCache, settings, userAgentProvider, trackingIdGenerator,
                activityListener, lnContext, callControlService, sanitizer, context, injector, conversationProcessor, sdkClient,
                contentManager, locusService, schedulerProvider,
                bitmapProvider, fileLoader, mediaEngine, authenticatedUserProvider, coreFeatures, clock, metricsReporter,
                contentResolver);
    }

    @Provides
    @Singleton
    ActivityListener provideActivityListener(EventBus bus) {
        return new ActivityListener(bus);
    }

    @Provides
    @Singleton
    WhiteboardListService provideWhiteboardListService(EventBus bus, Lazy<WhiteboardService> whiteboardService, KeyManager keyManager,
                                                       SchedulerProvider schedulerProvider, ApiClientProvider apiClientProvider,
                                                       EncryptedConversationProcessor conversationProcessor, Injector injector, Context context, MetricsReporter metricsReporter) {
        return new WhiteboardListService(bus, whiteboardService, keyManager, schedulerProvider, apiClientProvider,
                conversationProcessor, injector, context, metricsReporter);
    }

    public void setRefWatcher(RefWatcher refWatcher) {
        this.refWatcher = refWatcher;
    }

    @Provides
    @Singleton
    CallControlService provideCallControlService(LocusService locusService, final MediaEngine mediaEngine,
                                                 CallMetricsReporter callMetricsReporter, EventBus bus, Context context,
                                                 TrackingIdGenerator trackingIdGenerator,
                                                 DeviceRegistration deviceRegistration, LogFilePrint logFilePrint,
                                                 Gson gson,
                                                 UploadLogsService uploadLogsService, CallNotification callNotification,
                                                 LocusDataCache locusDataCache,
                                                 Settings settings,
                                                 Provider<Batch> batchProvider, Ln.Context lnContext, com.cisco.spark.android.callcontrol.CallUi callUi,
                                                 LinusReachabilityService linusReachabilityService, SdkClient sdkClient,
                                                 CallAnalyzerReporter callAnalyzerReporter, Toaster toaster, CoreFeatures coreFeatures, LocusMeetingInfoProvider locusMeetingInfoProvider) {
        return new CallControlService(locusService, mediaEngine, callMetricsReporter,
                bus, context,
                trackingIdGenerator,
                deviceRegistration, logFilePrint, gson,
                uploadLogsService, callNotification, locusDataCache,
                settings,
                batchProvider, lnContext, callUi,
                linusReachabilityService, sdkClient, callAnalyzerReporter,
                toaster, coreFeatures, locusMeetingInfoProvider);
    }

    @Provides
    @Singleton
    CoreFeatures provideCoreFeatures(DeviceRegistration deviceRegistration) {
        return new AppFeatures(deviceRegistration);
    }

    @Provides
    @Singleton
    SdkClient provideSdkClient() {
        return new SparkAndroid();
    }

}
