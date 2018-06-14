/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.room.internal;


import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.room.Room;
import com.ciscowebex.androidsdk.room.RoomClient;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;

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

public class RoomClientImpl implements RoomClient {

    private Authenticator _authenticator;

    private RoomService _service;

    public RoomClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(RoomService.class);
    }

    public void list(@Nullable String teamId, int max, @Nullable Room.RoomType type, @Nullable SortBy sortBy, @NonNull CompletionHandler<List<Room>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, teamId, type != null ? type.name() : null, sortBy != null ? sortBy.name().toLowerCase() : null, max <= 0 ? null : max).enqueue(new ListCallback<Room>(handler));
        });
    }

    public void create(@NonNull String title, @Nullable String teamId, @NonNull CompletionHandler<Room> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.create(s, Maps.makeMap("title", title, "teamId", teamId)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void get(@NonNull String roomId, @NonNull CompletionHandler<Room> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, roomId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void update(@NonNull String roomId, @NonNull String title, @NonNull CompletionHandler<Room> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.update(s, roomId, Maps.makeMap("title", title)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void delete(@NonNull String roomId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.delete(s, roomId).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface RoomService {
        @GET("rooms")
        Call<ListBody<Room>> list(@Header("Authorization") String authorization,
                                  @Query("teamId") String teamId,
                                  @Query("type") String type,
                                  @Query("sortBy") String sortBy,
                                  @Query("max") Integer max);

        @POST("rooms")
        Call<Room> create(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("rooms/{roomId}")
        Call<Room> get(@Header("Authorization") String authorization, @Path("roomId") String roomId);

        @PUT("rooms/{roomId}")
        Call<Room> update(@Header("Authorization") String authorization, @Path("roomId") String roomId, @Body Map parameters);

        @DELETE("rooms/{roomId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("roomId") String membershipId);
    }
}
