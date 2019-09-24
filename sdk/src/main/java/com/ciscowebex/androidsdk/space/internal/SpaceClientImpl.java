/*
 * Copyright 2016-2019 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.space.internal;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.model.conversation.Conversation;
import com.cisco.spark.android.processing.ActivityListener;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.WebexEvent;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.InternalWebexEventPayload;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.space.*;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;

import com.ciscowebex.androidsdk_commlib.SDKCommon;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.collection.Maps;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import javax.inject.Inject;

public class SpaceClientImpl implements SpaceClient {

    private Authenticator _authenticator;

    private SpaceService _service;

    private SpaceObserver _observer;

    private Context _context;

    @Inject
    ActivityListener activityListener;

    @Inject
    ApiTokenProvider _provider;

    @Inject
    ApiClientProvider _client;

    public SpaceClientImpl(Context context, Authenticator authenticator, SDKCommon common) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(SpaceService.class);
        _context = context;
        common.inject(this);
        activityListener.register(activity -> {
            processorActivity(activity);
            return null;
        });
    }

    @Override
    public void setSpaceObserver(SpaceObserver observer) {
        _observer = observer;
    }

    @Override
    public void list(@Nullable String teamId, int max, @Nullable Space.SpaceType type, @Nullable SortBy sortBy, @NonNull CompletionHandler<List<Space>> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.list(s, teamId, type != null ? type.serializedName() : null, sortBy != null ? sortBy.serializedName() : null, max <= 0 ? null : max), new ListCallback<Space>(handler));
    }

    @Override
    public void create(@NonNull String title, @Nullable String teamId, @NonNull CompletionHandler<Space> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.create(s, Maps.makeMap("title", title, "teamId", teamId)), new ObjectCallback<>(handler));
    }

    @Override
    public void get(@NonNull String spaceId, @NonNull CompletionHandler<Space> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.get(s, spaceId), new ObjectCallback<>(handler));
    }

    @Override
    public void update(@NonNull String spaceId, @NonNull String title, @NonNull CompletionHandler<Space> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.update(s, spaceId, Maps.makeMap("title", title)), new ObjectCallback<>(handler));
    }

    @Override
    public void delete(@NonNull String spaceId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.delete(s, spaceId), new ObjectCallback<>(handler));
    }

    @Override
    public void getMeeting(@NonNull String spaceId, @NonNull CompletionHandler<SpaceMeeting> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.getMeeting(s, spaceId), new ObjectCallback<>(handler));
    }

    @Override
    public void getWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<SpaceReadStatus> handler) {
        _client.getConversationClient().getConversationReadStatus(WebexId.translate(spaceId)).enqueue(new Callback<Conversation>() {
            @Override
            public void onResponse(Call<Conversation> call, Response<Conversation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handler.onComplete(ResultImpl.success(new InternalSpaceReadStatus(response.body())));
                } else {
                    handler.onComplete(ResultImpl.error(response));
                }
            }

            @Override
            public void onFailure(Call<Conversation> call, Throwable throwable) {
                handler.onComplete(ResultImpl.error(throwable));
            }
        });
    }

    @Override
    public void listWithReadStatus(@NonNull CompletionHandler<List<SpaceReadStatus>> handler) {
        _client.getConversationClient().getConversationsReadStatus().enqueue(new Callback<ItemCollection<Conversation>>() {
            @Override
            public void onResponse(Call<ItemCollection<Conversation>> call, Response<ItemCollection<Conversation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SpaceReadStatus> result = new ArrayList<>();
                    for (Conversation conversation : response.body().getItems()) {
                        result.add(new InternalSpaceReadStatus(conversation));
                    }
                    handler.onComplete(ResultImpl.success(result));
                } else {
                    handler.onComplete(ResultImpl.error(response));
                }
            }

            @Override
            public void onFailure(Call<ItemCollection<Conversation>> call, Throwable throwable) {
                handler.onComplete(ResultImpl.error(throwable));
            }
        });
    }

    private void processorActivity(Activity activity) {
        if (null == _observer) {
            return;
        }
        if (null == activity) {
            Ln.e("SpaceClientImpl.processorActivity() activity is null");
            return;
        }
        Space space = new InternalSpace(activity);
        WebexEvent.Payload payload = new InternalWebexEventPayload(activity, _provider.getAuthenticatedUserOrNull(), space);
        SpaceObserver.SpaceEvent event;
        switch (activity.getVerb()) {
            case Verb.create:
                event = new SpaceObserver.SpaceCreated(space, payload);
                break;
            case Verb.update:
                event = new SpaceObserver.SpaceUpdated(space, payload);
                break;
            default:
                return;
        }
        runOnUiThread(() -> _observer.onEvent(event), _observer);
    }

    private void runOnUiThread(Runnable r, Object conditioner) {
        if (conditioner == null) return;
        Handler handler = new Handler(_context.getMainLooper());
        handler.post(r);
    }

    private interface SpaceService {
        @GET("rooms")
        Call<ListBody<Space>> list(@Header("Authorization") String authorization,
                                   @Query("teamId") String teamId,
                                   @Query("type") String type,
                                   @Query("sortBy") String sortBy,
                                   @Query("max") Integer max);

        @POST("rooms")
        Call<Space> create(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("rooms/{spaceId}")
        Call<Space> get(@Header("Authorization") String authorization, @Path("spaceId") String spaceId);

        @PUT("rooms/{spaceId}")
        Call<Space> update(@Header("Authorization") String authorization, @Path("spaceId") String spaceId, @Body Map parameters);

        @DELETE("rooms/{spaceId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("spaceId") String spaceId);

        @GET("rooms/{spaceId}/meetingInfo")
        Call<SpaceMeeting> getMeeting(@Header("Authorization") String authorization, @Path("spaceId") String spaceId);

    }
}
