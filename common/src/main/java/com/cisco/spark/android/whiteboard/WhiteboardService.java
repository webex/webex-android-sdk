package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Base64;

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
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.mercury.events.WhiteboardMercuryClient;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.reachability.NetworkReachabilityChangedEvent;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.RemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ImageContent;
import com.cisco.spark.android.whiteboard.snapshot.SnapshotManager;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardLoadContentsEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardMercuryUpdateEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardServiceResponseEvent;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;
import retrofit2.Response;
import rx.Observable;

import static com.cisco.spark.android.whiteboard.SnapshotRequest.SnapshotRequestType.GET_SNAPSHOT_FOR_CHAT;
import static com.cisco.spark.android.whiteboard.SnapshotRequest.SnapshotRequestType.GET_SNAPSHOT_FOR_UPLOAD;

public class WhiteboardService implements Component {

    protected static final String TAG = "WhiteboardService";
    protected static final long TIMER_DELAY_SERVER = 2000;
    protected static final long TIMER_DELAY_NETWORK = 1000;
    protected static final long TIMER_PERIOD = 5000;
    protected static final long DEFAULT_IMAGE_UPLOAD_INTERVAL = 360000;
    protected static final int MESSAGE_RELOAD_BOARD = 1;

    public static final int ERROR_TOLERANCE_THRESHOLD = 3;
    public static final int ERROR_DETECT_THRESHOLD = 4;

    private final SnapshotManager snapshotManager;
    private final SdkClient sdkClient;
    private final LocusDataCache locusDataCache;

    protected final TrackingIdGenerator trackingIdGenerator;
    protected final ActivityListener activityListener;
    protected final Ln.Context lnContext;
    protected final Gson gson;
    protected final EventBus bus;
    protected final JsonParser jsonParser;
    protected final KeyManager keyManager;
    protected final ContentManager contentManager;
    protected final ApiTokenProvider apiTokenProvider;
    protected final UserAgentProvider userAgentProvider;
    protected final SchedulerProvider schedulerProvider;
    protected final ApiClientProvider apiClientProvider;
    protected final Injector injector;
    protected final DeviceRegistration deviceRegistration;
    protected final Settings settings;

    protected final WhiteboardEncryptor whiteboardEncryptor;
    protected final WhiteboardShareDelegate shareDelegate;
    protected final WhiteboardCreator whiteboardCreator;
    protected final WhiteboardSavingInConversationDelegate whiteboardSavingDelegate;
    protected final WhiteboardMercuryController whiteboardMercuryController;

    private boolean running;

    // Avoid using these directly
    private Uri aclUrlLink;
    private Uri keyUrl;
    private Uri parentkmsResourceObjectUrl;
    private String aclId;
    private Uri mediaShareUrl;
    private Channel backgroundUpdatingChannel;

    private Channel currentChannel;
    private String loadingChannelId;

    protected ApplicationController applicationController;

    @Nullable protected WhiteboardStore localStore;
    protected WhiteboardStore remoteStore;

    private OnWhiteboardEventListener onWhiteboardEventListener;
    private boolean isReloadBoard;
    private boolean isReadonlyMode;
    private boolean isLosingNetwork;

    private boolean uploadSnapshotWorking;
    private boolean useSharedWebSocket;
    private UploadSnapshotTask snapshotTask;
    private long imageUpdateInterval;

    // FIXME These should be pulled out
    protected Map<String, String> whiteboardCache = new ConcurrentHashMap<>();
    private Map<String, String> sentWhiteboard = new ConcurrentHashMap<>();

    @Deprecated
    protected boolean hasMultipleLocalBoards;
    @Deprecated
    protected boolean hasMultipleRemoteBoards;
    protected int numberOfLocalBoards;
    protected int numberOfRemoteBoards;
    protected SnapshotSyncQueue pendingGetSnapshotQueue;

    private List<PendingContent> pendingContents = new ArrayList<>();
    private final Object pendingContentLock = new Object();

