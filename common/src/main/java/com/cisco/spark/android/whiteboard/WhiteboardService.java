package com.cisco.spark.android.whiteboard;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.WhiteboardChannelUpdateEvent;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.mercury.MercuryClient.WebSocketStatusCodes;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.mercury.events.WhiteboardMercuryClient;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.reachability.NetworkReachabilityChangedEvent;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.Sanitizer;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.cisco.spark.android.whiteboard.loader.WhiteboardLoader;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.RemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ImageContent;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.snapshot.SnapshotManager;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardLoadContentsEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardMercuryUpdateEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardServiceResponseEvent;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;
import retrofit2.Response;
import rx.Observable;

import static com.cisco.spark.android.whiteboard.WhiteboardError.ErrorData.LOAD_BOARD_ERROR;

public class WhiteboardService implements Component {

    protected static final String TAG = "WhiteboardService";
    protected static final long TIMER_DELAY_SERVER = 2000;
    protected static final long TIMER_DELAY_NETWORK = 1000;
    protected static final long TIMER_PERIOD = 5000;
    protected static final int MESSAGE_RELOAD_BOARD = 1;

    public static final int ERROR_TOLERANCE_THRESHOLD = 3;
    public static final int ERROR_DETECT_THRESHOLD = 4;

    protected final WhiteboardCache whiteboardCache;
    protected final LocusDataCache locusDataCache;
    protected final TrackingIdGenerator trackingIdGenerator;
    protected final ActivityListener activityListener;
    protected final Ln.Context lnContext;
    protected final Gson gson;
    protected final EventBus bus;
    protected final Sanitizer sanitizer;
    protected final SdkClient sdkClient;
    protected final JsonParser jsonParser;
    protected final KeyManager keyManager;
    protected final CoreFeatures coreFeatures;
    protected final ContentManager contentManager;
    protected final SnapshotManager snapshotManager;
    protected final ApiTokenProvider apiTokenProvider;
    protected final UserAgentProvider userAgentProvider;
    protected final SchedulerProvider schedulerProvider;
    protected final ApiClientProvider apiClientProvider;
    protected final Injector injector;
    protected final OperationQueue operationQueue;
    protected final DeviceRegistration deviceRegistration;
    protected final Settings settings;
    protected final Context context;
    protected final ContentResolver contentResolver;

    protected final WhiteboardLoader whiteboardLoader;
    protected final WhiteboardSavingInConversationDelegate whiteboardSavingDelegate;
    protected final WhiteboardMercuryController whiteboardMercuryController;
    protected final WhiteboardEncryptor whiteboardEncryptor;
    protected final WhiteboardShareDelegate shareDelegate;
    protected final AnnotationCreator annotationCreator;
    protected final WhiteboardCreator whiteboardCreator;

    private boolean running;

    // Avoid using these directly
    private Uri aclUrlLink;
    private Uri keyUrl;
    private Uri parentkmsResourceObjectUrl;
    private String aclId;
    private Uri mediaShareUrl;

    private WhiteboardContext wbContext;
    private Channel currentChannel;

    private String loadingChannelId;

    protected ApplicationController applicationController;

    @Nullable protected WhiteboardStore localStore;
    @Nullable protected WhiteboardStore remoteStore;

    private OnWhiteboardEventListener onWhiteboardEventListener;
    private boolean isReloadBoard;
    @Deprecated
    private boolean isReadonlyMode;
    private boolean isLosingNetwork;

    private long whiteboardLoadTimeStart;
    private long whiteboardStartDecryptingTime;
    protected Clock clock;

    private MetricsReporter metricsReporter;

    private List<PendingContent> pendingContents = new ArrayList<>();
    private final Object pendingContentLock = new Object();
    private Executor saveContentExecutor;

    private final Object stateLock = new Object();

