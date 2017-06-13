package com.cisco.spark.android.lyra;

import com.cisco.spark.android.client.LyraClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.github.benoitdion.ln.Ln;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CloudBindingBackend implements BindingBackend {

    private LyraClient lyraClient;
    private ApiClientProvider apiClientProvider;

    public CloudBindingBackend(ApiClientProvider apiClientProvider, EventBus bus) {
        this.apiClientProvider = apiClientProvider;
    }

    @Override
    public void bind(String roomIdentity, BindingRequest bindingRequest, final BindingCallback callback) {
        Call<BindingResponse> call = getLyraClient().bind(roomIdentity, bindingRequest);
        call.enqueue(new Callback<BindingResponse>() {
            @Override
            public void onResponse(Call<BindingResponse> call, Response<BindingResponse> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    Ln.w("bind to room error: %s", response.raw().toString());
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<BindingResponse> call, Throwable t) {
                callback.onError();
            }
        });
    }

    @Override
    public void unbind(final String roomIdentity, final String  bindingUrl, final String kmsMessage, final BindingCallback callback) {
        Call<Void> call = getLyraClient().unbind(roomIdentity, bindingUrl, kmsMessage);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    Ln.w("unbind to room error: %s", response.raw().toString());
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError();
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