    public WhiteboardService(ApiClientProvider apiClientProvider, Gson gson, EventBus eventBus,
                             ApiTokenProvider apiTokenProvider, KeyManager keyManager, OperationQueue operationQueue,
                             DeviceRegistration deviceRegistration, LocusDataCache locusDataCache, Settings settings,
                             UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator,
                             ActivityListener activityListener, Ln.Context lnContext,
                             CallControlService callControlService, Context context, Injector injector,
                             EncryptedConversationProcessor conversationProcessor, SdkClient sdkClient,
                             ContentManager contentManager, LocusService locusService, SchedulerProvider schedulerProvider,
                             AuthenticatedUserProvider authenticatedUserProvider) {

        this.gson = gson;
        this.bus = eventBus;
        this.apiTokenProvider = apiTokenProvider;
        this.deviceRegistration = deviceRegistration;
        this.keyManager = keyManager;
        this.schedulerProvider = schedulerProvider;
        this.jsonParser = new JsonParser();
        this.apiClientProvider = apiClientProvider;

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

        // Potentially move this to the module
        this.shareDelegate = new WhiteboardShareDelegate(conversationProcessor, locusDataCache, callControlService, deviceRegistration,
                                                         apiClientProvider, this, locusService, keyManager, eventBus);
        this.whiteboardCreator = new WhiteboardCreator(deviceRegistration, apiClientProvider, this, schedulerProvider,
                                                       keyManager, sdkClient, eventBus, conversationProcessor, injector,
                                                       context, authenticatedUserProvider);
        this.whiteboardSavingDelegate = new WhiteboardSavingInConversationDelegate(this, schedulerProvider,
                apiClientProvider, conversationProcessor, injector, context, eventBus);
        this.whiteboardMercuryController = new WhiteboardMercuryController(whiteboardEncryptor, schedulerProvider,
                                                                           this, apiClientProvider, locusDataCache, bus,
                                                                           gson);


        localStore = buildLocalStore();
        remoteStore = buildRemoteStore();
        snapshotTask = new UploadSnapshotTask();
        currentChannel = null;

        snapshotManager = new SnapshotManager(injector, keyManager, operationQueue, new FileLoader(apiClientProvider), bus, context, this);
        pendingGetSnapshotQueue = new SnapshotSyncQueue();
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
        Ln.i(TAG, "Whiteboard start");
        running = true;
        isLosingNetwork = false;

        if (!bus.isRegistered(this)) {
            bus.register(this);
        }

        shareDelegate.start();
        whiteboardCreator.start();
        snapshotManager.start();

        remoteStore.start();

        if (localStore != null) {
            localStore.start();
        }

        bus.post(WhiteboardServiceEvent.changeWhiteboardServiceState(true));
    }

    @Override
    public void stop() {
        Ln.i(TAG, "Whiteboard stop");
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }

        shareDelegate.stop();
        whiteboardCreator.stop();
        snapshotManager.stop();

        remoteStore.stop();

        if (localStore != null) {
            localStore.stop();
        }

