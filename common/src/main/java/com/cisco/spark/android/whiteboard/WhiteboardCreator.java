package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.KmsKeyEvent;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public class WhiteboardCreator implements Component {

    public static final int KEY_REQUEST_TIMEOUT = 10;

    private final EncryptedConversationProcessor conversationProcessor;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final DeviceRegistration deviceRegistration;
    private final ApiClientProvider apiClientProvider;
    private final WhiteboardService whiteboardService;
    private final SchedulerProvider schedulerProvider;
    private final KeyManager keyManager;
    private final SdkClient sdkClient;
    private final Injector injector;
    private final Context context;
    private final EventBus bus;

    private boolean running;

    // FIXME This needs to be a queue
    private CreateData currentCreateRequest;

    private Executor singleThreadedExecutor = Executors.newSingleThreadExecutor();

    private Subscription keyFetchTimer;

    public WhiteboardCreator(DeviceRegistration deviceRegistration, ApiClientProvider apiClientProvider,
                             WhiteboardService whiteboardService, SchedulerProvider schedulerProvider, KeyManager keyManager,
                             SdkClient sdkClient, EventBus bus, EncryptedConversationProcessor conversationProcessor,
                             Injector injector, Context context, AuthenticatedUserProvider authenticatedUserProvider) {

        this.apiClientProvider = apiClientProvider;
        this.sdkClient = sdkClient;

        this.conversationProcessor = conversationProcessor;
        this.deviceRegistration = deviceRegistration;
        this.whiteboardService = whiteboardService;
        this.schedulerProvider = schedulerProvider;
        this.keyManager = keyManager;
        this.bus = bus;
        this.injector = injector;
        this.context = context;
        this.authenticatedUserProvider = authenticatedUserProvider;
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

    public synchronized void createBoard(CreateData createData) {

        if (currentCreateRequest != null) {
            Ln.v("Already performing a create, refusing to do another");
            return;
        }

        create(createData);
    }

    @SuppressWarnings("ConstantConditions")
    private synchronized void create(CreateData createData) {

        currentCreateRequest = createData;

        if (createData.conversationId != null) {
            if (!aclWhiteboardEnabled()) {
                createBoardWithConversationId(createData);
            } else {
                if (createData.aclUrlLink == null) {
                    setupWhiteboardServiceByConversationId(createData, null);
                    return;
                }
                createBoardWithAcl(createData);
            }
        } else if (createData.conversationId == null && sdkClient.supportsPrivateBoards()) {
            createPrivateBoard(createData);
        } else if (createData.conversationId == null && createData.aclUrlLink != null) {
            createBoardWithAcl(createData);
        } else {
            Ln.e("Unable to create non-private whiteboard with no conversation Id");
            currentCreateRequest = null;
        }
    }

    private boolean aclWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isWhiteboardWithAclEnabled();
    }

    private void createPrivateBoard(final CreateData createData) {

        KeyObject unboundKey = keyManager.getCachedUnboundKey();
        if (unboundKey == null) {
            startKeyFetchTimeout();
            return;
        }

        // Darling local case
        Observable.just(unboundKey).subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                  .subscribe(new Action1<KeyObject>() {
                      @Override
                      public void call(KeyObject key) {

                          final String userId = authenticatedUserProvider.getAuthenticatedUserOrNull().getUserId();
                          List<String> userIds = new ArrayList<>();
                          userIds.add(userId);
                          boolean requestSent = createChannelHelper(userIds, key, createData, true, whiteboardService.getPrivateStore());
                          if (!requestSent) {
                              createFailed("Failed to send create private channel request");
                          }
                      }
                  }, new Action1<Throwable>() {
                      @Override
                      public void call(Throwable throwable) {
                          Ln.e(throwable);
                          createFailed("Exception thrown while trying to send create private channel request");
                      }
                  });
    }

    @Deprecated
    public synchronized void createBoardWithConversationId(final CreateData createData) {

        if (createData.keyUrl != null) {

            Channel channelRequest = new Channel(createData.conversationId);
            channelRequest.setDefaultEncryptionKeyUrl(createData.keyUrl);
            whiteboardService.getRemoteStore().createChannel(channelRequest);

        } else {

            Observable.just(createData.conversationId).subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                      .map(new Func1<String, Uri>() {
                          @Override
                          public Uri call(String s) {
                              return getConversationKeyUrl(createData.conversationId);
                          }
                      }).subscribe(new Action1<Uri>() {
                @Override
                public void call(Uri uri) {
                    Channel channelRequest = new Channel(createData.conversationId);
                    channelRequest.setDefaultEncryptionKeyUrl(uri);
                    boolean requestSent = whiteboardService.getRemoteStore().createChannel(channelRequest);
                    if (!requestSent) {
                        createFailed("Failed to send create conversation channel request");
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Ln.e(throwable);
                    createFailed("Exception thrown while trying to send create conversation channel request");
                }
            });
        }
    }

    public ConversationResolver getConversationResolver(String conversationId) {
        return ConversationContentProviderQueries.getConversationResolverById(context.getContentResolver(),
                                                                              conversationId, injector);
    }

    protected Uri getConversationKeyUrl(String conversationId) {
        if (conversationId == null) {
            return null;
        }

        Uri keyUrl = null;
        ConversationResolver resolver = getConversationResolver(conversationId);

        if (resolver == null) {
            try {
                Response<Conversation> response = apiClientProvider.getConversationClient().getConversation(conversationId).execute();
                if (response.isSuccessful()) {
                    keyUrl = response.body().getDefaultActivityEncryptionKeyUrl();
                } else {
                    Ln.w("Failed getting key url " + LoggingUtils.toString(response));
                }
            } catch (IOException e) {
                Ln.w(e);
            }
        } else {
            keyUrl = resolver.getDefaultEncryptionKeyUrl();
        }
        return keyUrl;
    }

    private boolean createChannelHelper(List<String> userIds, KeyObject key, CreateData createData, boolean useAcl,
                                        WhiteboardStore store) {

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

        if (useAcl && createData.aclUrlLink != null) {
            channelRequest.setAclUrlLink(createData.aclUrlLink);
        }

        store.createChannel(channelRequest);
        return true;
    }

    void createFailed(String message) {
        Ln.e(message);
        createComplete(WhiteboardError.ErrorData.CREATE_BOARD_ERROR);
    }

    public synchronized void createBoardWithAcl(final CreateData createData) {

        if (createData.aclUrlLink != null) {
            whiteboardService.setAclUrlLink(createData.aclUrlLink);
        } else {
            Ln.e("can't create acl board without acl url");
            return;
        }

        KeyObject unboundKey = keyManager.getCachedUnboundKey();
        if (unboundKey == null) {
            startKeyFetchTimeout();
            return;
        }

        Observable.just(unboundKey)
                  .subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                  .subscribe(new Action1<KeyObject>() {
                      @Override
                      public void call(KeyObject key) {
                          List<String> userIds = new ArrayList<>();
                          Uri parentkmsResourceObjectUrl = whiteboardService.getParentkmsResourceObjectUrl();
                          if (parentkmsResourceObjectUrl != null) {
                              String kmsObjectUrl = parentkmsResourceObjectUrl.toString();
                              userIds = Collections.singletonList(kmsObjectUrl);
                          }
                          boolean requestSent = createChannelHelper(userIds, key, createData, true,
                                                                    whiteboardService.getRemoteStore());
                          if (!requestSent) {
                              createFailed("Failed to send create acl channel request");
                          }
                      }
                  }, new Action1<Throwable>() {
                      @Override
                      public void call(Throwable throwable) {
                          Ln.e(throwable);
                          createFailed("Exception thrown while trying to send create private channel request");
                      }
                  });
    }

    private void startKeyFetchTimeout() {
        Ln.e("No unbound keys, retrying later");
        keyFetchTimer = Observable.timer(KEY_REQUEST_TIMEOUT, TimeUnit.SECONDS, schedulerProvider.computation())
                                  .subscribe(new Action1<Long>() {
                                 @Override
                                 public void call(Long aLong) {
                                     createFailed("Create timer triggered before create complete");
                                 }
                             }, new Action1<Throwable>() {
                                 @Override
                                 public void call(Throwable throwable) {
                                     Ln.e(throwable);
                                     createFailed("Create cancel timer failed, aborting current create request");
                                 }
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

    public synchronized void setupWhiteboardServiceByConversationId(CreateData createData, final WhiteboardService.WhiteboardServiceAvailableHandler handler) {

        whiteboardService.setAclId(createData.conversationId);
        if (!aclWhiteboardEnabled()) {
            handler.onWhiteboardServiceAvailable(createData.conversationId, createData.keyUrl, createData.aclUrlLink);
            return;
        }

        UpdateWhiteboardServiceTask updateTask = new UpdateWhiteboardServiceTask(whiteboardService, handler, context, injector);
        updateTask.execute(createData.conversationId);
    }

    public synchronized void createComplete() {

        if (keyFetchTimer != null && !keyFetchTimer.isUnsubscribed()) {
            // Kill the key fetch timer
            keyFetchTimer.unsubscribe();
        }

        currentCreateRequest = null;
    }

    public void createComplete(WhiteboardError.ErrorData errorType) {
        createComplete();
        bus.post(new WhiteboardError(errorType));
    }

    public static final class CreateData {

        String conversationId;
        Uri keyUrl;
        Uri aclUrlLink;

        public CreateData(String conversationId, Uri keyUrl, Uri aclUrlLink) {
            this.conversationId = conversationId;
            this.keyUrl = keyUrl;
            this.aclUrlLink = aclUrlLink;
        }
    }
}
