package com.cisco.spark.android.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.os.RemoteException;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.app.ActivityManager;
import com.cisco.spark.android.app.AudioManager;
import com.cisco.spark.android.app.PowerManager;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.callcontrol.CallPhoneStateReceiver;
import com.cisco.spark.android.client.SquaredUrlProvider;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.contacts.ContactsContractManager;
import com.cisco.spark.android.contacts.ViewContactNotifyService;
import com.cisco.spark.android.content.ContentLoader;
import com.cisco.spark.android.flag.FlagOperation;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.service.LocusProcessor;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.log.UploadLogsService;
import com.cisco.spark.android.lyra.BindingBackend;
import com.cisco.spark.android.lyra.CloudBindingBackend;
import com.cisco.spark.android.lyra.LyraService;
import com.cisco.spark.android.media.BluetoothBroadcastReceiver;
import com.cisco.spark.android.media.HeadsetIntentReceiver;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.meetings.LocusMeetingInfoProvider;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.metrics.MetricsEnvironment;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.model.Json;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.notification.Gcm;
import com.cisco.spark.android.presence.PresenceStatusListener;
import com.cisco.spark.android.presence.operation.FetchPresenceStatusOperation;
import com.cisco.spark.android.presence.operation.SendPresenceEventOperation;
import com.cisco.spark.android.presence.operation.SubscribePresenceStatusOperation;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.provisioning.ProvisioningClientProvider;
import com.cisco.spark.android.reachability.ConnectivityChangeReceiver;
import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.reachability.UIServiceAvailability;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContentProvider;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.TitleBuilder;
import com.cisco.spark.android.sync.operationqueue.ActivityFillOperation;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.AddPersonOperation;
import com.cisco.spark.android.sync.operationqueue.AliasPreloginMetricsUserIdOperation;
import com.cisco.spark.android.sync.operationqueue.AssignRoomAvatarOperation;
import com.cisco.spark.android.sync.operationqueue.AudioMuteOperation;
import com.cisco.spark.android.sync.operationqueue.AudioVolumeOperation;
import com.cisco.spark.android.sync.operationqueue.AvatarUpdateOperation;
import com.cisco.spark.android.sync.operationqueue.CatchUpSyncOperation;
import com.cisco.spark.android.sync.operationqueue.ContentUploadOperation;
import com.cisco.spark.android.sync.operationqueue.CreateKmsResourceOperation;
import com.cisco.spark.android.sync.operationqueue.CustomNotificationsTagOperation;
import com.cisco.spark.android.sync.operationqueue.DeleteActivityOperation;
import com.cisco.spark.android.sync.operationqueue.FeatureToggleOperation;
import com.cisco.spark.android.sync.operationqueue.FetchActivityContextOperation;
import com.cisco.spark.android.sync.operationqueue.FetchMentionsOperation;
import com.cisco.spark.android.sync.operationqueue.FetchSpaceUrlOperation;
import com.cisco.spark.android.sync.operationqueue.FetchStickyPackOperation;
import com.cisco.spark.android.sync.operationqueue.FetchUnjoinedTeamRoomsOperation;
import com.cisco.spark.android.sync.operationqueue.GetAvatarUrlsOperation;
import com.cisco.spark.android.sync.operationqueue.GetRetentionPolicyInfoOperation;
import com.cisco.spark.android.sync.operationqueue.IncrementShareCountOperation;
import com.cisco.spark.android.sync.operationqueue.IntegrateContactsOperation;
import com.cisco.spark.android.sync.operationqueue.JoinTeamRoomOperation;
import com.cisco.spark.android.sync.operationqueue.KeyFetchOperation;
import com.cisco.spark.android.sync.operationqueue.MapEventToConversationOperation;
import com.cisco.spark.android.sync.operationqueue.MarkReadOperation;
import com.cisco.spark.android.sync.operationqueue.MoveRoomToTeamOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationWithRepostedMessagesOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostGenericMetricOperation;
import com.cisco.spark.android.sync.operationqueue.PostKmsMessageOperation;
import com.cisco.spark.android.sync.operationqueue.PostStickyActivityOperation;
import com.cisco.spark.android.sync.operationqueue.RemoteSearchOperation;
import com.cisco.spark.android.sync.operationqueue.RemoveParticipantOperation;
import com.cisco.spark.android.sync.operationqueue.RemoveRoomAvatarOperation;
import com.cisco.spark.android.sync.operationqueue.ScheduledEventActivityOperation;
import com.cisco.spark.android.sync.operationqueue.SendDtmfOperation;
import com.cisco.spark.android.sync.operationqueue.SetTitleAndSummaryOperation;
import com.cisco.spark.android.sync.operationqueue.SetupSharedKeyWithKmsOperation;
import com.cisco.spark.android.sync.operationqueue.TagOperation;
import com.cisco.spark.android.sync.operationqueue.ToggleActivityOperation;
import com.cisco.spark.android.sync.operationqueue.ToggleParticipantActivityOperation;
import com.cisco.spark.android.sync.operationqueue.TokenRefreshOperation;
import com.cisco.spark.android.sync.operationqueue.TokenRevokeOperation;
import com.cisco.spark.android.sync.operationqueue.UnboundKeyFetchOperation;
import com.cisco.spark.android.sync.operationqueue.UnsetFeatureToggleOperation;
import com.cisco.spark.android.sync.operationqueue.UpdateEncryptionKeyOperation;
import com.cisco.spark.android.sync.operationqueue.UpdateTeamColorOperation;
import com.cisco.spark.android.sync.operationqueue.VideoThumbnailOperation;
import com.cisco.spark.android.sync.operationqueue.core.FetchAcksOperation;
import com.cisco.spark.android.sync.operationqueue.core.FetchParticipantsOperation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.queue.AbstractConversationSyncTask;
import com.cisco.spark.android.sync.queue.ActivitySyncQueue;
import com.cisco.spark.android.sync.queue.ActivitySyncTask;
import com.cisco.spark.android.sync.queue.BulkActivitySyncTask;
import com.cisco.spark.android.sync.queue.CatchUpSyncTask;
import com.cisco.spark.android.sync.queue.ConversationBackFillTask;
import com.cisco.spark.android.sync.queue.ConversationForwardFillTask;
import com.cisco.spark.android.sync.queue.ConversationFrontFillTask;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.sync.queue.FetchActivityContextTask;
import com.cisco.spark.android.sync.queue.IncrementalSyncTask;
import com.cisco.spark.android.sync.queue.KeyPushEventTask;
import com.cisco.spark.android.sync.queue.MentionsTask;
import com.cisco.spark.android.sync.queue.NewActivityTask;
import com.cisco.spark.android.sync.queue.PushSyncTask;
import com.cisco.spark.android.sync.queue.ShellsTask;
import com.cisco.spark.android.sync.queue.SyncFlagsOperation;
import com.cisco.spark.android.sync.queue.SyncTaskOperation;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.DiagnosticManager;
import com.cisco.spark.android.util.LinusReachabilityService;
import com.cisco.spark.android.util.ProximitySensor;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.SystemClock;
import com.cisco.spark.android.util.TestUtils;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceInfo;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.RegisterDeviceOperation;
import com.cisco.spark.android.wdm.UCDeviceType;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.LoadWhiteboardContentTask;
import com.cisco.spark.android.whiteboard.persistence.RemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.WhiteboardListCache;
import com.cisco.spark.android.whiteboard.snapshot.SnapshotUploadOperation;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusBuilder;
import okhttp3.OkHttpClient;