    public WhiteboardService(WhiteboardCache whiteboardCache, ApiClientProvider apiClientProvider, Gson gson, EventBus eventBus,
                             ApiTokenProvider apiTokenProvider, KeyManager keyManager, OperationQueue operationQueue,
                             DeviceRegistration deviceRegistration, LocusDataCache locusDataCache, Settings settings,
                             UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator,
                             ActivityListener activityListener, Ln.Context lnContext, CallControlService callControlService, Sanitizer sanitizer, Context context,
                             Injector injector, EncryptedConversationProcessor conversationProcessor, SdkClient sdkClient,
                             ContentManager contentManager, LocusService locusService, SchedulerProvider schedulerProvider,
                             BitmapProvider bitmapProvider, FileLoader fileLoader, MediaEngine mediaEngine,
                             AuthenticatedUserProvider authenticatedUserProvider, CoreFeatures coreFeatures, Clock clock, MetricsReporter metricsReporter,
                             ContentResolver contentResolver) {
        this.gson = gson;
        this.bus = eventBus;
        this.apiTokenProvider = apiTokenProvider;
        this.operationQueue = operationQueue;
        this.deviceRegistration = deviceRegistration;
        this.keyManager = keyManager;
        this.sanitizer = sanitizer;
        this.schedulerProvider = schedulerProvider;
        this.coreFeatures = coreFeatures;
        this.jsonParser = new JsonParser();
        this.apiClientProvider = apiClientProvider;
        this.context = context;
        this.whiteboardCache = whiteboardCache;
        this.whiteboardEncryptor = new WhiteboardEncryptor(keyManager);
        this.locusDataCache = locusDataCache;
        this.settings = settings;
        this.userAgentProvider = userAgentProvider;
        this.trackingIdGenerator = trackingIdGenerator;
        this.activityListener = activityListener;
        this.lnContext = lnContext;
        this.injector = injector;
        this.sdkClient = sdkClient;
        this.contentManager = contentManager;
        this.metricsReporter = metricsReporter;
        this.clock = clock;
        this.contentResolver = contentResolver;

        // Potentially move this to the module
        this.shareDelegate = new WhiteboardShareDelegate(conversationProcessor, locusDataCache, callControlService, deviceRegistration,
                                                         apiClientProvider, this, locusService, keyManager, eventBus);
        this.whiteboardCreator = new WhiteboardCreator(apiClientProvider, this, schedulerProvider,
                                                       whiteboardCache, keyManager, sdkClient, eventBus, conversationProcessor,
                                                       authenticatedUserProvider, gson);
        this.whiteboardSavingDelegate = new WhiteboardSavingInConversationDelegate(this, schedulerProvider,
                apiClientProvider, conversationProcessor, injector, context, eventBus);
        this.whiteboardMercuryController = new WhiteboardMercuryController(whiteboardEncryptor, schedulerProvider,
                                                                           this, whiteboardCache, apiClientProvider, locusDataCache, bus,
                                                                           gson);

        this.whiteboardLoader = new WhiteboardLoader(this, whiteboardCache, schedulerProvider,
                                                     gson, bitmapProvider, context, apiClientProvider, fileLoader, mediaEngine);


        this.snapshotManager = new SnapshotManager(injector, keyManager, operationQueue, fileLoader, bus, context, this, contentManager, contentResolver);

        this.annotationCreator = new AnnotationCreator(apiClientProvider, this, schedulerProvider,
                                                       snapshotManager, callControlService, locusDataCache, coreFeatures,
                                                       keyManager, sdkClient, context, bus, fileLoader);


        localStore = buildLocalStore();
        remoteStore = buildRemoteStore();

        wbContext = new WhiteboardContext();
        saveContentExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    protected WhiteboardStore buildLocalStore() {
        // This is overridden by both implementations
        return null;
    }

    protected WhiteboardStore buildRemoteStore() {
        return new RemoteWhiteboardStore(this, apiClientProvider, injector, schedulerProvider);
    }

    public void setParentkmsResourceObjectUrl(Uri parentkmsResourceObjectUrl) {
        this.parentkmsResourceObjectUrl = parentkmsResourceObjectUrl;
    }

    public Uri getMediaShareUrl() {
        return mediaShareUrl;
    }

    public void setMediaShareUrl(Uri mediaShareUrl) {
        this.mediaShareUrl = mediaShareUrl;
    }

    @Override
    public boolean shouldStart() {
        return !running;
    }

    @Override
    public synchronized void start() {

        if (running) {
            return;
        }
        Ln.i("Whiteboard start");
        running = true;
        isLosingNetwork = false;

        if (!bus.isRegistered(this)) {
            bus.register(this);
        }

        shareDelegate.start();
        whiteboardCreator.start();
        snapshotManager.start();

        if (null != remoteStore) {
            remoteStore.start();
        }

        if (localStore != null) {
            localStore.start();
        }

        bus.post(WhiteboardServiceEvent.changeWhiteboardServiceState(true));
    }

    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
    }

    @Override
    public void stop() {
        Ln.i("Whiteboard stop");
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }

        shareDelegate.stop();
        whiteboardCreator.stop();
        snapshotManager.stop();
        if (null != remoteStore) {
            remoteStore.stop();
        }

        if (localStore != null) {
            localStore.stop();
        }

