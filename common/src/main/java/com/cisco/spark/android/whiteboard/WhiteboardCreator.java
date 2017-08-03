package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.events.KmsKeyEvent;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelType;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;

import static java.util.concurrent.TimeUnit.SECONDS;

public class WhiteboardCreator implements Component {

    private static final Pair<Integer, TimeUnit> KEY_REQUEST_TIMEOUT = new Pair<>(10, SECONDS);

    private final EncryptedConversationProcessor conversationProcessor;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ApiClientProvider apiClientProvider;
    private final WhiteboardService whiteboardService;
    private final SchedulerProvider schedulerProvider;
    private final WhiteboardCache whiteboardCache;
    private final JsonParser jsonParser;
    private final KeyManager keyManager;
    private final SdkClient sdkClient;

    private final EventBus bus;
    private final Gson gson;

    private boolean running;

    // FIXME These need to be re-worked to do multiple requests
    private CreateData currentCreateRequest;
    @Nullable private CreateCallback createCallback;

    private Executor createThread = Executors.newSingleThreadExecutor();

    private Subscription keyFetchTimer;

    public WhiteboardCreator(ApiClientProvider apiClientProvider, WhiteboardService whiteboardService, SchedulerProvider schedulerProvider,
                             WhiteboardCache whiteboardCache, KeyManager keyManager, SdkClient sdkClient, EventBus bus,
                             EncryptedConversationProcessor conversationProcessor,
                             AuthenticatedUserProvider authenticatedUserProvider, Gson gson) {

        this.apiClientProvider = apiClientProvider;
        this.whiteboardCache = whiteboardCache;
        this.sdkClient = sdkClient;

        this.conversationProcessor = conversationProcessor;
        this.whiteboardService = whiteboardService;
        this.schedulerProvider = schedulerProvider;
        this.keyManager = keyManager;
        this.bus = bus;
        this.gson = gson;

        this.authenticatedUserProvider = authenticatedUserProvider;

        this.jsonParser = new JsonParser();
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        running = true;
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }
    }

    public synchronized boolean createBoard(CreateData createData, CreateCallback callback) {

        if (currentCreateRequest != null) {
            Ln.v("Already performing a create, refusing to do another");
            return false;
        }

        createCallback = callback;

        create(createData);
        return true;
    }

    public synchronized boolean createBoard(CreateData createData) {

        if (currentCreateRequest != null) {
            Ln.v("Already performing a create, refusing to do another");
            return false;
        }
        create(createData);
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    private synchronized void create(CreateData createData) {

        currentCreateRequest = createData;

        if (createData.getAclUrlLink() != null) {
            createBoardWithAcl(createData);
        } else if (sdkClient.supportsPrivateBoards()) {
            createPrivateBoard(createData);
        } else {
            Ln.e("Unable to create non-private whiteboard with no conversation Id");
            currentCreateRequest = null;
            if (createCallback != null) {
                createCallback.onFail(createData.getId());
            }
        }
    }

    private void createPrivateBoard(final CreateData createData) {

        KeyObject unboundKey = keyManager.getCachedUnboundKey();
        if (unboundKey == null) {
            startKeyFetchTimeout();
            return;
        }

        // Darling local case
        Observable.just(unboundKey).subscribeOn(schedulerProvider.from(createThread))
                  .subscribe(key -> {

                      final String userId = authenticatedUserProvider.getAuthenticatedUserOrNull().getUserId();
                      List<String> userIds = new ArrayList<>();
                      userIds.add(userId);
                      boolean requestSent = createChannelHelper(userIds, key, createData, true);
                      if (!requestSent) {
                          createFailed("Failed to send create private channel request");
                      }
                  }, throwable -> {
                      Ln.e(throwable);
                      createFailed("Exception thrown while trying to send create private channel request");
                  });
    }

    private boolean createChannelHelper(List<String> userIds, KeyObject key, CreateData createData, boolean useAcl) {

        String kmsMessage = conversationProcessor.createNewResource(userIds, Collections.singletonList(key.getKeyId()));

        if (TextUtils.isEmpty(kmsMessage)) {
            Ln.e("Trying to create an ACL board without providing a KMS message");
            return false;
        }

        Channel channelRequest = new Channel();
        if (key.getKeyUrl() != null) {
            channelRequest.setDefaultEncryptionKeyUrl(key.getKeyUrl());
        }
        channelRequest.setKmsMessage(kmsMessage);
        channelRequest.setChannelType(createData.type);

        if (useAcl && createData.getAclUrlLink() != null) {
            channelRequest.setAclUrlLink(createData.getAclUrlLink());
        }

        return createChannel(channelRequest, createData.getId());
    }

    private synchronized void createBoardWithAcl(final CreateData createData) {

        if (createData.getAclUrlLink() != null) {
            whiteboardService.setAclUrlLink(createData.getAclUrlLink());
        } else {
            Ln.e("can't create acl board without acl url");
            return;
        }

        KeyObject unboundKey = keyManager.getCachedUnboundKey();
        if (unboundKey == null) {
            startKeyFetchTimeout();
            return;
        }

        bus.post(new WhiteboardCreationStartEvent());
        Observable.just(unboundKey)
                  .subscribeOn(schedulerProvider.from(createThread))
                  .subscribe(key -> {
                      List<String> userIds = new ArrayList<>();
                      Uri parentkmsResourceObjectUrl = whiteboardService.getParentkmsResourceObjectUrl();
                      if (parentkmsResourceObjectUrl != null) {
                          String kmsObjectUrl = parentkmsResourceObjectUrl.toString();
                          userIds = Collections.singletonList(kmsObjectUrl);
                      }
                      boolean requestSent = createChannelHelper(userIds, key, createData, true);
                      if (!requestSent) {
                          createFailed("Failed to send create acl channel request");
                      }
                  }, throwable -> {
                      Ln.e(throwable);
                      createFailed("Exception thrown while trying to send create private channel request");
                  });
    }

    private void startKeyFetchTimeout() {
        Ln.e("No unbound keys, retrying later");
        keyFetchTimer = Observable.timer(KEY_REQUEST_TIMEOUT.first, KEY_REQUEST_TIMEOUT.second, schedulerProvider.computation())
                                  .subscribe(aLong -> createFailed("Create timer triggered before create complete"),
                                             throwable -> {
                                                 Ln.e(throwable);
                                                 createFailed("Create cancel timer failed, aborting current create request");
                                             });
    }

    @SuppressWarnings("UnusedDeclaration")
    public synchronized void onEventAsync(KmsKeyEvent event) {

        if (!running || currentCreateRequest == null) {
            // There was no prior create request
            return;
        }

        create(currentCreateRequest);
    }

    public boolean createRequesting() {
        return currentCreateRequest != null;
    }

    @CheckResult
    public boolean createChannel(Channel channelRequest, UUID channelCreationRequestId) {
        return createChannel(channelRequest, channelCreationRequestId, null);
    }

    private Call<Channel> createCreateChannelCall(Channel channel) {

        WhiteboardPersistenceClient whiteboardPersistenceClient = apiClientProvider.getWhiteboardPersistenceClient();

        // Create whiteboard with OpenSpaceUrl and HiddenSpaceUrl
        // Always create HiddenSpaceUrl
        // Create OpenSpaceUrl according to the whiteboard type
        boolean createOpenSpace = channel.getType() == ChannelType.ANNOTATION;

        if (channel.isPrivateChannel() && sdkClient.supportsPrivateBoards()) {
            Ln.d("Creating private channel");
            return whiteboardPersistenceClient.createPrivateChannel(createOpenSpace, true, channel);
        } else {
            Ln.d("Creating conversation-backed channel");
            return whiteboardPersistenceClient.createChannel(createOpenSpace, true, channel);
        }
    }

    @CheckResult
    public boolean createChannel(final Channel channel, final UUID channelCreationRequestId, @Nullable final List<Content> content) {

        if (channel == null) {
            return false;
        }

        if (channel.getDefaultEncryptionKeyUrl() == null) {
            return false;
        }

        Call<Channel> call = createCreateChannelCall(channel);
        call.enqueue(new Callback<Channel>() {
            @Override
            public void onResponse(Call<Channel> call, Response<Channel> response) {
                if (response.isSuccessful()) {
                    processCreateChannelResponse(response, content);
                    whiteboardService.createBoardComplete(response.body(), channelCreationRequestId, channel.getDefaultEncryptionKeyUrl());

                    if (createCallback != null) {
                        createCallback.onSuccess(currentCreateRequest.getId(), response.body());
                        createCallback = null;
                    }
                    createComplete();
                } else {
                    createFailed(WhiteboardError.ErrorData.NETWORK_ERROR, String.valueOf(response.code()));
                    whiteboardService.boardReady();
                }
            }

            @Override
            public void onFailure(Call<Channel> call, Throwable t) {
                createFailed(WhiteboardError.ErrorData.CREATE_BOARD_ERROR, WhiteboardError.ErrorData.NETWORK_ERROR.name());
            }
        });

        return true;
    }

    protected void processCreateChannelResponse(Response<Channel> response, @Nullable List<Content> content) {
        if (response.isSuccessful()) {
            Channel channelResult = response.body();
            Ln.d("Created board %s", channelResult.getChannelId());
            whiteboardService.setCurrentChannel(channelResult);

            if (channelResult.getAclUrl() != null && !whiteboardService.isLocal()) {
                whiteboardService.loadBoard(channelResult.getChannelId(), false);
            }

            List<Stroke> strokeList = new ArrayList<>();
            if (content != null) {
                Observable.just(1)
                        .subscribeOn(schedulerProvider.from(whiteboardService.getSaveContentExecutor()))
                        .subscribe(integer -> {
                            whiteboardService.getChannelWhiteboardStore(channelResult).saveContent(channelResult, content, null);
                        }, Ln::e);
                for (Content c : content) {
                    Stroke stroke = WhiteboardUtils.createStroke(c, jsonParser, gson);
                    if (stroke != null) {
                        strokeList.add(stroke);
                    }
                }
            }
            whiteboardCache.initAndStartRealtimeForBoard(channelResult.getChannelId(), strokeList);
        } else {
            createFailed(WhiteboardError.ErrorData.CREATE_BOARD_ERROR, String.valueOf(response.code()));
        }
    }

    private void createFailed(String message) {
        Ln.e(message);
        if (createCallback != null) {
            createCallback.onFail(currentCreateRequest.getId());
            createCallback = null;
        }
        createFailed(WhiteboardError.ErrorData.CREATE_BOARD_ERROR, message);
    }


    private void sendBoardError(WhiteboardError whiteboardError) {
        WhiteboardService.OnWhiteboardEventListener boardListener = whiteboardService.getOnWhiteboardEventListener();

        if (boardListener != null) {
            boardListener.onBoardError(whiteboardError);
        }
    }

    private void createFailed(WhiteboardError.ErrorData errorType, String errorMsg) {
        WhiteboardOriginalData originalData = new WhiteboardOriginalData(errorType, null);
        WhiteboardError whiteboardError = new WhiteboardError(errorType, originalData, errorMsg);
        sendBoardError(whiteboardError);
        bus.post(new WhiteboardError(errorType));
        whiteboardService.boardReady();
        createComplete();
    }

    private synchronized void createComplete() {

        if (keyFetchTimer != null && !keyFetchTimer.isUnsubscribed()) {
            // Kill the key fetch timer
            keyFetchTimer.unsubscribe();
        }

        currentCreateRequest = null;
    }

    public static final class CreateData {

        final UUID uuid;

        final WhiteboardService.WhiteboardContext context;

        ChannelType type;

        public CreateData(WhiteboardService.WhiteboardContext context) {
            this.uuid = UUID.randomUUID();
            this.context = context;
            this.type = ChannelType.WHITEBOARD;
        }

        @Nullable
        public Uri getAclUrlLink() {
            return context == null ? null : context.getAclUrl();
        }

        public void setType(ChannelType type) {
            this.type = type;
        }

        public boolean isAnnotation() {
            return type == ChannelType.ANNOTATION;
        }

        public boolean isValid() {
            return context != null && context.getAclUrl() == null && context.getAclId() == null;
        }

        public UUID getId() {
            return uuid;
        }

        public String getConversationId() {
            return context == null ? null : context.getAclId();
        }
    }

    public interface CreateCallback {
        void onSuccess(UUID createID, Channel channel);
        void onFail(UUID uuid);
    }
}