import static android.content.Context.MODE_PRIVATE;

/**
 * Module for all the application dependencies.
 */
@Module(
        complete = false,
        library = true,
        injects = {
                ApplicationDelegate.class,
                ConversationContentProvider.class,
                AuthenticatedUserTask.class,
                LocusService.class,
                LocusDataCache.class,
                SquaredBackgroundCheck.class,
                ConnectivityChangeReceiver.class,
                HeadsetIntentReceiver.class,
                MercuryClient.class,
                ConversationSyncQueue.class,
                BitmapProvider.class,
                ContentManager.class,
                DiagnosticManager.class,
                ProximitySensor.class,
                OperationQueue.class,
                Operation.class,
                AvatarUpdateOperation.class,
                KeyFetchOperation.class,
                ActivityOperation.class,
                PostCommentOperation.class,
                NewConversationOperation.class,
                Batch.class,
                PostContentActivityOperation.class,
                EncryptionDurationMetricManager.class,
                AbstractConversationSyncTask.class,
                IncrementalSyncTask.class,
                ShellsTask.class,
                PushSyncTask.class,
                ConversationFrontFillTask.class,
                ConversationBackFillTask.class,
                KeyPushEventTask.class,
                SetTitleAndSummaryOperation.class,
                MarkReadOperation.class,
                ToggleActivityOperation.class,
                ConversationResolver.class,
                NewActivityTask.class,
                ScheduledEventActivityOperation.class,
                UnboundKeyFetchOperation.class,
                FeatureToggleOperation.class,
                UnsetFeatureToggleOperation.class,
                MapEventToConversationOperation.class,
                ToggleParticipantActivityOperation.class,
                TitleBuilder.class,
                SendDtmfOperation.class,
                IncrementShareCountOperation.class,
                UpdateEncryptionKeyOperation.class,
                PostKmsMessageOperation.class,
                SetupSharedKeyWithKmsOperation.class,
                AddPersonOperation.class,
                TagOperation.class,
                CustomNotificationsTagOperation.class,
                DeleteActivityOperation.class,
                CallPhoneStateReceiver.class,
                VideoThumbnailOperation.class,
                RemoveParticipantOperation.class,
                ContentUploadOperation.class,
                FetchSpaceUrlOperation.class,
                RegisterDeviceOperation.class,
                KeyManager.class,
                PostStickyActivityOperation.class,
                UIServiceAvailability.class,
                ConversationForwardFillTask.class,
                ActivityFillOperation.class,
                MentionsTask.class,
                FetchActivityContextTask.class,
                FetchActivityContextOperation.class,
                FetchMentionsOperation.class,
                FetchUnjoinedTeamRoomsOperation.class,
                CreateKmsResourceOperation.class,
                RemoteSearchOperation.class,
                ActivitySyncTask.class,
                BulkActivitySyncTask.class,
                JoinTeamRoomOperation.class,
                FetchStickyPackOperation.class,
                UpdateTeamColorOperation.class,
                FetchPresenceStatusOperation.class,
                SubscribePresenceStatusOperation.class,
                MoveRoomToTeamOperation.class,
                NewConversationWithRepostedMessagesOperation.class,
                ContactsContractManager.class,
                IntegrateContactsOperation.class,
                ViewContactNotifyService.class,
                AssignRoomAvatarOperation.class,
                RemoveRoomAvatarOperation.class,
                FlagOperation.class,
                SyncFlagsOperation.class,
                SendPresenceEventOperation.class,
                TokenRefreshOperation.class,
                TokenRevokeOperation.class,
                GetAvatarUrlsOperation.class,
                CatchUpSyncOperation.class,
                SyncTaskOperation.class,
                CatchUpSyncTask.class,
                FetchParticipantsOperation.class,
                PostGenericMetricOperation.class,
                GetRetentionPolicyInfoOperation.class,
                FetchAcksOperation.class,
                AudioMuteOperation.class,
                AudioVolumeOperation.class,
                RemoteWhiteboardStore.class,
                LocusMeetingInfoProvider.class,
                SnapshotUploadOperation.class,
                AliasPreloginMetricsUserIdOperation.class,
                LoadWhiteboardContentTask.class,
        }
)

