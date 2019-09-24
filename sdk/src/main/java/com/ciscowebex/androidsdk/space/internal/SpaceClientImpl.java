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

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.processing.ActivityListener;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscowebex.androidsdk.internal.WebexEventPayloadImpl;
import com.ciscowebex.androidsdk.membership.MembershipObserver;
import com.ciscowebex.androidsdk.membership.internal.MembershipImpl;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.space.Space;
import com.ciscowebex.androidsdk.space.SpaceClient;
import com.ciscowebex.androidsdk.space.SpaceObserver;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;

import me.helloworld.utils.collection.Maps;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class SpaceClientImpl implements SpaceClient {
    @Inject
    volatile Settings _settings;

    private DeviceRegistration _device;

    private Authenticator _authenticator;

    private SpaceService _service;

    private SpaceObserver _observer;

    private Context _context;

    @Inject
    ActivityListener activityListener;

    @Inject
    ApiTokenProvider _provider;

    @Deprecated
    public SpaceClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(SpaceService.class);
        if (null != _settings)
            _device = _settings.getDeviceRegistration();
        if (null != _device) {
            Uri uri = _device.getConversationServiceUrl();
            if (uri != null) {
                String url = uri.toString();
                if (!url.endsWith("/")) {
                    url = url + "/";
                }
                _cService = new ServiceBuilder().baseURL(url).build(ConversationService.class);
            }
        }
    }

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
        if (null == _cService) {
            handler.onComplete(ResultImpl.error("Device not registered."));
            return;
        }
        ServiceBuilder.async(_authenticator, handler, s ->
                _cService.getWithReadStatus(s, spaceId, false, true, true, 0, false), new ObjectCallback<>(handler));
    }

    @Override
    public void listWithReadStatus(@NonNull CompletionHandler<List<SpaceReadStatus>> handler) {
        if (null == _cService) {
            handler.onComplete(ResultImpl.error("Device not registered."));
            return;
        }
        ServiceBuilder.async(_authenticator, handler, s ->
                _cService.listWithReadStatus(s, false, true, true, 0, false), new ListCallback<>(handler));
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

    private interface ConversationService {
        @GET("conversations/{spaceId}")
        Call<SpaceReadStatus> getWithReadStatus(@Header("Authorization") String authorization, @Nullable @Path("spaceId") String spaceId,
                                                @Query("includeParticipants") boolean includeParticipants,
                                                @Query("uuidEntryFormat") boolean uuidEntryFormat,
                                                @Query("personRefresh") boolean personRefresh,
                                                @Query("activitiesLimit") int activitiesLimit,
                                                @Query("includeConvWithDeletedUserUUID") boolean includeConvWithDeletedUserUUID);

        @GET("conversations")
        Call<ListBody<SpaceReadStatus>> listWithReadStatus(@Header("Authorization") String authorization,
                                                 @Query("includeParticipants") boolean includeParticipants,
                                                 @Query("uuidEntryFormat") boolean uuidEntryFormat,
                                                 @Query("personRefresh") boolean personRefresh,
                                                 @Query("activitiesLimit") int activitiesLimit,
                                                 @Query("includeConvWithDeletedUserUUID") boolean includeConvWithDeletedUserUUID);
    }

    private void runOnUiThread(Runnable r, Object conditioner) {
        if (conditioner == null) return;
        Handler handler = new Handler(_context.getMainLooper());
        handler.post(r);
    }

    private void processorActivity(Activity activity) {
        if (null == _observer)
            return;
        if (null == activity) {
            Ln.e("SpaceClientImpl.processorActivity() activity is null");
            return;
        }
        WebexEventPayload eventPayload = new WebexEventPayloadImpl(activity, _provider.getAuthenticatedUserOrNull(), "rooms");
        SpaceObserver.SpaceEvent event;
        switch (activity.getVerb()) {
            case Verb.create:
                event = new SpaceObserver.SpaceCreated(eventPayload,
                        new WebexId(WebexId.Type.ROOM_ID, activity.getObject().getId()).toHydraId(),
                        new WebexId(WebexId.Type.PEOPLE_ID, activity.getActor().getId()).toHydraId());

                break;
            case Verb.update:
                event = new SpaceObserver.SpaceUpdated(eventPayload, new WebexId(WebexId.Type.ROOM_ID, activity.getObject().getId()).toHydraId());
                break;
            default:
                return;
        }
        runOnUiThread(() -> _observer.onEvent(event), _observer);
    }
}
