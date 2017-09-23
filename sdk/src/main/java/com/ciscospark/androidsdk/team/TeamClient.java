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

package com.ciscospark.androidsdk.team;


import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.utils.http.ListBody;
import com.ciscospark.androidsdk.utils.http.ListCallback;
import com.ciscospark.androidsdk.utils.http.ObjectCallback;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
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

public class TeamClient {

    private Authenticator _authenticator;

    private TeamService _service;

    public TeamClient(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(TeamService.class);
    }

    public void list(int max, @NonNull CompletionHandler<List<Team>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, max <= 0 ? null : max).enqueue(new ListCallback<>(handler));
        });
    }

    public void create(@NonNull String name, @NonNull CompletionHandler<Team> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.create(s, Maps.makeMap("name", name)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void get(@NonNull String teamId, @NonNull CompletionHandler<Team> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, teamId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void update(@NonNull String teamId, String name, @NonNull CompletionHandler<Team> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.update(s, teamId, Maps.makeMap("name", name)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void delete(@NonNull String teamId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.delete(s, teamId).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface TeamService {
        @GET("teams")
        Call<ListBody<Team>> list(@Header("Authorization") String authorization, @Query("max") Integer max);

        @POST("teams")
        Call<Team> create(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("teams/{teamId}")
        Call<Team> get(@Header("Authorization") String authorization, @Path("teamId") String teamId);

        @PUT("teams/{teamId}")
        Call<Team> update(@Header("Authorization") String authorization, @Path("teamId") String teamId, @Body Map parameters);

        @DELETE("teams/{teamId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("teamId") String teamId);
    }

}