public class BaseSquaredModule {

    public BaseSquaredModule() {
    }

    @Provides
    @Singleton
    Settings provideSettings(final Context context, final Gson gson) {
        return new Settings(context.getSharedPreferences("com.cisco.wx2.android", MODE_PRIVATE), context, gson);
    }

    @Provides
    @Singleton
    Ln.Context provideLnContext() {
        return new Ln.Context() {
            @Override
            public String get() {
                return null;
            }
        };
    }

    @Provides
    AuthenticatedUserProvider provideAuthenticatedUserProvider(ApiTokenProvider apiTokenProvider) {
        return apiTokenProvider;
    }

    @Provides
    Gson provideGson() {
        return Json.buildGson();
    }

    @Provides
    @Singleton
    UCDeviceType provideUCDeviceType() {
        return new UCDeviceType(DeviceInfo.UC_DEVICE_TYPE);
    }

    @Provides
    @Singleton
    DeviceRegistration provideDeviceRegistration(Settings settings, UrlProvider urlProvider) {
        DeviceRegistration registration = settings.getDeviceRegistration();
        registration.initialize(urlProvider);
        return registration;
    }

    @Provides
    DeviceInfo provideDeviceInfo(Gcm gcm, AccessManager accessManager) {
        return DeviceInfo.defaultConfig(gcm.register(), accessManager.getRegion());
    }