        running = false;
    }

    public void reset() {
        Ln.i("WhiteboardService unregisterWhiteboard");
        loadingChannelId = null;
        aclUrlLink = null;
        keyUrl = null;
        currentChannel = null;
        isReloadBoard = false;
        stopMercury();
        isReadonlyMode = false;
        if (snapshotTask != null) {
            snapshotTask.cancelTimer();
        }
        imageUpdateInterval = 0L;
        whiteboardCache.clear();
        sentWhiteboard.clear();
        pendingGetSnapshotQueue.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
        this.applicationController = applicationController;
    }

    public synchronized void createBoardGeneral(String conversationId, Uri keyUrl, Uri aclUrl) {
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(conversationId, keyUrl, aclUrl));
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

    public synchronized UUID getImageSnapshot(SnapshotRequest.SnapshotRequestType snapShotType) {
        SnapshotRequest request = SnapshotRequest.getSnapshotRequest(snapShotType);
        pendingGetSnapshotQueue.add(request);
        sendGetImageSnapshot();
        return request.getRequestId();
    }

    public UUID getImageSnapShotForSendToChat() {
        return getImageSnapshot(GET_SNAPSHOT_FOR_CHAT);
    }

    public UUID getImageSnapshotForUpload() {
        return getImageSnapshot(GET_SNAPSHOT_FOR_UPLOAD);
    }

    public void getImageSnapshotResponse(final JsonObject msg) {
        String image = msg.get("data").getAsString();
        if (TextUtils.isEmpty(image))
            return;
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onSnapshotReceived(image);
        }
        File file = parseSnapshot(image);
        if (null == file)
            return;
        handleSnapshot(image, file);
    }

    protected void handleSnapshot(String image, File file) {
        SnapshotRequest request = pendingGetSnapshotQueue.poll();
        if (request == null)
            return;
        if (request.getRequestType() == GET_SNAPSHOT_FOR_UPLOAD) {
            sendSnapShot(image, file);
        } else if (request.getRequestType() == GET_SNAPSHOT_FOR_CHAT) {
            bus.post(WhiteboardSnapshotEvent.whiteboardSnapshotEvent(image, file, request.getRequestId(), request.getRequestType()));
        }
    }

    public void uploadWhiteboardSnapshot(final Uri imageUrl, final Channel snapshotChannel) {
        snapshotManager.uploadWhiteboardSnapshot(imageUrl, snapshotChannel, getChannelWhiteboardStore(snapshotChannel), snapshotChannel.getDefaultEncryptionKeyUrl(), getAclId(),
                new SnapshotManager.SnapshotManagerUploadListener() {
                    @Override
                    public void onSuccess() {
                        if (!new java.io.File(imageUrl.getPath()).delete())
                            Ln.w("Couldn't delete file: " + imageUrl.getPath());
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!new java.io.File(imageUrl.getPath()).delete())
                            Ln.w("Couldn't delete file: " + imageUrl.getPath());
                        createDefaultError(new JsonObject());
                        setReloadBoard(true);
                    }
                });
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
        return currentChannel;
    }

    public synchronized void setCurrentChannel(Channel currentChannel) {
        this.currentChannel = currentChannel;
        bus.post(new WhiteboardChannelUpdateEvent());
    }

    public boolean isLocal() {
        if (deviceRegistration.getFeatures().isWhiteboardWithAclEnabled()) {
            return getAclUrlLink() == null;
        } else {
            return getAclId() == null;
        }
    }

    public synchronized void setAclUrlLink(Uri aclUrlLink) {
        this.aclUrlLink = aclUrlLink;
    }

    public void setAclId(String aclId) {
        if (!TextUtils.isEmpty(aclId)) {
            this.aclId = aclId;
        }
    }

    /**
     * Overridden in sparkling
     */
    public Uri getAclUrlLink() {
        return aclUrlLink;
    }

    public Uri getParentkmsResourceObjectUrl() {
        return parentkmsResourceObjectUrl;
    }

    // THIS IS THE KEY URL FOR THE CURRENT SPACE BEING VIEWED. ****NOT**** THE WHITEBOARD
    public Uri getDefaultConversationKeyUrl() {
        return keyUrl;
    }

    public void setDefaultConversationKeyUrl(Uri keyUrl) {
        this.keyUrl = keyUrl;
    }

    public String getAclId() {
        return aclId;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public DeviceRegistration getDeviceRegistration() {
        return deviceRegistration;
    }

    public void parseContentItems(List<Content> items, boolean autoloadContentBatch) {
        List<Content> parsedContents = new ArrayList<>();
        if (items != null) {
            for (Content content : items) {
                String data = whiteboardEncryptor.decryptContent(content);
                if (!TextUtils.isEmpty(data)) {
                    content.setPayload(data);
                    parsedContents.add(content);
                }
            }
        }
        bus.post(new WhiteboardLoadContentsEvent(parsedContents, getBoardId(), !autoloadContentBatch));
    }

    protected void clearPendingContent() {
        synchronized (pendingContentLock) {
            pendingContents.clear();
        }
    }

    protected void sendGetImageSnapshot() {
        // FIXME
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

        getCurrentWhiteboardStore().loadContent(channelId);
        getCurrentWhiteboardStore().getChannel(channelId);
    }

    public void saveContents(final JsonObject msg) {
        if (msg == null) return;
        List<Content> contentRequests = parseContents(msg);
        if (contentRequests == null) {
            return;
        }
        handleWhiteboardMessage(msg);
        getCurrentWhiteboardStore().saveContent(getBoardId(), contentRequests, msg);
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
        getCurrentWhiteboardStore().clear(getBoardId(), msg);
    }

    public void clearBoardSnapshot(Channel snapshotChannel) {
        snapshotManager.clearWhiteboardSnapshot(snapshotChannel, getChannelWhiteboardStore(snapshotChannel), snapshotChannel.getDefaultEncryptionKeyUrl(), getAclId());
    }

    public synchronized String getBoardId() {
        if (getCurrentChannel() != null) {
            return getCurrentChannel().getChannelId();
        } else {
            return null;
        }

    }

    @Nullable
    @CheckResult
    private List<Content> createEncryptedContents(JsonArray contents) {
        List<Content> listContents = new ArrayList<>();

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
            } catch (IOException e) {
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

    public void resetBoard() {
        setCurrentChannel(null);
        stopMercury();
        clearPendingContent();
        getMercuryController().clearPendingMessages();
    }

    public void realtimeMessage(final JsonObject msg) {

        final String conversationId = getAclId();
        final Uri appKeyUrl = getDefaultConversationKeyUrl();
        final Uri aclUrl = getAclUrlLink();

        if (getCurrentChannel() == null) {
            createBoardGeneral(conversationId, appKeyUrl, aclUrl);
        }

        whiteboardMercuryController.realtimeMessage(msg, getCurrentChannel());
    }

    private void inCallMercuryInit(Channel channel) {
        whiteboardMercuryController.inCallMercuryInit(channel);
    }

    public boolean usePrimaryMercury() {
        return whiteboardMercuryController.usePrimaryMercury();
    }

    public void loadBoard(final String channelId, final boolean isLoadboard) {
        if (isLoadboard) {
            loadBoardFromMercury(channelId);
        }
        whiteboardMercuryController.initMercury(channelId, false);
    }

    public void stopMercury() {
       whiteboardMercuryController.stopMercury();
    }

    /**
     * Separate method for mocking
     */
    @NonNull
    protected MercuryClient createWhiteboardMercuryClient() {
        return new WhiteboardMercuryClient(apiClientProvider, apiTokenProvider, gson, bus,
                deviceRegistration, settings, userAgentProvider,
                trackingIdGenerator, activityListener, lnContext, this);
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

    public WhiteboardShareDelegate getWhiteboardShareDelegate() {
        return shareDelegate;
    }

    public void share() {
        shareDelegate.share(getCurrentChannel(), getAclId());
    }

    public void saveWhiteboardsInBoundConversation(List<Channel> boards) {
        //All boards must have at least: boardId and kmsResourceUrl
        for (final Channel board : boards) {
            whiteboardSavingDelegate.saveBoardInConversation(board, getAclId(), getRemoteStore(), new AbsRemoteWhiteboardStore.ClientCallback() {
                @Override
                public void onSuccess() {
                    bus.post(new WhiteboardChannelSavedEvent(board, true));
                }

                @Override
                public void onFailure(String errorMessage) {
                    bus.post(new WhiteboardChannelSavedEvent(board, false, errorMessage));
                }
            });
        }
        localStore.clearCache();
        numberOfLocalBoards = 0;
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
        whiteboardCreator.createComplete();
        bus.post(WhiteboardStateEvent.whiteboardPersistanceReady());
    }

    public void boardStart() {
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardStarted();
        }
    }

    @Deprecated
    public boolean hasMultipleRemoteBoards() {
        return hasMultipleRemoteBoards;
    }

    @Deprecated
    public boolean hasMultipleLocalBoards() {
        return hasMultipleLocalBoards;
    }

    public int getNumberOfLocalBoards() {
        return numberOfLocalBoards;
    }

    public int getNumberOfRemoteBoards() {
        return numberOfRemoteBoards;
    }

    public String getSharedBoardId() {
        return shareDelegate.getSharedBoardId();
    }

    public boolean isSharingBoard(String boardId) {
        String sharedBoardId = getSharedBoardId();
        return sharedBoardId != null && sharedBoardId.equals(boardId);
    }

    public void saveContents(List<Content> content, String id) {
        String boardId = getBoardId();
        if (boardId == null) {
            synchronized (pendingContentLock) {
                pendingContents.add(new PendingContent(content, id));
            }
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("writerId", id);
        json.addProperty("boardId", boardId);
        json.addProperty("action", WhiteboardConstants.SAVE_CONTENT);
        bus.post(new WhiteboardMercuryUpdateEvent(content, boardId, false));
        List<Content> encryptedContents = whiteboardEncryptor.encryptContent(content, getCurrentChannel().getDefaultEncryptionKeyUrl());

        if (encryptedContents != null) {
            getCurrentWhiteboardStore().saveContent(boardId, encryptedContents, json);
        } else {
            Ln.e("Unable to encrypt contents.");
        }
    }

    public void clearBoard() {
        String boardId = getBoardId();
        JsonObject json = new JsonObject();
        json.addProperty("boardId", boardId);
        json.addProperty("action", WhiteboardConstants.CLEAR_BOARD);
        getCurrentWhiteboardStore().clear(boardId, json);
        clearBoardSnapshot(getCurrentChannel());
    }

    public void unshareWhiteboard() {
        shareDelegate.stopShare();
    }

    public WhiteboardMercuryController getMercuryController() {
        return whiteboardMercuryController;
    }

    public interface OnWhiteboardEventListener {

        void onBoardStarted();

        void onBoardFinished();

        void onBoardError(WhiteboardError whiteboardError);

        void onSnapshotReceived(String image);
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
            Ln.d(TAG, "Network is connected");
            isLosingNetwork = false;
            bus.post(WhiteboardServiceEvent.changeWhiteboardServiceState(false));
            if (isReloadBoard) {
                loadBoard(getBoardId(), true);
            }
        } else {
            Ln.d(TAG, "Network is disconnected");
            isLosingNetwork = true;
        }
    }

    public void mercuryErrorEvent(ResetEvent event) {

        if (!isWhiteboardEnabled()) {
            return;
        }

        Ln.d("mercuryErrorEvent " + event.getCode());
        WhiteboardError whiteboardError = new WhiteboardError(WhiteboardError.ErrorData.LOSE_NETWORK_CONNECTION_ERROR);
        if (onWhiteboardEventListener != null) {
            onWhiteboardEventListener.onBoardError(whiteboardError);
        }
        bus.post(WhiteboardServiceEvent.changeWhiteboardServiceState(false));

    }

    /**
     * Overridden in sparkling
     */
    protected boolean isWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isWhiteboardEnabled();
    }

    public void setReloadBoard(boolean reloadBoard) {
        isReloadBoard = reloadBoard;
    }

    public boolean shouldReloadBoard() {
        return isReloadBoard;
    }

    public boolean isReadonlyMode() {
        return isReadonlyMode;
    }

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

    public synchronized void loadBoardList(String conversationId, int channelsLimit) {
        if (deviceRegistration.getFeatures().isWhiteboardWithAclEnabled()) {
            Uri aclUrlLink = getAclUrlLink();
            String aclUrl = aclUrlLink != null ? aclUrlLink.toString() : null;
            getCurrentWhiteboardStore().loadWhiteboardList(aclUrl, channelsLimit);
        } else {
            getCurrentWhiteboardStore().loadWhiteboardList(conversationId, channelsLimit);
        }
    }

    public void loadNextPageBoards(String conversationId, String url) {
        if (conversationId == null) {
            localStore.loadNextPageWhiteboards(conversationId, url);
        } else {
            remoteStore.loadNextPageWhiteboards(conversationId, url);
        }
    }

    public void loadWhiteboardsComplete(List<Channel> items, final String link, final boolean isLocalStore,
                                        final boolean isFirstPage) {
        if (isLocalStore) {
            hasMultipleLocalBoards = !items.isEmpty();
            numberOfLocalBoards = items.size();
        } else {
            hasMultipleRemoteBoards = !items.isEmpty();
            numberOfRemoteBoards = items.size();
        }

        try {
            for (Channel channel : items) {
                if (channel.getImage() != null && channel.getImage().getEncryptionKeyUrl() != null) {
                    KeyObject keyObject = keyManager.getBoundKey(channel.getImage().getEncryptionKeyUrl());
                    channel.getImage().decrypt(keyObject);
                }
            }
        } catch (ParseException e) {
            Ln.e("Unable to decrypt", e);
        } catch (IOException e) {
            Ln.e(e.getMessage(), e);
        }
        WhiteboardListReadyEvent event = new WhiteboardListReadyEvent(items, link, isLocalStore, isFirstPage);
        bus.post(event);
    }

    public void getChannelInfo(String channelId) {
        getCurrentWhiteboardStore().getChannelInfo(channelId);
    }

    public void getChannelComplete(Channel channel) {
        if (currentChannel != null && currentChannel.getDefaultEncryptionKeyUrl() == null) {
            currentChannel.setDefaultEncryptionKeyUrl(getDefaultConversationKeyUrl());
            inCallMercuryInit(channel);
        }
    }

    public void getChannelInfoComplete(final Channel channel) {
        WhiteboardChannelInfoReadyEvent event = new WhiteboardChannelInfoReadyEvent(channel);
        bus.post(event);
    }

    public void createBoardComplete(Channel channel, Uri keyUrl) {

        whiteboardCreator.createComplete();
        currentChannel = channel;

        if (channel.getDefaultEncryptionKeyUrl() == null) {
            channel.setDefaultEncryptionKeyUrl(keyUrl);
        }

        synchronized (pendingContentLock) {
            for (PendingContent pendingContent : pendingContents) {
                saveContents(pendingContent.getContents(), pendingContent.getId());
            }
            pendingContents.clear();
        }

        inCallMercuryInit(channel);
        bus.post(new WhiteboardCreationCompleteEvent());
    }

    public void loadBoardsError(WhiteboardError.ErrorData errorData) {
        bus.post(errorData);
    }

    public void createBoardError(WhiteboardError.ErrorData errorData) {
        whiteboardCreator.createComplete(WhiteboardError.ErrorData.CREATE_BOARD_ERROR);
    }

    public synchronized void setupWhiteboardServiceByConversationId(WhiteboardCreator.CreateData createData, final WhiteboardServiceAvailableHandler handler) {
        whiteboardCreator.setupWhiteboardServiceByConversationId(createData, handler);
    }

    private boolean hasReceivedRealtimeWhiteboard() {
        return whiteboardCache.containsKey(getBoardId()) ? false : true;
    }

    private void handleWhiteboardMessage(JsonObject msg) {
        whiteboardCache.put(getBoardId(), msg.toString());
        long updateInterval;
        if (imageUpdateInterval <= 0) {
            updateInterval = getCurrentChannel().getImageUpdateInterval();
        } else {
            updateInterval = imageUpdateInterval;
        }
        if (!uploadSnapshotWorking) {
            if (updateInterval > 0) {
                snapshotTask.doWork(updateInterval);
            } else {
                snapshotTask.doWork(DEFAULT_IMAGE_UPLOAD_INTERVAL);
            }
        }
    }

    public void setImageUpdateInterval(Long imageUpdateInterval) {
        this.imageUpdateInterval = imageUpdateInterval;
    }

    private void sendSnapShot(String image, File content) {
        if (sentWhiteboard.containsValue(image)) {
            snapshotTask.cancelTimer();
            return;
        }
        Ln.d("got image snapshot:" + image);
        sentWhiteboard.put(getBoardId(), image);
        uploadWhiteboardSnapshot(Uri.fromFile(content), getCurrentChannel());
    }


    private File parseSnapshot(String image) {
        FileOutputStream out = null;
        File content = null;
        try {
            byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            content = File.createTempFile("photo", ".jpg");
            out = new FileOutputStream(content);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Ln.e(e);
            }
            return content;
        }
    }

    class UploadSnapshotTask {

        private Timer uploadSnapshotTimer;
        private TimerTask uploadSnapshotTask;
        private boolean isTimerStarting = true;

        public synchronized void doWork(long period) {
            if (uploadSnapshotTimer == null) {
                uploadSnapshotTimer = new Timer();
                startTimer();
            }

            if (uploadSnapshotTask == null) {
                uploadSnapshotTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (isTimerStarting) {
                            if (hasReceivedRealtimeWhiteboard()) {
                                cancelTimer();
                            } else {
                                Ln.d("start get snapshot in timer");
                                getImageSnapshotForUpload();
                            }
                        }
                    }
                };
            }

            if (uploadSnapshotTimer != null && uploadSnapshotTask != null) {
                uploadSnapshotTimer.scheduleAtFixedRate(uploadSnapshotTask, period, period);
                uploadSnapshotWorking = true;
            }
        }

        public synchronized void startTimer() {
            this.isTimerStarting = true;
            uploadSnapshotWorking = true;
        }

        public synchronized void cancelTimer() {
            isTimerStarting = false;
            uploadSnapshotWorking = false;
            if (uploadSnapshotTimer != null) {
                uploadSnapshotTimer.cancel();
                uploadSnapshotTimer = null;
            }

            if (uploadSnapshotTask != null) {
                uploadSnapshotTask.cancel();
                uploadSnapshotTask = null;
            }
        }
    }

    public interface WhiteboardServiceAvailableHandler {
        void onWhiteboardServiceAvailable(String convId, Uri keyUrl, Uri aclUrlLink);
    }
}
