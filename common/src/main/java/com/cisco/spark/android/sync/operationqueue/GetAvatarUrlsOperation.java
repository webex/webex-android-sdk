package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.GetAvatarUrlsRequest;
import com.cisco.spark.android.model.GetAvatarUrlsResponse;
import com.cisco.spark.android.model.SingleUserAvatarUrlsInfo;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class GetAvatarUrlsOperation extends Operation {

    private GetAvatarUrlsRequest getAvatarUrlsRequest;
    private Map<Uri, ContentManager.CacheRecordRequestParameters> parametersMap = new HashMap<>();

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient Gson gson;

    @Inject
    transient EventBus bus;

    public GetAvatarUrlsOperation(Injector injector, @NonNull ContentManager.CacheRecordRequestParameters parameters) {
        super(injector);
        getAvatarUrlsRequest = new GetAvatarUrlsRequest(parameters.getUuidOrEmail());
        parametersMap.put(parameters.getRemoteUri(), parameters);
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
        Type mapType = new TypeToken<Map<String, Map<Integer, SingleUserAvatarUrlsInfo>>>() {
        }.getType();
        Map<String, Map<Integer, SingleUserAvatarUrlsInfo>> result = gson.fromJson(jsonObject, mapType);

        GetAvatarUrlsResponse avatarUrlsResponse = new GetAvatarUrlsResponse();
        avatarUrlsResponse.setAvatarUrlsMap(result);
        avatarUrlsResponse.setParametersMap(parametersMap);
        bus.post(avatarUrlsResponse);

        return SyncState.SUCCEEDED;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.GET_AVATAR_URLS) {
            return false;
        }

        if (getState().isPreExecute()) {
            GetAvatarUrlsOperation getAvatarUrlsOperation = (GetAvatarUrlsOperation) newOperation;
            // API will auto merge those same request info
            getAvatarUrlsRequest.getAvatarsList().addItems(getAvatarUrlsOperation.getAvatarUrlsRequest.getAvatarsList().getItems());
            parametersMap.putAll(getAvatarUrlsOperation.parametersMap);

        } else {
            newOperation.setDependsOn(this);
        }

        return getState().isPreExecute();
    }
}
