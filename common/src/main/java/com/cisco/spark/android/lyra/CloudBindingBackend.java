package com.cisco.spark.android.lyra;

import android.net.Uri;
import com.cisco.spark.android.client.LyraClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.operationqueue.RoomBindOperation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.github.benoitdion.ln.Ln;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CloudBindingBackend implements BindingBackend {

    private LyraClient lyraClient;
    private ApiClientProvider apiClientProvider;
    private OperationQueue operationQueue;
    private Injector injector;

    public CloudBindingBackend(ApiClientProvider apiClientProvider, EventBus bus, Injector injector, OperationQueue operationQueue) {
        this.apiClientProvider = apiClientProvider;
        this.operationQueue = operationQueue;
        this.injector = injector;
    }

    @Override
    public void bind(Uri conversationUrl, String roomIdentity, String conversationId) {
        operationQueue.submit(new RoomBindOperation(injector, conversationUrl, roomIdentity, conversationId));
    }

    @Override
    public void unbind(final String roomIdentity, final String  bindingUrl, final String kmsMessage, final BindingCallback callback) {
        Call<Void> call = getLyraClient().unbind(roomIdentity, bindingUrl, kmsMessage);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response);
                } else {
                    Ln.w("unbind to room error: %s", response.raw().toString());
                    callback.onError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(null);
            }
        });
    }

    @Override
    public void updateBindings(String roomIdentity, final UpdateBindingCallback callback) {
        Call<BindingResponses> call = getLyraClient().getBindings(roomIdentity);
        call.enqueue(new Callback<BindingResponses>() {
            @Override
            public void onResponse(Call<BindingResponses> call, Response<BindingResponses> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<BindingResponses> call, Throwable t) {
                callback.onError();
            }
        });
    }

    private LyraClient getLyraClient() {
        if (lyraClient == null) {
            lyraClient = apiClientProvider.getLyraClient();
        }

        return lyraClient;
    }
}
