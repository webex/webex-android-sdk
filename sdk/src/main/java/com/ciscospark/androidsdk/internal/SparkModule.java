/*
 * Copyright 2016-2017 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscospark.androidsdk.internal;

import java.util.List;
import javax.inject.Provider;
import javax.inject.Singleton;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
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
import com.cisco.spark.android.core.BackgroundCheck;
import com.cisco.spark.android.core.Component;
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
import com.cisco.spark.android.mercury.MercuryProvider;
import com.cisco.spark.android.mercury.events.WhiteboardMercuryClient;
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
import com.cisco.spark.android.sproximity.SProximityPairingCallback;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.ui.call.VideoMultitaskComponent;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.CpuLogger;
import com.cisco.spark.android.util.LinusReachabilityService;
import com.cisco.spark.android.util.Sanitizer;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.Toaster;
import com.cisco.spark.android.voicemail.VoicemailService;
import com.cisco.spark.android.wdm.DeviceInfo;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscospark.androidsdk.Spark;
import com.ciscospark.androidsdk.auth.JWTAuthenticator;
import com.ciscospark.androidsdk.auth.OAuthAuthenticator;
import com.ciscospark.androidsdk.auth.OAuthWebViewAuthenticator;
import com.ciscospark.androidsdk.phone.internal.PhoneImpl;
import com.ciscospark.androidsdk.utils.http.DefaultHeadersInterceptor;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.squareup.leakcanary.RefWatcher;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import org.greenrobot.eventbus.EventBus;

@Module(
        complete = false,
        library = true,
        includes = {
                //ActivityProcessorModule.class
        },
        injects = {
                SparkInjector.class,
                Spark.class,
                PhoneImpl.class,
                Call.class,
                JWTAuthenticator.class,
                OAuthAuthenticator.class,
                OAuthWebViewAuthenticator.class
        }
)
class SparkModule {

    private RefWatcher refWatcher;

    public SparkModule() {
    }

    @Provides
    public OkHttpClient.Builder provideOkHttpClientBuilder() {
        OkHttpClient.Builder ret = new OkHttpClient.Builder();
        ret.addInterceptor(new DefaultHeadersInterceptor());
        return ret;
    }

    @Provides
    @Singleton
    public RefWatcher provideRefWatcher() {
        return refWatcher;
    }

    @Provides
    @Singleton
    public ApplicationController provideApplicationController(final Context context, final ApiClientProvider clientProvider,
                                                       final ApiTokenProvider tokenProvider,
                                                       final AuthenticatedUserProvider userProvider, final EventBus bus,
                                                       final DeviceRegistration deviceRegistration, CoreFeatures coreFeatures,
                                                       final BackgroundCheck backgroundCheck, final Settings settings,
                                                       final MediaEngine mediaEngine,
                                                       final ActorRecordProvider actorRecordProvider, final MetricsReporter metricsReporter,
                                                       final StatusManager statusManager,
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
                                                       final LyraService lyraService,
                                                       final UrlProvider urlProvider, final SdkClient sdkClient, final VoicemailService voicemailService,
                                                       final ScheduledMeetingsService scheduledMeetingsService) {
        return new ApplicationController(context, clientProvider, tokenProvider, userProvider, bus, deviceRegistration, coreFeatures, 
            backgroundCheck, settings, mediaEngine, actorRecordProvider, metricsReporter, statusManager, mercuryClient, 
            searchManager, locusService, callHistoryService, cpuLogger, conversationSyncQueue, notificationManager, accessManager, 
            keyManager, uiServiceAvailability, operationQueue, injector, videoMultitaskComponent, lnContext, accountUi, log, 
            linusReachabilityService, lyraService, urlProvider, sdkClient, voicemailService, scheduledMeetingsService);
    }

    @Provides
    @Singleton
    public CallNotification provideCallNotification(Context context, LocusDataCache locusDataCache, EventBus eventBus,
                                             BackgroundCheck backgroundCheck,
                                             CallOptions options, CallAnalyzerReporter callAnalyzerReporter, AppFeatures appFeatures) {
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
    public Gcm provideGcm(Context context, SharedPreferences preferences) {
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
    public SnoozeStore provideSnoozeStore(Settings settings) {
        return settings;
    }

    @Provides
    @Singleton
    VideoMultitaskComponent provideVideoMultitasking(Context context, LocusDataCache locusDataCache, EventBus bus,
                                                     CallControlService callControlService, StatusManager statusManager,
                                                     RoomService roomService, DeviceRegistration deviceRegistration, AppFeatures appFeatures,
                                                     Toaster toaster) {
        return new VideoMultitaskComponent() {
            @Override
            public void setApplicationController(ApplicationController applicationController) {
                
            }

            @Override
            public void transitionToFullscreen() {

            }

            @Override
            public void setScreenSharingFromThisDevice(boolean b) {

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
            public void setMultitaskingMode(boolean b) {

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

            @Override
            public VideoMode getInCallVideoMode(LocusData locusData) {
                return null;
            }

            @Override
            public VideoMode getInCallWithRoomVideoMode(LocusData locusData) {
                return null;
            }
        };
    }

    @Provides
    @Singleton
    public CallUi provideCalUi(Context context, Settings settings, RoomService roomService,
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
                return 3600 * 24 * 365 ;
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
    public AccountUi provideAccountUi(NotificationManager notificationManager, BackgroundCheck backgroundCheck) {
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
    public RoomService provideRoomService(EventBus bus, Context context, ApiClientProvider apiClientProvider, 
                                          DeviceRegistration deviceRegistration, AppFeatures appFeatures, 
                                          ProximityDetector proximityDetector, ProximityBackend proximityBackend, 
                                          MetricsReporter metricsReporter, android.app.Application application) {
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
            public String getRoomStatusString(String s, String s1) {
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
    MediaEngine provideMediaEngine(LogFilePrint log, EventBus bus, Settings settings, Context context, DeviceRegistration deviceRegistration, Gson gson, Ln.Context lnContext, CoreFeatures coreFeatures) {
        if (Build.FINGERPRINT.contains("generic")) {
            // If running on an emulator, allow using the MockMediaEngine.
            // TODO: See if we are able to run wme on x86 emulators, didn't work right out of the bat.
            return new MockMediaEngine(lnContext);
        } else {
            return new MediaSessionEngine(log, bus, settings, context, BuildConfig.GIT_COMMIT_SHA, deviceRegistration, gson, lnContext, coreFeatures);
        }
    }
	
    @Provides
    @Singleton
    public ActivityListener provideActivityListener(EventBus bus) {
        return new ActivityListener(bus);
    }

    @Provides
    @Singleton
    MercuryProvider provideMercuryProdiver(ApiClientProvider apiClientProvider, Gson gson, EventBus bus,
                                           OperationQueue operationQueue, DeviceRegistration deviceRegistration,
                                           CoreFeatures coreFeatures, ActivityListener activityListener, Ln.Context lnContext,
                                           SchedulerProvider schedulerProvider, Clock clock, Sanitizer sanitizer) {
        return new MercuryProvider() {
            @Override
            public MercuryClient buildWhiteboardMercuryClient() {
                return new WhiteboardMercuryClient(apiClientProvider, gson, bus, deviceRegistration, coreFeatures, activityListener,
                    lnContext, operationQueue, sanitizer, schedulerProvider, clock);
            }
        };
    }
    
    public void setRefWatcher(RefWatcher refWatcher) {
        this.refWatcher = refWatcher;
    }

    @Provides
    @Singleton
    public CallControlService provideCallControlService(LocusService locusService, final MediaEngine mediaEngine,
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
        return new CallControlService(locusService, mediaEngine, callMetricsReporter, bus, context, 
            trackingIdGenerator, deviceRegistration, logFilePrint, gson, uploadLogsService, callNotification,
            locusDataCache, settings, batchProvider, lnContext, callUi, linusReachabilityService, sdkClient, 
            callAnalyzerReporter, toaster, coreFeatures);
    }

    @Provides
    @Singleton
    CoreFeatures provideCoreFeatures(DeviceRegistration deviceRegistration) {
        return new AppFeatures(deviceRegistration);
    }

    @Provides
    @Singleton
    AppFeatures provideAppFeatures(CoreFeatures coreFeatures) {
        return (AppFeatures) coreFeatures;
    }

    @Provides
    @Singleton
    SdkClient provideSdkClient() {
        return new SdkClient() {
	        @Override
	        public String getDeviceType() {
		        return DeviceInfo.ANDROID_DEVICE_TYPE;
	        }

	        @Override
	        public boolean toastsEnabled() {
		        return false;
	        }

	        @Override
	        public boolean supportsHybridKms() {
		        return false;
	        }

	        @Override
	        public boolean supportsVoicemailScopes() {
		        return false;
	        }

	        @Override
	        public boolean operationEnabled(Operation operation) {
		        return true;
	        }

	        @Override
	        public boolean componentEnabled(Component component) {
		        return true;
	        }

	        @Override
	        public boolean conversationCachingEnabled() {
		        return false;
	        }

	        @Override
	        public boolean supportsPrivateBoards() {
		        return false;
	        }

	        @Override
	        public boolean shouldClearRemoteBoardStore() {
		        return false;
	        }

	        @Override
	        public boolean isMobileDevice() {
		        return true;
	        }

	        @NonNull
	        @Override
	        public String generateClientInfo() {
		        return "";
	        }

            @Override
            public boolean isDebugBuild() {
                return false;
            }

            @Override
            public boolean isTestBuild() {
                return false;
            }
        };
    }
}
