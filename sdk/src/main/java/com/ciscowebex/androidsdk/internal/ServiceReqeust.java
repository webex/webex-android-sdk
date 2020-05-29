package com.ciscowebex.androidsdk.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.queue.SerialQueue;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.http.HttpClient;
import com.github.benoitdion.ln.Ln;
import okhttp3.*;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class ServiceReqeust {

    public static final String HEADER_TRACKING_ID = "TrackingID";

    private final Service service;

    private String url;

    private final List<String> paths = new ArrayList<>();

    private final Map<String, String> queries = new HashMap<>();

    private RequestBody body;

    private Device device;

    private Authenticator authenticator;

    private CompletionHandler<?> errorHandler;

    private Queue queue;

    private Type typeOfModel;

    private final Request.Builder builder;

    public ServiceReqeust(Service service, Request.Builder builder) {
        this.service = service;
        this.builder = builder;
    }

    public ServiceReqeust url(String url) {
        this.url = url;
        return this;
    }

    public ServiceReqeust to(String... paths) {
        this.paths.addAll(Arrays.asList(paths));
        return this;
    }

    public ServiceReqeust with(String key, String value) {
        if (key != null && value != null) {
            queries.put(key, value);
        }
        return this;
    }

    public ServiceReqeust header(String key, String value) {
        if (key != null && value != null) {
            builder.header(key, value);
        }
        return this;
    }

    public ServiceReqeust auth(Authenticator authenticator) {
        this.authenticator = authenticator;
        return this;
    }

    public <M> ServiceReqeust error(CompletionHandler<M> handler) {
        this.errorHandler = handler;
        return this;
    }

    public ServiceReqeust queue(Queue queue) {
        this.queue = queue;
        return this;
    }

    public ServiceReqeust device(Device device) {
        this.device = device;
        return this;
    }

    public ServiceReqeust model(Type typeOfModel) {
        this.typeOfModel = typeOfModel;
        return this;
    }

    public <M> ServiceReqeust model(Class<M> typeOfModel) {
        this.typeOfModel = typeOfModel;
        return this;
    }

    public <M> void async(Closure<M> closure) {
        String base = this.url;
        if (base == null && service != null) {
            base = service.endpoint(device);
        }
        HttpUrl.Builder url = HttpUrl.parse(base).newBuilder();
        if (paths.size() > 0) {
            url.addPathSegments(Utils.join("/", paths));
        }
        if (queries.size() > 0) {
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                url.addQueryParameter(entry.getKey(),  entry.getValue());
            }
        }
        builder.url(url.build());

        if (authenticator == null) {
            async(HttpClient.defaultClient, builder.build(), false, closure);
        }
        else {
            authenticator.getToken(tokenResult -> {
                String token = tokenResult.getData();
                if (token == null) {
                    doError(tokenResult.getError() == null ? WebexError.from("Token is null") : tokenResult.getError());
                } else {
                    builder.header("Authorization", "Bearer " + token);
                    async(HttpClient.defaultClient, builder.build(), true, closure);
                }
            });
        }
    }

    private <M> void async(OkHttpClient client, Request request, boolean refresh, Closure<M> closure) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (e instanceof SSLException && !client.connectionSpecs().contains(HttpClient.TLS_SPEC)) {
                    async(HttpClient.newClient().connectionSpecs(Collections.singletonList(HttpClient.TLS_SPEC)).build(), request, refresh, closure);
                    return;
                }
                doError(WebexError.from(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.code() == 401 && refresh && authenticator != null) {
                    authenticator.refreshToken(refreshResult -> {
                        String refreshedToken = refreshResult.getData();
                        if (refreshedToken == null) {
                            doError(refreshResult.getError() == null ? WebexError.from("Token is null") : refreshResult.getError());
                        } else {
                            async(client, request.newBuilder().header("Authorization", "Bearer " + refreshedToken).build(), false, closure);
                        }
                    });
                }
                else if (response.isSuccessful()) {
                    try {
                        ResponseBody body = response.body();
                        if (typeOfModel == Response.class) {
                            doResult(closure, (M) response);
                        } else if (typeOfModel == ResponseBody.class) {
                            doResult(closure, (M) body);
                        } else if (typeOfModel == null || typeOfModel == Void.class || typeOfModel == void.class) {
                            doResult(closure, null);
                        } else {
                            String data = body == null ? null : body.string();
                            if (data == null) {
                                doError(WebexError.from("No body in the response"));
                            } else {
                                M model = Json.fromJson(data, typeOfModel);
                                if (model == null) {
                                    doError(WebexError.from("Illegal body in the response"));
                                } else {
                                    doResult(closure, model);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        doError(WebexError.from(t));
                    }
                } else {
                    doError(WebexError.from(response));
                }
            }
        });
    }

    private void doError(@NonNull WebexError error) {
        Ln.e("HTTP Error: " + error);
        if (errorHandler != null) {
            if (queue == null) {
                Queue.main.run(() -> errorHandler.onComplete(ResultImpl.error(error)));
                return;
            }
            if (queue instanceof SerialQueue) {
                Queue.main.run(() -> {
                    errorHandler.onComplete(ResultImpl.error(error));
                    queue.yield();
                });
            } else {
                queue.run(() -> errorHandler.onComplete(ResultImpl.error(error)));
            }
        }
    }

    private <M1, M2> void doResult(Closure<M1> modelCallback, @Nullable M1 model) {
        if (modelCallback != null) {
            if (queue == null || queue instanceof SerialQueue) {
                modelCallback.invoke(model);
                return;
            }
            queue.run(() -> modelCallback.invoke(model));
        }
    }
}

