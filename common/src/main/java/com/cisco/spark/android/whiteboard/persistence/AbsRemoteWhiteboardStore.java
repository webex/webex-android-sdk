package com.cisco.spark.android.whiteboard.persistence;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardError;
import com.cisco.spark.android.whiteboard.WhiteboardListService;
import com.cisco.spark.android.whiteboard.WhiteboardOriginalData;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.WhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelItems;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.persistence.model.SpaceUrl;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;

import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WHITEBOARD_LOAD_BOARD_LIST_DELAY_MILLIS;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public abstract class AbsRemoteWhiteboardStore implements WhiteboardStore {

    public static final int MAX_CONTENT_BATCH_SIZE = 150;
    @Inject WhiteboardListCache whiteboardListCache;
    @Inject WhiteboardCache whiteboardCache;
    @Inject Clock clock;
    @Inject Gson gson;
    @Inject EventBus bus;

    protected final WhiteboardService whiteboardService;
    protected final ApiClientProvider apiClientProvider;
    protected final SchedulerProvider schedulerProvider;

    private Executor singleThreadedExecutor = Executors.newSingleThreadExecutor();
    private long lastUpdatedBoardListTimeMillis;

    private JsonParser jsonParser;

    private WhiteboardPersistenceClient whiteboardPersistenceClient;

    private Injector injector;
    private final Handler handler;
    private Runnable delayedLoadListRunnable;

    // Methods to override
    protected abstract Call<ChannelItems<Channel>> createGetChannelsCall(String conversationId, int channelLimit);
    protected abstract boolean isPrivateStore();

    public AbsRemoteWhiteboardStore(@NonNull WhiteboardService whiteboardService, @NonNull  ApiClientProvider apiClientProvider,
                                    @NonNull Injector injector, @NonNull SchedulerProvider schedulerProvider) {

        this.apiClientProvider = apiClientProvider;
        this.whiteboardService = whiteboardService;
        this.schedulerProvider = schedulerProvider;
        this.jsonParser = new JsonParser();
        this.injector = injector;
        injector.inject(this);

        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        if (handler != null && delayedLoadListRunnable != null) {
            handler.removeCallbacks(delayedLoadListRunnable);
        }
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @NonNull
    protected WhiteboardPersistenceClient getWhiteboardPersistenceClient() {
        if (whiteboardPersistenceClient == null) {
            whiteboardPersistenceClient = apiClientProvider.getWhiteboardPersistenceClient();
        }
        return whiteboardPersistenceClient;
    }

    @Override
    public void updateChannel(Channel updatedChannel) {
        Call<Channel> call = getWhiteboardPersistenceClient().updateChannel(updatedChannel.getChannelId(), updatedChannel);
        call.enqueue(new ProcessChannelResponse(null));
    }

    @Override
    public void patchChannel(Channel updatedChannel, @Nullable ClientCallback clientCallback) {
        Call<Channel> call = getWhiteboardPersistenceClient().patchChannel(updatedChannel.getChannelId(), updatedChannel);
        call.enqueue(new ProcessChannelResponse(clientCallback));
    }

    public Observable<Channel> getChannel(String channelId) {
        return getWhiteboardPersistenceClient().getChannelRx(channelId).subscribeOn(schedulerProvider.network());
    }

    @Override
    public void getChannelInfo(String channelId, @Nullable ChannelInfoCallback callback) {
        Call<Channel> call = getWhiteboardPersistenceClient().getChannel(channelId);
        if (call != null) {
            call.enqueue(new ProcessGetChannelInfoResponse(callback));
        }
    }

    @Deprecated
    @Override
    public void getChannelInfo(String channelId) {
        getChannelInfo(channelId, null);
    }

    @Override
    public void loadContent(final String channelId) {
        LoadWhiteboardContentTask.Callback callback = new LoadWhiteboardContentTask.Callback() {
            @Override
            public void onSuccess(String channelId, ContentItems contentItems) {
                whiteboardService.parseContentItems(channelId, contentItems.getItems(), false);
                whiteboardService.setReloadBoard(false);
                whiteboardService.setLoadingChannel(null);
                sendBoardContentLoaded();
            }

            @Override
            public void onFailure(String channelId, String errorMessage) {
                sendBoardError(WhiteboardError.ErrorData.LOAD_BOARD_ERROR, null, errorMessage);
                whiteboardService.boardReady();
                whiteboardService.setLoadingChannel(null);
            }
        };

        LoadWhiteboardContentTask task = new LoadWhiteboardContentTask(channelId, callback, injector);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    //TODO: we need to enable this method after webview support incremental loading or enable native
    //whiteboard both on Darling and client.
    //    @Override
    public void _loadContent(final String channelId) {
        Call<ContentItems> call = getWhiteboardPersistenceClient().getContents(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE);
        callLoadContent(channelId, call, true);
    }

    protected void loadNextContentBatch(final String nextUrl, final String channelId) {
        Call<ContentItems> call = getWhiteboardPersistenceClient().getContents(nextUrl);
        callLoadContent(channelId, call, true);
    }

    protected void callLoadContent(String channelId, Call<ContentItems> call, final boolean autoloadContentBatch) {
        call.enqueue(new Callback<ContentItems>() {
            @Override
            public void onResponse(Call<ContentItems> call, final Response<ContentItems> response) {
                if (response.isSuccessful()) {

                    final ContentItems result = response.body();
                    Observable.just(result)
                            .subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                            .subscribe(result1 -> {
                                whiteboardService.parseContentItems(channelId, result1.getItems(), false);
                                whiteboardService.setReloadBoard(false);
                            }, Ln::e);
                } else {
                    String message;
                    try {
                        message = response.errorBody().string();
                        Ln.e("Can't load the board, response = %s", message);
                    } catch (IOException e) {
                        Ln.e(e);
                    }
                    sendBoardError(WhiteboardError.ErrorData.LOAD_BOARD_ERROR, null, WhiteboardConstants.LOAD_BOARD_CONTENT_FAILURE);
                    boardReady();
                }
            }

            @Override
            public void onFailure(Call<ContentItems> call, Throwable t) {
                sendBoardError(WhiteboardError.ErrorData.NETWORK_ERROR);
                sendBoardError(WhiteboardError.ErrorData.LOAD_BOARD_ERROR, null, WhiteboardConstants.LOAD_BOARD_CONTENT_NETWORK_ISSUE);
                whiteboardService.boardReady();
            }
        });
    }

    @Override
    public synchronized void saveContent(Channel channel, List<Content> contentRequests, final JsonObject originalMessage) {

        if (contentRequests.size() <= MAX_CONTENT_BATCH_SIZE) {
            saveContentBatch(channel, contentRequests, originalMessage);
        } else {
            List<List<Content>> contentBatch = getContentBatch(contentRequests);
            for (List<Content> contents : contentBatch) {
                saveContentBatch(channel, contents, originalMessage);
            }
        }

    }

    @Override
    public void clearPartialContents(String channelId, List<Content> contentRequests, final JsonObject originalMessage) {
        Call<ContentItems> call = getWhiteboardPersistenceClient().clearPartialContents(channelId, contentRequests);
        call.enqueue(new retrofit2.Callback<ContentItems>() {
            @Override
            public void onResponse(Call<ContentItems> call, retrofit2.Response<ContentItems> response) {
                if (response.isSuccessful()) {
                    ContentItems contentItems = response.body();
                    JsonArray resultArray = new JsonArray();
                    for (Content content : contentItems.getItems()) {
                        Ln.d("clear partial contents");
                        resultArray.add(whiteboardService.createBridgeContentJson(content));
                    }
                    whiteboardService.createResponse(originalMessage, resultArray, null);
                } else {
                    whiteboardService.createResponse(originalMessage, new JsonObject(), response);
                    whiteboardService.setReloadBoard(true);
                    sendBoardError(WhiteboardError.ErrorData.CLEAR_PARTIAL_CONTENTS_ERROR, originalMessage);
                }
            }

            @Override
            public void onFailure(Call<ContentItems> call, Throwable t) {
                Ln.e("clear partial contents failed " + t.getMessage());
                // TODO retrofit2 error case?
                whiteboardService.setReloadBoard(true);
                sendBoardError(WhiteboardError.ErrorData.NETWORK_ERROR);
            }
        });
    }

    // Package local for testing
    List<List<Content>> getContentBatch(List<Content> contentRequests) {

        int index = 0;
        List<List<Content>> contentBatches = new ArrayList<>();

        if (contentRequests == null) {
            Ln.w("PRovided with null content batches, returning empty content");
            return contentBatches;
        }

        while (index * MAX_CONTENT_BATCH_SIZE < contentRequests.size()) {

            int start = index * MAX_CONTENT_BATCH_SIZE;
            int end = Math.min(start + MAX_CONTENT_BATCH_SIZE, contentRequests.size());
            contentBatches.add(contentRequests.subList(start, end));
            index++;
        }

        return contentBatches;
    }

    private void saveContentBatch(Channel channel, List<Content> contentRequests, final JsonObject originalMessage) {

        if (channel == null) {
            return;
        }
        Call<ContentItems> call = null;
        try {
            call = getWhiteboardPersistenceClient().addContents(channel.getChannelId(), contentRequests);
            Response<ContentItems> response = call.execute();
            if (response.isSuccessful()) {
                ContentItems contentItems = response.body();
                JsonArray resultArray = new JsonArray();
                for (Content content : contentItems.getItems()) {
                    Ln.d("save content create bridge content");
                    resultArray.add(whiteboardService.createBridgeContentJson(content));
                }
                whiteboardService.createResponse(originalMessage, resultArray, null);
                sendBoardError(WhiteboardError.ErrorData.NONE);
            } else {
                removeStrokeFromWhiteboardCache(originalMessage);
                whiteboardService.createResponse(originalMessage, new JsonObject(), response);
                whiteboardService.setReloadBoard(true);
                sendBoardError(WhiteboardError.ErrorData.SAVE_CONTENT_ERROR, originalMessage);
            }
        } catch (IOException e) {
            Ln.e("saveContent failed " + e.getMessage());
            removeStrokeFromWhiteboardCache(originalMessage);
            whiteboardService.createDefaultError(originalMessage);
            whiteboardService.setReloadBoard(true);
            sendBoardError(WhiteboardError.ErrorData.NETWORK_ERROR);
        }
    }

    private void removeStrokeFromWhiteboardCache(JsonObject msg) {

        if (msg == null) {
            Ln.w("Unable to remove whiteboard stroke from cache with null message");
            return;
        }

        String writerId = WhiteboardUtils.safeGetAsString(msg.get("writerId"), null);
        if (writerId != null) {
            whiteboardCache.removeStrokeFromCurrentRealtimeBoard(UUID.fromString(writerId));
        }
    }

    @Override
    public void clear(String channelId, final JsonObject originalMessage) {
        Call<Void> call = getWhiteboardPersistenceClient().deleteAllContents(channelId);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    whiteboardService.createResponse(originalMessage, new JsonObject(), null);
                } else {
                    sendBoardError(WhiteboardError.ErrorData.CLEAR_BOARD_ERROR);
                    whiteboardService.createDefaultError(originalMessage);
                    whiteboardService.setReloadBoard(true);
                    whiteboardService.createResponse(originalMessage, new JsonObject(), response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // TODO retrofit2 error case?
                sendBoardError(WhiteboardError.ErrorData.NETWORK_ERROR);
                whiteboardService.createDefaultError(originalMessage);
                whiteboardService.setReloadBoard(true);
            }
        });
    }

    @Override
    public void loadWhiteboardList(final String aclLink, final int channelsLimit, WhiteboardListService.WhiteboardListCallback callback) {

        WhiteboardListCache.WhiteboardListCacheEntry cachedEntry = whiteboardListCache.get(aclLink);
        boolean isCacheEntryEmpty = true;
        if (cachedEntry != null) {
            List<Channel> cachedList = cachedEntry.getChannels();
            if (!cachedList.isEmpty()) {
                isCacheEntryEmpty = false;
                callback.loadWhiteboardsComplete(new ArrayList<>(cachedList), cachedEntry.getLink(), isPrivateStore(), true);
            }
        }
        if (isBoardListObsolete() || isCacheEntryEmpty) {
            Ln.d("board list obsolete, performing full refresh");
            Call<ChannelItems<Channel>> call = createGetChannelsCall(aclLink, channelsLimit);
            processLoadListResponse(call, aclLink, true, callback);
        } else {

            Ln.d("Board list not obsolete, scheduling update");
            if (delayedLoadListRunnable != null) {
                handler.removeCallbacks(delayedLoadListRunnable);
            }

            delayedLoadListRunnable = () -> {
                Call<ChannelItems<Channel>> call = createGetChannelsCall(aclLink, channelsLimit);
                processLoadListResponse(call, aclLink, true, callback);
            };
            handler.postDelayed(delayedLoadListRunnable, WHITEBOARD_LOAD_BOARD_LIST_DELAY_MILLIS);
        }
    }

    @Override
    public void loadNextPageWhiteboards(String conversationId, String url, WhiteboardListService.WhiteboardListCallback callback) {
        Call<ChannelItems<Channel>> call = getWhiteboardPersistenceClient().getChannels(url);
        processLoadListResponse(call, conversationId, false, callback);
    }

    protected void processLoadListResponse(Call<ChannelItems<Channel>> call, final String conversationId, final boolean isFirstPage, WhiteboardListService.WhiteboardListCallback callback) {
        call.enqueue(new Callback<ChannelItems<Channel>>() {
            @Override
            public void onResponse(Call<ChannelItems<Channel>> call, final Response<ChannelItems<Channel>> response) {
                if (response.isSuccessful()) {
                    final ChannelItems<Channel> channelItems = response.body();
                    Headers header = response.headers();
                    String link = "";
                    if (header != null && header.get("Link") != null) {
                        link = header.get("Link").replaceAll("[<>;]", "").split(" ")[0];
                    }
                    List<Channel> items = channelItems.getItems();
                    decryptChannelImages(items);
                    callback.loadWhiteboardsComplete(items, link, isPrivateStore(), isFirstPage);
                    if (isFirstPage) {
                        whiteboardListCache.put(conversationId, items, link);
                    } else {
                        whiteboardListCache.add(conversationId, items, link);
                    }
                } else {
                    try {
                        String message = response.errorBody().string();
                        Ln.e("Load boards list failed, response = %s", message);
                        if (response.code() == HTTP_NOT_FOUND) {
                            //It's not necessarily a failure, the boards were probably just deleted
                            //So we send an empty list event anyway
                            callback.loadWhiteboardsComplete(new ArrayList<>(), "", isPrivateStore(),
                                                                      isFirstPage);
                        } else {
                            whiteboardService.loadBoardsError(WhiteboardError.ErrorData.LOAD_BOARD_LIST_ERROR);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ChannelItems<Channel>> call, Throwable t) {
                whiteboardService.loadBoardsError(WhiteboardError.ErrorData.NETWORK_ERROR);
            }
        });
    }

    protected void sendBoardError(WhiteboardError.ErrorData errorData, JsonObject originalMessage, String errorMessage) {
        WhiteboardOriginalData originalData = new WhiteboardOriginalData(errorData, originalMessage);
        WhiteboardError whiteboardError = new WhiteboardError(errorData, originalData, errorMessage);
        WhiteboardService.OnWhiteboardEventListener boardListener = whiteboardService.getOnWhiteboardEventListener();

        if (boardListener != null) {
            boardListener.onBoardError(whiteboardError);
        }
        bus.post(whiteboardError);
    }

    protected void sendBoardError(WhiteboardError.ErrorData errorData, JsonObject originalMessage) {
        sendBoardError(errorData, originalMessage, null);
    }

    protected void sendBoardError(WhiteboardError.ErrorData errorData) {
        sendBoardError(errorData, null);
    }

    protected void boardReady() {
        whiteboardService.boardReady();
        whiteboardService.setLoadingChannel(null);
    }

    protected void sendBoardContentLoaded() {
        WhiteboardService.OnWhiteboardEventListener boardListener = whiteboardService.getOnWhiteboardEventListener();
        if (boardListener != null) {
            boardListener.onBoardContentLoaded();
        }
    }

    @Override
    public void fetchHiddenSpaceUrl(final Channel channel, final OnHiddenSpaceUrlFetched listener) {
        final String channelId = channel.getChannelId();
        Call<SpaceUrl> call = getWhiteboardPersistenceClient().getHiddenSpace(channelId);
        call.enqueue(new retrofit2.Callback<SpaceUrl>() {
            @Override
            public void onResponse(Call<SpaceUrl> call, retrofit2.Response<SpaceUrl> response) {
                if (response.isSuccessful()) {
                    Uri hiddenSpaceUrl = response.body().getSpaceUrl();
                    if (hiddenSpaceUrl != null) {
                        listener.onSuccess(hiddenSpaceUrl);
                        return;
                    }
                }
                sendBoardError(WhiteboardError.ErrorData.GET_BOARD_SPACEURL_ERROR);
                listener.onFailure("Couldn't get or set the hiddenSpaceUrl");
            }

            @Override
            public void onFailure(Call<SpaceUrl> call, Throwable t) {
                sendBoardError(WhiteboardError.ErrorData.GET_BOARD_SPACEURL_ERROR);
                listener.onFailure("fetchHiddenSpaceUrl failed: " + t.getMessage());
            }
        });
    }

    protected boolean isBoardListObsolete() {
        final boolean isObsolete = clock.monotonicNow() - lastUpdatedBoardListTimeMillis > WHITEBOARD_LOAD_BOARD_LIST_DELAY_MILLIS;
        if (isObsolete)
            lastUpdatedBoardListTimeMillis = clock.monotonicNow();
        return isObsolete;
    }

    @Override
    public void clearCache() {
        whiteboardListCache.clear();
    }

    @Override
    public boolean removeFromCache(String channelId) {
        return whiteboardListCache.remove(channelId);
    }

    private class ProcessChannelResponse implements Callback<Channel> {
        private final ClientCallback mClientCallback;

        public ProcessChannelResponse(@Nullable ClientCallback clientCallback) {
            mClientCallback = clientCallback;
        }

        @Override
        public void onResponse(Call<Channel> call, Response<Channel> response) {
            if (response.isSuccessful()) {

                Ln.d(call.request().method() + " Channel successful");
                //TODO refactor this method, it should NOT always call setCurrentChannel
                Channel channel = response.body();

                if (!Strings.equalsIgnoreCase(call.request().method(), "patch")) {
                    whiteboardService.setCurrentChannel(channel);
                }
                whiteboardService.getChannelInfoComplete(channel);
                whiteboardService.getChannelComplete(channel);

                whiteboardListCache.update(channel);

                if (mClientCallback != null)
                    mClientCallback.onSuccess(channel);
            } else {
                String message = "";
                try {
                    message = response.errorBody().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // If saving a private board fails, we need to decrypt the kms message from the acl service
                if (Strings.equalsIgnoreCase(call.request().method(), "patch") && !Strings.isEmpty(message)) {
                    String msg = parseKmsError(message);
                    if (msg != null) {
                        Ln.e(new RuntimeException(msg));
                    } else {
                        Ln.e("Failed to decrypt KMS message");
                    }
                }

                String errorMessage = "Can't "  + call.request().method() + " the board, response:  " + message;
                Ln.e(errorMessage);

                sendBoardError(getErrorData(call.request().method()));
                whiteboardService.createDefaultError(new JsonObject());

                if (mClientCallback != null) {
                    mClientCallback.onFailure(errorMessage);
                }
            }
        }

        @Override
        public void onFailure(Call<Channel> call, Throwable t) {
            Ln.e("Can't "  + call.request().method() + " the board response = %s", t.getMessage());
            sendBoardError(getErrorData(call.request().method()));
            whiteboardService.createDefaultError(new JsonObject());
        }

        private WhiteboardError.ErrorData getErrorData(String method) {
            WhiteboardError.ErrorData err = null;
            switch (method) {
                case "GET":
                    err = WhiteboardError.ErrorData.GET_BOARD_ERROR;
                    break;
                case "UPDATE":
                    err = WhiteboardError.ErrorData.UPDATE_BOARD_ERROR;
                    break;
                case "PATCH":
                    err = WhiteboardError.ErrorData.PATCH_BOARD_ERROR;
                    break;
            }
            return err;
        }
    }

    @Nullable
    private String parseKmsError(String message) {

        try {
            JsonElement json = jsonParser.parse(message);
            String errorMessage = json.getAsJsonObject().get("message").getAsString();
            int errorIndex  = errorMessage.indexOf("error = '");
            if (errorIndex != -1) {
                String kmsErrorMessage = errorMessage.substring(errorIndex + 9, errorMessage.indexOf("')"));
                KmsResponseBody kmsResponseBody = CryptoUtils.decryptKmsMessage(kmsErrorMessage,
                                                                                whiteboardService.getKeyManager()
                                                                                                 .getSharedKeyAsJWK());

                if (kmsResponseBody != null) {
                    return kmsResponseBody.getReason();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private class ProcessGetChannelInfoResponse implements Callback<Channel> {
        private final ChannelInfoCallback mCallback;

        public ProcessGetChannelInfoResponse(@Nullable ChannelInfoCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResponse(Call<Channel> call, Response<Channel> response) {
            if (response.isSuccessful()) {
                Ln.d(call.request().method() + " Channel successful");
                Channel channel = response.body();

                decryptChannelImages(Arrays.asList(channel));

                whiteboardService.getChannelInfoComplete(channel);
                whiteboardListCache.update(channel);

                if (mCallback != null) {
                    mCallback.onSuccess(channel);
                }
            } else {
                String message = "";
                try {
                    message = response.errorBody().string();
                    Ln.e("Can't "  + call.request().method() + " the board, response = %s", message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mCallback != null) {
                    mCallback.onFailure(message);
                }
            }
        }

        @Override
        public void onFailure(Call<Channel> call, Throwable t) {
            Ln.e("Can't "  + call.request().method() + " the board response = %s", t.getMessage());
        }
    }

    private void decryptChannelImages(List<Channel> channels) {
        for (Channel channel : channels) {
            if (channel.getImage() != null && channel.getImage().getEncryptionKeyUrl() != null) {
                whiteboardService.getKeyManager().getBoundKeyDelaySync(channel.getImage().getEncryptionKeyUrl()).subscribe(ko -> {
                    try {
                        channel.getImage().decrypt(ko);
                    } catch (IOException e) {
                        Ln.e(e);
                    } catch (ParseException e) {
                        Ln.e(e);
                    }
                }, Ln::e);
            }
        }
    }

    public interface ClientCallback {
        void onSuccess(Channel channel);
        void onFailure(String errorMessage);
    }

    public interface ChannelInfoCallback {
        void onSuccess(Channel channel);
        void onFailure(String errorMessage);
    }
}