        running = false;
    }

    public void reset() {
        Ln.i("WhiteboardService unregisterWhiteboard");
        loadingChannelId = null;
        setCurrentChannel(null);
        isReloadBoard = false;
        wbContext = new WhiteboardContext();
        currentChannel = null;
        stopMercury();
        isReadonlyMode = false;

        if (localStore != null) {
            localStore.clearCache();
        }
        if (sdkClient.shouldClearRemoteBoardStore() && null != remoteStore) {
            remoteStore.clearCache();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
        this.applicationController = applicationController;
    }

    public synchronized UUID createBoard(WhiteboardCreator.CreateData createData, WhiteboardCreator.CreateCallback createCallback) {
        if (whiteboardCreator.createBoard(createData, createCallback)) {
            return createData.getId();
        } else {
            return null;
        }
    }

    public Channel loadBoardById(String boardId) {
        return getCurrentWhiteboardStore().getBoardById(boardId);
    }

    public WhiteboardStore getCurrentWhiteboardStore() {
        if (isLocal()) {
            Ln.d("It is a local board");
            return localStore;
        } else {
            return remoteStore;
        }
    }

    public WhiteboardStore getChannelWhiteboardStore(Channel channel) {
        if (channel.isPrivateChannel()) {
            return localStore;
        } else {
            return remoteStore;
        }
    }

    private JsonElement createImageContentNode(ImageContent imageContent) {

        JsonElement data = gson.toJsonTree(imageContent);
        JsonObject image = new JsonObject();
        image.addProperty("type", "image");
        image.add("data", data);

        return image;
    }

    public void uploadWhiteboardSnapshot(final Uri imageUrl, @NonNull final Channel channel) {
        snapshotManager.uploadWhiteboardSnapshot(imageUrl, channel, getAclId(), null, true, createSnapshotUploadListener(imageUrl));
    }

    public void uploadWhiteboardSnapshot(final Uri imageUrl, final UUID channelCreationRequestId) {
        snapshotManager.uploadWhiteboardSnapshot(imageUrl, channelCreationRequestId, getAclId(), createSnapshotUploadListener(imageUrl));
    }

    private SnapshotManager.SnapshotManagerUploadListener createSnapshotUploadListener(Uri imageUrl) {
        return new SnapshotManager.SnapshotManagerUploadListener() {

            @Override
            public void onSuccess(String operationId, com.cisco.spark.android.model.File file, Channel channel) {
                deleteSnapshotFile();
            }

            @Override
            public void onFailure(String errorMessage) {
                deleteSnapshotFile();

                createDefaultError(new JsonObject());
                setReloadBoard(true);
            }


            private void deleteSnapshotFile() {
                if (!new File(imageUrl.getPath()).delete())
                    Ln.w("Couldn't delete file: " + imageUrl.getPath());
            }
        };
    }


    public void downloadWhiteboardSnapshot(@Nullable SecureContentReference secureContentReference, @NonNull SnapshotManager.SnapshotManagerDownloadListener snapshotManagerDownloadListener) {
        snapshotManager.downloadWhiteboardSnapshot(secureContentReference, snapshotManagerDownloadListener);
    }

    public WhiteboardStore getRemoteStore() {
        return remoteStore;
    }

    public WhiteboardStore getPrivateStore() {
        return localStore;
    }

    public Channel getCurrentChannel() {
        synchronized (stateLock) {
            return currentChannel;
        }
    }

    public synchronized void setCurrentChannel(Channel currentChannel) {
        synchronized (stateLock) {
            Ln.e("Setting current channel " + currentChannel + " <- " + LoggingUtils.getCaller());
            this.currentChannel = currentChannel;
        }
        bus.post(new WhiteboardChannelUpdateEvent());
    }

    public boolean isLocal() {
        return getAclUrlLink() == null;
    }

    public synchronized void setAclUrlLink(Uri aclUrlLink) {
        synchronized (stateLock) {
            this.aclUrlLink = aclUrlLink;
        }
    }

    public void setAclId(String aclId) {
        synchronized (stateLock) {
            if (!TextUtils.isEmpty(aclId)) {
                this.aclId = aclId;
            }
        }
    }

    /**
     * Overridden in sparkling
     */
    public Uri getAclUrlLink() {
        synchronized (stateLock) {
            return aclUrlLink;
        }
    }

    public Uri getParentkmsResourceObjectUrl() {
        synchronized (stateLock) {
            return parentkmsResourceObjectUrl;
        }
    }

    // THIS IS THE KEY URL FOR THE CURRENT SPACE BEING VIEWED. ****NOT**** THE WHITEBOARD
    public Uri getDefaultConversationKeyUrl() {
        synchronized (stateLock) {
            return keyUrl;
        }
    }

    public void setDefaultConversationKeyUrl(Uri keyUrl) {
        synchronized (stateLock) {
            this.keyUrl = keyUrl;
        }
    }

    public String getAclId() {
        synchronized (stateLock) {
            return aclId;
        }
    }

    public WhiteboardContext getWhiteboardContext() {
        return wbContext;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public UUID beginAnnotation(AnnotationCreator.AnnotationParams args) {
        return annotationCreator.beginAnnotation(args);
    }

    public AnnotationCreator getAnnotationCreator() {
        return annotationCreator;
    }

    public DeviceRegistration getDeviceRegistration() {
        return deviceRegistration;
    }

    public void parseContentItems(String channelId, List<Content> items, boolean autoloadContentBatch) {

        if (items == null || items.size() == 0) {
            // Empty board
            bus.post(new WhiteboardLoadContentsEvent(items, getCurrentChannel(), !autoloadContentBatch));
            return;
        }
        whiteboardStartDecryptingTime = clock.monotonicNow();
        List<Content> parsedContents = new ArrayList<>();
        List<Content> failedContents = new ArrayList<>();

        for (Content content : items) {
            String data = whiteboardEncryptor.decryptContent(content);
            if (!TextUtils.isEmpty(data)) {
                content.setPayload(data);
                parsedContents.add(content);
            } else {
                failedContents.add(content);
            }
        }

        if (!failedContents.isEmpty()) {
            bus.post(new WhiteboardError(WhiteboardError.ErrorData.DECRYPT_CONTENTS_ERROR, channelId));
        } else if (!parsedContents.isEmpty()) {
            bus.post(new WhiteboardLoadContentsEvent(parsedContents, getCurrentChannel(), !autoloadContentBatch));
            sendWhiteboardLoadMetrics(items.size(), channelId);
        } else {
            Ln.e(new IllegalStateException("Does not compute"));
        }
    }

    protected void clearPendingContent() {
        synchronized (pendingContentLock) {
            pendingContents.clear();
        }
    }

    @WorkerThread
    public void privateShare(LocusKey locusKey) {
        if (sdkClient.supportsPrivateBoards()) {
            shareDelegate.adHocShare(getCurrentChannel(), locusKey);
        } else {
            Ln.w("Invoking private share on a client that doesn't support private boards");
        }
    }

    public WhiteboardShareDelegate.ShareRequest getShareState() {
        return shareDelegate.getShareRequestState();
    }

    public Observable<WhiteboardShareDelegate.ShareState> getShareRequestStream() {
        return shareDelegate.getShareRequestStream();
    }

    public boolean isLoadingBoard() {
        return loadingChannelId != null;
    }

    public void setLoadingChannel(String loadingBoard) {
        loadingChannelId = loadingBoard;
    }

    synchronized void loadBoardFromMercury(String channelId) {

        if (channelId == null) {
            Ln.e(new NullPointerException("Can't load board with null channel ID"));
            return;
        }

        if (isLoadingBoard()) {
            Ln.d("Board already loading");
            return;
        }

        Ln.d("Loading whiteboard with id %s from cloud", channelId);

        setLoadingChannel(channelId);

        Channel tempChannel = new Channel();
        tempChannel.setChannelId(channelId);
        setCurrentChannel(tempChannel); //// TODO: 09.11.16 need this ?

        WhiteboardStore store = getCurrentWhiteboardStore();
        store.getChannel(channelId).subscribe(channel -> {
            // TODO This is where mercury should be initialised
            setCurrentChannel(channel);
            store.loadContent(channelId);
        }, throwable -> {
            bus.post(new WhiteboardError(LOAD_BOARD_ERROR));
            setLoadingChannel(null);
            setCurrentChannel(null);
        });
    }

    public void saveContents(final JsonObject msg) {
        if (msg == null) return;
        List<Content> contentRequests = parseContents(msg);
        if (contentRequests == null) {
            return;
        }
        handleWhiteboardMessage(msg);

        Observable.just(1)
                .subscribeOn(schedulerProvider.from(getSaveContentExecutor()))
                .subscribe(integer -> {
                    getCurrentWhiteboardStore().saveContent(getCurrentChannel(), contentRequests, msg);
                }, Ln::e);
    }

    @Nullable
    @CheckResult
    protected List<Content> parseContents(JsonObject msg) {
        final JsonArray contents = msg.get("data").getAsJsonArray();
        if (contents == null) {
            Ln.w("Missing mandatory property \"contents\" for \"saveContents\" message");
            return null;
        }

        List<Content> contentRequests = createEncryptedContents(contents);
        if (contentRequests == null || contentRequests.isEmpty()) {
            Ln.e("Not trying to create contents for board when there aren't any contents");
            return null;
        }

        return contentRequests;
    }

    public void clearBoard(final JsonObject msg) {
        Observable.just(1)
                .subscribeOn(schedulerProvider.from(getSaveContentExecutor()))
                .subscribe(integer -> {
                    Channel currentChannel = getCurrentChannel();
                    String boardId = currentChannel != null ? currentChannel.getChannelId() : null;
                    Whiteboard whiteboard = whiteboardCache.getWhiteboard(boardId);
                    if (whiteboard == null || whiteboard.getBackgroundBitmap() == null) {
                        Ln.d("clear board - no background content");
                        getCurrentWhiteboardStore().clear(getBoardId(), msg);
                    } else {
                        Ln.d("clear board - have background content");
                        List<Content> backgroundContentList = new ArrayList<>();
                        backgroundContentList.add(whiteboard.getBackgroundContent());
                        getCurrentWhiteboardStore().clearPartialContents(getBoardId(), backgroundContentList, msg);
                    }
                }, Ln::e);
    }

    public void clearBoardSnapshot(Channel snapshotChannel) {
        snapshotManager.clearWhiteboardSnapshot(snapshotChannel, getChannelWhiteboardStore(snapshotChannel), getAclId());
    }

    public void postWhiteboardsSnapshotsToConversation(Conversation conversation, List<Channel> channels) {
        snapshotManager.postWhiteboardsSnapshotsToConversation(conversation, channels);
    }

    public synchronized String getBoardId() {
        if (getCurrentChannel() != null) {
            return getCurrentChannel().getChannelId();
        } else {
            return null;
        }

    }

    protected void handleWhiteboardMessage(JsonObject msg) {
    }

    @Nullable
    @CheckResult
    private List<Content> createEncryptedContents(JsonArray contents) {
        List<Content> listContents = new ArrayList<>();

        Channel currentChannel = getCurrentChannel();

        if (currentChannel == null) {
            return null;
        }


        Uri keyUrl = currentChannel.getDefaultEncryptionKeyUrl();

        for (JsonElement content : contents) {
            JsonObject contentObject = content.getAsJsonObject();
            JsonElement jsonElement = contentObject.get("data");
            //contentObject.get("type").getAsString()  will be uncommented when the solution is determined.
            listContents.add(new Content(Content.CONTENT_TYPE, sdkClient.getDeviceType(), keyUrl.toString(),
                    jsonElement.toString()
            ));
        }
        return whiteboardEncryptor.encryptContent(listContents, keyUrl);
    }

    public void createResponse(JsonObject msg, JsonElement data, Response error) {
        JsonObject errorResponse = null;
        if (error != null) {
            ErrorDetail errorDetail = null;
            try {
                errorDetail = gson.fromJson(error.errorBody().string(), ErrorDetail.class);
            } catch (JsonSyntaxException | IOException e) {
                Ln.e(e);
            }

            if (errorDetail != null) {
                errorResponse = new JsonObject();
                errorResponse.addProperty("code", error.code());
                errorResponse.addProperty("message", errorDetail.getMessage());
                errorResponse.addProperty("backtrace", errorDetail.getStackTrace());
            }
        }

        sendRESTResponseEvent(msg, data, errorResponse);
    }

    protected void sendRESTResponseEvent(final JsonObject msg, final JsonElement data, final JsonObject errorResponse) {
        if (msg != null && msg.has("boardId") && !msg.get("boardId").isJsonNull() &&
                msg.get("boardId").getAsString().equals(getBoardId())) {
            String action = null;
            if (msg.has("action")) {
                action = msg.get("action").getAsString();
            }

            JsonObject response = new JsonObject();
            if (msg.has("writerId")) {
                response.addProperty("writerId", msg.get("writerId").getAsString());
            }
            if (data != null) {
                response.add("data", data);
            }

            bus.post(new WhiteboardServiceResponseEvent(action, response, errorResponse));
        }
    }

    protected JsonObject createError(int code, String message, String backtrace) {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("code", code);
        errorResponse.addProperty("message", message);
        errorResponse.addProperty("backtrace", backtrace);
        return errorResponse;
    }

    public void createDefaultError(JsonObject msg) {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("code", "666");
        errorResponse.addProperty("message", "Generic error");
        errorResponse.addProperty("backtrace", "Nothing to see here");

        sendRESTResponseEvent(msg, null, errorResponse);
    }

    public void partialResetBoard() {
        setCurrentChannel(null);
        clearPendingContent();
        getMercuryController().clearPendingMessages();
    }

    public void fullResetBoard() {
        partialResetBoard();
        stopMercury();
    }

    public void realtimeMessage(final JsonObject msg) {

        final Uri appKeyUrl = getDefaultConversationKeyUrl();
        final Uri aclUrl = getAclUrlLink();

        if (getCurrentChannel() == null) {
            createBoard(new WhiteboardCreator.CreateData(getWhiteboardContext()), new WhiteboardCreator.CreateCallback() {
                @Override
                public void onSuccess(UUID createID, Channel channel) {
                    // TODO
                }

                @Override
                public void onFail(UUID uuid) {
                    // TODO
                }
            });
        }

        whiteboardMercuryController.realtimeMessage(msg, getCurrentChannel());
    }

    private void inCallMercuryInit(Channel channel) {
        whiteboardMercuryController.inCallMercuryInit(channel);
    }

    public boolean usePrimaryMercury() {
        return whiteboardMercuryController.usePrimaryMercury();
    }

    public Observable<Whiteboard> load(String channelId) {
        return whiteboardLoader.load(channelId);
    }

    public Observable<Whiteboard> load(String channelId, WhiteboardLoader.LoaderArgs loaderArgs) {
        return whiteboardLoader.load(channelId, loaderArgs);
    }

    public void loadBoard(final String channelId, final boolean isLoadboard) {
        whiteboardLoadTimeStart = clock.monotonicNow();
        if (isLoadboard) {
            loadBoardFromMercury(channelId);
        }
        whiteboardMercuryController.initMercury(channelId, false);
    }

    protected void reloadBoard() {
        loadBoard(getBoardId(), true);
    }

    public void stopMercury() {
       whiteboardMercuryController.stopMercury();
    }

    /**
     * Separate method for mocking
     */
    @NonNull
    protected MercuryClient createWhiteboardMercuryClient() {
        return new WhiteboardMercuryClient(apiClientProvider, gson, bus,
                deviceRegistration, activityListener, lnContext, this, operationQueue, sanitizer);
    }

    // Need to maintain this interface until the web one goes away
    public void mercuryEvent(WhiteboardActivityEvent event) {
        whiteboardMercuryController.mercuryEvent(event);
    }

    public JsonObject createBridgeContentJson(Content contentModel) {
        String type = contentModel.getType();

        if (Content.CONTENT_TYPE_FILE.equals(type)) {
            return createFileContentJson(contentModel);
        } else {
            return createBridgeStringContentJson(contentModel);
        }
    }

    @Nullable
    protected JsonObject createFileContentJson(Content contentModel) {
        return null;
    }

    private JsonObject createBridgeStringContentJson(Content contentModel) {
        String data = whiteboardEncryptor.decryptContent(contentModel);
        JsonObject dataObject = null;

        /**
         * the board data could be encrypted with the wrong key by malfunctioning clients, need to
         * report a Error in this case. Will add it after getting the UE design.
         */
        if (TextUtils.isEmpty(data)) {
            dataObject = new JsonObject();
        } else {
            try {
                dataObject = jsonParser.parse(data).getAsJsonObject();
            } catch (IllegalStateException e) {
                Ln.w(e, "Wrong whitebaord data.");
            }
        }
        JsonObject content = new JsonObject();
        content.addProperty("id", contentModel.getContentId());
        content.addProperty("type", contentModel.getType());
        content.add("data", dataObject);
        return content;
    }

    public void setPrimaryMercury(MercuryClient mercuryClient) {
        whiteboardMercuryController.setPrimaryMercury(mercuryClient);
    }

    public void setMetricsReporter(MetricsReporter metricsReporter) {
        this.metricsReporter = metricsReporter;
    }

    public WhiteboardShareDelegate getWhiteboardShareDelegate() {
        return shareDelegate;
    }

    public void share() {
        shareDelegate.share(getCurrentChannel(), getAclId());
    }

    public void removeSelfFromWhiteboardsAcls(List<Channel> boards) {
        //All boards must have at least: boardId, kmsResourceUrl and aclUrl
        for (Channel board : boards) {
            whiteboardSavingDelegate.removeSelfFromBoardAcl(board, apiTokenProvider.getAuthenticatedUser().getUserId());
        }
    }

    public void setOnWhiteboardEventListener(OnWhiteboardEventListener onWhiteboardEventListener) {
        this.onWhiteboardEventListener = onWhiteboardEventListener;
    }

    public void boardReady() {
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardFinished();
        }
        bus.post(WhiteboardStateEvent.whiteboardPersistanceReady());
    }

    public void boardStart() {
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardStarted();
        }
    }

    public void boardConnectMercuryError(String errorMessage) {
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardError(new WhiteboardError(WhiteboardError.ErrorData.LOAD_BOARD_ERROR, null, errorMessage));
        }
    }

    public String getSharedBoardId() {
        return shareDelegate.getSharedBoardId();
    }

    public boolean isSharingBoard(String boardId) {
        String sharedBoardId = getSharedBoardId();
        return sharedBoardId != null && sharedBoardId.equals(boardId);
    }

    public void saveContents(List<Content> content, String id) {
        Channel channel = getCurrentChannel();
        if (channel == null) {
            synchronized (pendingContentLock) {
                pendingContents.add(new PendingContent(content, id));
            }
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("writerId", id);
        json.addProperty("boardId", channel.getChannelId());
        json.addProperty("action", WhiteboardConstants.SAVE_CONTENT);
        bus.post(new WhiteboardMercuryUpdateEvent(content, channel.getChannelId(), false));
        List<Content> encryptedContents = whiteboardEncryptor.encryptContent(content, channel.getDefaultEncryptionKeyUrl());

        if (encryptedContents != null) {
            Observable.just(1)
                    .subscribeOn(schedulerProvider.from(getSaveContentExecutor()))
                    .subscribe(integer -> {
                        getCurrentWhiteboardStore().saveContent(channel, encryptedContents, json);
                    }, Ln::e);
        } else {
            Ln.e("Unable to encrypt contents.");
        }
    }

    public void clearBoard() {
        String boardId = getBoardId();
        JsonObject json = new JsonObject();
        json.addProperty("boardId", boardId);
        json.addProperty("action", WhiteboardConstants.CLEAR_BOARD);
        clearBoard(json);
    }

    public void unshareWhiteboard() {
        shareDelegate.stopShare();
    }

    public WhiteboardMercuryController getMercuryController() {
        return whiteboardMercuryController;
    }

    public String getLoadingChannel() {
        return loadingChannelId;
    }

    public void setWhiteboardContext(WhiteboardContext whiteboardContext) {

        synchronized (stateLock) {
            this.aclId = whiteboardContext.getAclId();
            this.aclUrlLink = whiteboardContext.getAclUrl();
            this.keyUrl = whiteboardContext.getDefaultEncryptionKeyUrl();
            this.parentkmsResourceObjectUrl = whiteboardContext.getKro();
            this.wbContext = whiteboardContext;
        }
    }

    public interface OnWhiteboardEventListener {

        void onBoardStarted();

        void onBoardFinished();

        void onBoardError(WhiteboardError whiteboardError);

        void onSnapshotReceived(String image);

        void onBoardCreated(Channel channel);

        void onBoardContentLoaded();
    }

    public OnWhiteboardEventListener getOnWhiteboardEventListener() {
        return onWhiteboardEventListener;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(NetworkReachabilityChangedEvent event) {
        if (!isWhiteboardEnabled()) {
            return;
        }
        if (event.isConnected()) {
            Ln.d("Network is connected");
            isLosingNetwork = false;
            bus.post(WhiteboardServiceEvent.changeWhiteboardServiceState(false));
            if (isReloadBoard) {
                reloadBoard();
            }
        } else {
            Ln.d("Network is disconnected");
            isLosingNetwork = true;
        }
    }

    public void mercuryErrorEvent(ResetEvent event) {

        if (!isWhiteboardEnabled()) {
            return;
        }

        whiteboardMercuryController.stopMercury();

        Ln.d("mercuryErrorEvent " + event.getCode());
        WhiteboardError whiteboardError = new WhiteboardError(WhiteboardError.ErrorData.LOSE_NETWORK_CONNECTION_ERROR);
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardError(whiteboardError);
        }
        bus.post(WhiteboardServiceEvent.changeWhiteboardServiceState(false));

        // If we think that the connection has died for no good reason, try to open a new one
        Channel currentChannel = getCurrentChannel();
        if (isRunning() && currentChannel != null && shouldAttemptReopen(event)) {
            whiteboardMercuryController.initMercury(currentChannel.getChannelId(), false);
        }
    }

    protected boolean shouldAttemptReopen(ResetEvent event) {
        WebSocketStatusCodes code = event.getCode();
        return code == WebSocketStatusCodes.CLOSE_ABNORMAL || code == WebSocketStatusCodes.CLOSE_UNKNOWN;
    }

    /**
     * Overridden in sparkling
     */
    protected boolean isWhiteboardEnabled() {
        return coreFeatures.isWhiteboardEnabled();
    }

    public void setReloadBoard(boolean reloadBoard) {
        isReloadBoard = reloadBoard;
    }

    public boolean shouldReloadBoard() {
        return isReloadBoard;
    }

    @Deprecated
    public boolean isReadonlyMode() {
        return isReadonlyMode;
    }

    @Deprecated
    public void setReadonlyMode(boolean readonlyMode) {
        isReadonlyMode = readonlyMode;
    }

    @Nullable
    public Uri getBoardServiceUrl(@NonNull String boardId) {

        Uri url = deviceRegistration.getBoardServiceUrl();
        if (url == null) {
            return null;
        }

        url = Uri.withAppendedPath(url, "channels");
        url = Uri.withAppendedPath(url, boardId);
        return url;
    }

    public boolean isLosingNetwork() {
        return isLosingNetwork;
    }


    @Deprecated
    public void getChannelInfo(String channelId) {
        getCurrentWhiteboardStore().getChannelInfo(channelId, null);
    }

    public void getChannelInfo(String channelId, @Nullable AbsRemoteWhiteboardStore.ChannelInfoCallback callback) {
        getCurrentWhiteboardStore().getChannelInfo(channelId, callback);
    }

    public void getChannelComplete(Channel channel) {
        Channel currentChannel = getCurrentChannel();
        if (currentChannel != null && currentChannel.getDefaultEncryptionKeyUrl() == null) {
            currentChannel.setDefaultEncryptionKeyUrl(getDefaultConversationKeyUrl());
            inCallMercuryInit(channel);
        }
    }

    @Deprecated //should use the callback passed as in param instead
    public void getChannelInfoComplete(final Channel channel) {
        WhiteboardChannelInfoReadyEvent event = new WhiteboardChannelInfoReadyEvent(channel);
        bus.post(event);
    }

    public void createBoardComplete(Channel channel, UUID channelCreationRequestId, Uri keyUrl) {

        setCurrentChannel(channel);

        synchronized (pendingContentLock) {
            for (PendingContent pendingContent : pendingContents) {
                List<Content> pendingContentList = pendingContent.getContents();
                for (Content content : pendingContentList) {
                    whiteboardCache.addStrokeToCurrentRealtimeBoard(WhiteboardUtils.createStroke(content, jsonParser, gson));
                }
                saveContents(pendingContentList, pendingContent.getId());
            }
            pendingContents.clear();
        }

        inCallMercuryInit(channel);
        bus.post(new WhiteboardCreationCompleteEvent(channelCreationRequestId, channel));
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardCreated(channel);
        }
    }

    public void loadBoardsError(WhiteboardError.ErrorData errorData) {
        bus.post(errorData);
    }

    public WhiteboardEncryptor getWhiteboardEncryptor() {
        return whiteboardEncryptor;
    }

    public Executor getSaveContentExecutor() {
        if (null == saveContentExecutor) {
            saveContentExecutor = Executors.newSingleThreadExecutor();
        }
        return saveContentExecutor;
    }

    private void sendWhiteboardLoadMetrics(int whiteboardsize, String whiteboardId) {
        MetricsReportRequest request = metricsReporter.newWhiteboardServiceMetricsBuilder()
                .reportWhiteboardLoaded(
                        whiteboardStartDecryptingTime - whiteboardLoadTimeStart, clock.monotonicNow() - whiteboardStartDecryptingTime, whiteboardsize, whiteboardId
                )
                .build();
        metricsReporter.enqueueMetricsReport(request);
    }

    public interface WhiteboardServiceAvailableHandler {
        void onWhiteboardServiceAvailable(String convId, Uri keyUrl, Uri aclUrlLink);
    }

    public static class WhiteboardContext {

        private Uri aclUrl;
        private Uri defaultEncryptionKeyUrl;
        private String aclId;
        private Uri kro;

        public WhiteboardContext() {

        }

        public WhiteboardContext(Uri aclUrl, Uri defaultEncryptionKeyUrl, String aclId, Uri kro) {

            this.aclUrl = aclUrl;
            this.defaultEncryptionKeyUrl = defaultEncryptionKeyUrl;
            this.aclId = aclId;
            this.kro = kro;
        }

        public Uri getAclUrl() {
            return aclUrl;
        }

        public Uri getDefaultEncryptionKeyUrl() {
            return defaultEncryptionKeyUrl;
        }

        public String getAclId() {
            return aclId;
        }

        public Uri getKro() {
            return kro;
        }
    }
}