    @Provides
    Clock provideClock() {
        return new SystemClock();
    }

    @Provides
    @Singleton
    EventBus provideBus() {
        EventBusBuilder builder = EventBus.builder();

        if (BuildConfig.DEBUG) {
            builder.throwSubscriberException(true);
        }
        return builder.build();
    }

    @Provides
    @Singleton
    LocusService provideLocusService(final Context context, final EventBus bus, final DeviceRegistration deviceRegistration,
                                     final ApiClientProvider apiClientProvider, final LocusDataCache locusDataCache, final LocusProcessor locusProcessor,
                                     final TrackingIdGenerator trackingIdGenerator, final CallMetricsReporter callMetricsReporter, final AccessManager accessManager, final Gson gson,
                                     final ApiTokenProvider apiTokenProvider, final SchedulerProvider schedulerProvider, final  Lazy<EncryptedConversationProcessor> conversationProcessorLazy) {
        return new LocusService(context, bus, deviceRegistration, apiClientProvider, locusDataCache, locusProcessor, trackingIdGenerator, accessManager, callMetricsReporter, gson, apiTokenProvider, schedulerProvider, conversationProcessorLazy);
    }

    @Provides
    @Singleton
    BackgroundCheck provideBackgroundCheck(final EventBus bus, final Context context, final PowerManager powerManager, final ActivityManager activityManager, LocusDataCache locusDataCache) {
        return new SquaredBackgroundCheck(bus, context, powerManager, activityManager, locusDataCache);
    }

    @Provides
    @Singleton
    UIServiceAvailability provideUIServiceAvailability(EventBus bus, BackgroundCheck backgroundCheck,
                                                       MercuryClient mercuryClient, ConnectivityManager connectivityManager) {
        return new UIServiceAvailability(bus, backgroundCheck, mercuryClient, connectivityManager);
    }

