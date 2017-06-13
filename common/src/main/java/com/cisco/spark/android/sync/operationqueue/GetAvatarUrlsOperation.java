package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AvatarProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.GetAvatarUrlsRequest;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.model.GetAvatarUrlsRequest.SingleAvatarUrlRequestInfo;
import static com.cisco.spark.android.model.GetAvatarUrlsResponse.SingleAvatarInfo;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class GetAvatarUrlsOperation extends Operation {

    private GetAvatarUrlsRequest getAvatarUrlsRequest;
    private ConcurrentHashMap<String, List<Action<ContentDataCacheRecord>>> callbackMap = new ConcurrentHashMap<>();

    @Inject
    transient ApiClientProvider apiClientProvider;
    @Inject
    transient AvatarProvider avatarProvider;
    @Inject
    transient Gson gson;

    public GetAvatarUrlsOperation(Injector injector, String uuid, Action<ContentDataCacheRecord> action) {
        super(injector);
        SingleAvatarUrlRequestInfo singleAvatarUrlRequestInfo = new SingleAvatarUrlRequestInfo(uuid);
        initialize(singleAvatarUrlRequestInfo, action);
    }

    private void initialize(SingleAvatarUrlRequestInfo singleAvatarUrlRequestInfo, Action<ContentDataCacheRecord> action) {
        this.getAvatarUrlsRequest = new GetAvatarUrlsRequest(singleAvatarUrlRequestInfo);
        if (action != null) {
            List<Action<ContentDataCacheRecord>> callbackList = new ArrayList<>();
            callbackList.add(action);
            callbackMap.put(singleAvatarUrlRequestInfo.getUuid(), callbackList);
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.SECONDS)
                .withRetryDelay(0)
                .withMaxAttempts(3);
    }

    @Override
    public OperationType getOperationType() {
        return OperationType.GET_AVATAR_URLS;
    }

    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @Override
    protected SyncState doWork() throws IOException {
        Response response = apiClientProvider.getAvatarClient().getAvatarUrls(getAvatarUrlsRequest.getAvatarsList().getItems()).execute();
        if (!response.isSuccessful() || response.body() == null) {
            Ln.e("Failed to get avatars url: %s", LoggingUtils.toString(response));
            return SyncState.READY;
        }

        JsonObject jsonObject = (JsonObject) response.body();
        Type mapType = new TypeToken<Map<String, Map<Integer, SingleAvatarInfo>>>() { }.getType();
        Map<String, Map<Integer, SingleAvatarInfo>> result = gson.fromJson(jsonObject, mapType);

        Batch batch = newBatch();

        Map<Integer, SingleAvatarInfo> userAvatarsMap;
        ContentDataCacheRecord record;
        List<Action<ContentDataCacheRecord>> callbackList;

        for (String uuid : result.keySet()) {
            userAvatarsMap = result.get(uuid);
            for (Integer size : userAvatarsMap.keySet()) {
                record = genCacheRecord(uuid, size, userAvatarsMap.get(size));
                batch.add(ContentProviderOperation.newInsert(ContentDataCacheEntry.CONTENT_URI).withValues(record.getContentValues()).build());
            }
        }

        batch.apply();

        for (String uuid : result.keySet()) {
            callbackList = callbackMap.get(uuid);
            if (callbackList != null && callbackList.size() > 0) {
                for (Action<ContentDataCacheRecord> callback : callbackList) {
                    callback.call(null);
                }
            }
        }

        return SyncState.SUCCEEDED;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.GET_AVATAR_URLS) {
            return false;
        }

        if (getState().isPreExecute()) {
            GetAvatarUrlsOperation getAvatarUrlsOperation = (GetAvatarUrlsOperation) newOperation;
            // api will auto merge those same request info
            getAvatarUrlsRequest.getAvatarsList().addItems(getAvatarUrlsOperation.getAvatarUrlsRequest.getAvatarsList().getItems());
            for (String key : getAvatarUrlsOperation.callbackMap.keySet()) {
                if (callbackMap.containsKey(key)) {
                    Ln.d("GetAvatarUrls merge callbackMap for key " + key);
                    callbackMap.get(key).addAll(getAvatarUrlsOperation.callbackMap.get(key));
                } else {
                    callbackMap.put(key, getAvatarUrlsOperation.callbackMap.get(key));
                }
            }
        } else {
            newOperation.setDependsOn(this);
        }

        return getState().isPreExecute();
    }

    private ContentDataCacheRecord genCacheRecord(String uuid, Integer size, SingleAvatarInfo singleAvatarInfo) {
        ContentDataCacheRecord record = new ContentDataCacheRecord();

        // request s=35 but server will return 40 as size.
        Uri remoteURI = avatarProvider.getUri(uuid, String.valueOf(size));
        record.setRemoteUri(remoteURI);

        record.setCacheType(ContentDataCacheEntry.Cache.AVATAR);
        record.setLastAccessTime(System.currentTimeMillis());
        if (!singleAvatarInfo.isDefaultAvatar() && !TextUtils.isEmpty(singleAvatarInfo.getUrl())) {
            record.setRealUri(Uri.parse(singleAvatarInfo.getUrl()));
        }

        return record;
    }
}