    @Provides
    @Singleton
    ApiClientProvider provideApiClientProvider(AuthenticatedUserProvider authenticatedUserProvider,
                                               UserAgentProvider userAgentProvider,
                                               TrackingIdGenerator trackingIdGenerator,
                                               Gson gson,
                                               DeviceRegistration deviceRegistration,
                                               EventBus bus, Settings settings,
                                               Context context, Ln.Context lnContext,
                                               Provider<OkHttpClient.Builder> okHttpClientBuilderProvider,
                                               Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {
        return new ApiClientProvider(authenticatedUserProvider, userAgentProvider,
                trackingIdGenerator, gson, deviceRegistration, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, urlProvider);
    }

    @Provides
    @Singleton
    SearchManager provideSearchManager(Gson gson, ApiTokenProvider apiTokenProvider, Context context, ActorRecordProvider actorRecordProvider, DeviceRegistration deviceRegistration, EventBus bus, Provider<Batch> batchProvider) {
        return new SearchManager(gson, apiTokenProvider, context, actorRecordProvider, deviceRegistration, bus, batchProvider);
    }

    @Provides
    @Singleton
    ContentLoader provideContentLoader(Context context, MetricsReporter metricsReporter) {
        return new ContentLoader(context, metricsReporter);
    }

    @Provides
    @Singleton
    MetricsReporter provideMetricsReporter(final ApiTokenProvider apiTokenProvider,
                                           final ApiClientProvider apiClientProvider, final EventBus bus, final LocusDataCache locusDataCache) {
        MetricsEnvironment env = MetricsEnvironment.ENV_PROD;

        if (BuildConfig.DEBUG || TestUtils.isTestUser(apiTokenProvider)) {
            env = MetricsEnvironment.ENV_TEST;
        }

        return new MetricsReporter(apiClientProvider, bus, env, locusDataCache);
    }

    @Provides
    @Singleton
    ConversationSyncQueue provideConversationSyncQueue(ContentResolver contentResolver, EventBus bus, ActivitySyncQueue activitySyncQueue, Injector injector) {
        return new ConversationSyncQueue(contentResolver, bus, activitySyncQueue, injector);
    }

    @Provides
    @Singleton
    ApiTokenProvider provideApiTokenProvider(Settings settings, Gson gson, OAuth2 oAuth2, ActorRecordProvider actorRecordProvider) {
        return new ApiTokenProvider(settings, gson, oAuth2, actorRecordProvider);
    }

    @Provides
    @Singleton
    BitmapProvider provideBitmapProvider(Context context, ContentManager contentManager, EventBus eventBus, DeviceRegistration deviceRegistration) {
        return new BitmapProvider(context, contentManager, eventBus, deviceRegistration);
    }

    @Provides
    @Singleton
    ContentManager provideContentManager(Context context, ApiClientProvider apiClientProvider, ApiTokenProvider apiTokenProvider, EventBus eventBus, MetricsReporter metricsReporter, AvatarProvider avatarProvider, Provider<Batch> batchProvider, OperationQueue operationQueue) {
        return new ContentManager(context, apiClientProvider, apiTokenProvider, eventBus, metricsReporter, avatarProvider, batchProvider, operationQueue);
    }

    @Provides
    @Singleton
    ProvisioningClientProvider provisioningClientProvider(UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator,
                                                          Gson gson, EventBus bus, Settings settings,
                                                          Context context, Ln.Context lnContext,
                                                          Provider<OkHttpClient.Builder> okHttpClientBuilderProvider, Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {
        return new ProvisioningClientProvider(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, urlProvider);
    }

    @Provides
    @Singleton
    DiagnosticManager provideDiagnosticManager(EventBus eventBus, ApiClientProvider apiClientProvider) {
        return new DiagnosticManager(eventBus, apiClientProvider);
    }

    @Provides
    @Singleton
    EncryptedConversationProcessor provideEncryptedConversationProvider(Context context, KeyManager keyManager, ApiTokenProvider apiTokenProvider, EncryptionDurationMetricManager encryptionDurationMetricManager, Gson gson, ApiClientProvider apiClientProvider, DeviceRegistration deviceRegistration, Provider<Batch> batchProvider) {
        return new EncryptedConversationProcessor(context, keyManager, apiTokenProvider, encryptionDurationMetricManager, gson, apiClientProvider, deviceRegistration, batchProvider);
    }

    @Provides
    Batch provideBatch(final Context context) {
        return new Batch(context) {

            @Override
            public boolean apply() {
                if (isEmpty())
                    return true;

                try {
                    results = context.getContentResolver().applyBatch(authority, this);
                    if (results.length == size()) {
                        Ln.v("Batch operation successful. Size=" + results.length);
                        return true;
                    }

                    Ln.e("Batch operation failed on operation " + (results.length - 1) + " of " + size());
                    // verbose only because results can contain private stuff
                    Ln.v("Batch operation failed. Problem Operation: " + get(results.length));
                } catch (IllegalArgumentException e) {
                    Ln.e(e, "Failed batch operation");
                } catch (RemoteException e) {
                    Ln.e(e, "Failed batch operation");
                } catch (OperationApplicationException e) {
                    Ln.e(e, "Failed batch operation");
                }
                return false;
            }
        };
    }

    @Singleton
    @Provides
    EncryptionDurationMetricManager provideEncryptionDurationMetricManager(EventBus eventBus, MetricsReporter metricsReporter,
                                                                           ActivityListener activityListener, Ln.Context lnContext) {
        return new EncryptionDurationMetricManager(eventBus, metricsReporter, activityListener, lnContext);
    }

    @Provides
    @Singleton
    BluetoothBroadcastReceiver provideBluetoothBroadcastReceiver(AudioManager audioManager) {
        return new BluetoothBroadcastReceiver(audioManager);
    }

    @Provides
    @Singleton
    AccessManager provideAccessManager(ApiClientProvider apiClientProvider, EventBus bus) {
        return new DiscoveryAccessManager(apiClientProvider, bus);
    }

    @Provides
    ContentResolver provideContentResolver(final Context context) {
        return context.getContentResolver();
    }

    @Provides
    @Singleton
    PresenceStatusListener providesPresenceStatusCache(EventBus eventBus, ActorRecordProvider actorRecordProvider, AuthenticatedUserProvider authenticatedUserProvider, Provider<Batch> batchProvider, DeviceRegistration deviceRegistration) {
        return new PresenceStatusListener(eventBus, actorRecordProvider, authenticatedUserProvider, batchProvider, deviceRegistration);
    }

    @Provides
    @Singleton
    WhiteboardListCache providesWhiteboardListCache() {
        return new WhiteboardListCache();
    }

    @Provides
    @Singleton
    MercuryClient provideMercuryClient(ApiClientProvider apiClientProvider, ApiTokenProvider apiTokenProvider, Gson gson, EventBus bus,
                                       DeviceRegistration deviceRegistration, Settings settings, UserAgentProvider userAgentProvider,
                                       TrackingIdGenerator trackingIdGenerator, ActivityListener activityListener, Ln.Context lnContext,
                                       WhiteboardService whiteboardService) {
        MercuryClient client = new MercuryClient(true, apiClientProvider, apiTokenProvider, gson, bus, deviceRegistration,  settings,  userAgentProvider,
                trackingIdGenerator,  activityListener,  lnContext, whiteboardService);
        whiteboardService.setPrimaryMercury(client);
        return client;
    }

    @Provides
    @Singleton
    LogFilePrint provideLogFilePrint(Context context) {
        return new LogFilePrint(context);
    }

    @Provides
    @Singleton
    BindingBackend provideBindingBackend(ApiClientProvider apiClientProvider, EventBus bus) {
        return new CloudBindingBackend(apiClientProvider, bus);
    }

    @Provides
    @Singleton
    LyraService provideLyraService(ApiClientProvider apiClientProvider, EventBus eventBus,
                                   EncryptedConversationProcessor conversationProcessor, ContentResolver contentResolver,
                                   RoomService roomService, BindingBackend bindingBackend, Context context,
                                   DeviceRegistration deviceRegistration, Injector injector, ApiTokenProvider apiTokenProvider,
                                   LocusDataCache locusDataCache, Provider<Batch> batchProvider, SchedulerProvider schedulerProvider, MetricsReporter metricsReporter) {
        return new LyraService(eventBus, apiClientProvider, conversationProcessor, contentResolver, roomService, bindingBackend,
                deviceRegistration, context, injector, apiTokenProvider, locusDataCache, batchProvider, schedulerProvider, metricsReporter);
    }

    @Provides
    @Singleton
    ContactsContractManager provideContactsContractManager(Context context, AuthenticatedUserProvider authenticatedUserProvider,
                                                           Provider<Batch> batchProvider, ActorRecordProvider actorRecordProvider,
                                                           ContentResolver contentResolver, AvatarProvider avatarProvider,
                                                           ContentManager contentManager, EventBus bus) {
        return new ContactsContractManager(context, authenticatedUserProvider, batchProvider, actorRecordProvider, contentResolver,
                                           avatarProvider, contentManager, bus);

    }

    @Provides
    @Singleton
    LocusDataCache provideLocusDataCache(EventBus bus, DeviceRegistration deviceRegistration, UCDeviceType ucDeviceType,
                                         ApiTokenProvider apiTokenProvider) {
        return new LocusDataCache(bus, deviceRegistration, ucDeviceType, apiTokenProvider);
    }

    @Provides
    @Singleton
    LocusProcessor provideLocusProcessor(final ApiClientProvider apiClientProvider, final EventBus bus, final LocusDataCache locusDataCache,
                                         Ln.Context lnContext, DeviceRegistration deviceRegistration, Provider<Batch> batchProvider) {
        return new LocusProcessor(apiClientProvider, bus, locusDataCache, lnContext, deviceRegistration, batchProvider);
    }

    @Provides
    @Singleton
    LinusReachabilityService provideLinusReachabilityService(DeviceRegistration deviceRegistration, ApiClientProvider apiClientProvider,
                                                             EventBus bus, Gson gson, Ln.Context lnContext, MediaEngine mediaEngine) {
        return new LinusReachabilityService(deviceRegistration, apiClientProvider, bus, gson, lnContext, mediaEngine);
    }

    @Provides
    @Singleton
    UrlProvider provideUrlProvider() {
        return new SquaredUrlProvider();
    }

    @Provides
    @Singleton
    LocusMeetingInfoProvider provideLocusMeetingInfoProvider(Context context, ApiClientProvider apiClientProvider) {
        return new LocusMeetingInfoProvider(context, apiClientProvider);
    }

    @Provides
    @Singleton
    OAuth2 provideOauth2() {
        return OAuth2.userOauth2();
    }

    @Provides
    @Singleton
    SchedulerProvider provideSchedulerProvider() {
        return new SchedulerProvider();
    }

    @Provides
    @Singleton
    OperationQueue provideOperationQueue(Context context, Gson gson, EventBus bus, AuthenticatedUserProvider authenticatedUserProvider,
                                         NetworkReachability networkReachability, Provider<Batch> batchProvider, Injector injector,
                                         RefWatcher refWatcher, SdkClient sdkClient, LocusDataCache locusDataCache) {
        return new OperationQueue(context, gson, bus, authenticatedUserProvider, networkReachability, batchProvider,
                                  injector, refWatcher, sdkClient, locusDataCache);
    }

    @Provides
    @Singleton
    UploadLogsService provideUploadLogsService(Context context, ApiClientProvider apiClientProvider,
                                               ApiTokenProvider apiTokenProvider, MediaEngine mediaEngine,
                                               LogFilePrint logFilePrint, Settings settings,
                                               TrackingIdGenerator trackingIdGenerator, DeviceRegistration deviceRegistration,
                                               DiagnosticManager diagnosticManager, EventBus eventBus,
                                               RoomService roomService, ProvisioningClientProvider provisioningClientProvider) {
        return new UploadLogsService(context, apiClientProvider, apiTokenProvider, mediaEngine, logFilePrint, settings,
                                     trackingIdGenerator, deviceRegistration, diagnosticManager, eventBus, roomService,
                                     provisioningClientProvider);
    }

    @Provides
    @Singleton
    WhiteboardCache provideWhiteboardCache(EventBus eventBus, Gson gson) {
        return new WhiteboardCache(eventBus, gson);
    }
}
