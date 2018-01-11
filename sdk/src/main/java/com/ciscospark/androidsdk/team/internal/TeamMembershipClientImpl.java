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

package com.ciscospark.androidsdk.team.internal;


import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.team.TeamMembership;
import com.ciscospark.androidsdk.team.TeamMembershipClient;
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

public class TeamMembershipClientImpl implements TeamMembershipClient {

    private Authenticator _authenticator;

    private TeamMembershipService _service;

    public TeamMembershipClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(TeamMembershipService.class);
    }

    public void list(@Nullable String teamId, int max, @NonNull CompletionHandler<List<TeamMembership>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, teamId, max <= 0 ? null : max).enqueue(new ListCallback<>(handler));
        });
    }

    public void create(@NonNull String teamId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<TeamMembership> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.create(s, Maps.makeMap("teamId", teamId, "personId", personId, "personEmail", personEmail, "isModerator", isModerator)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void get(@NonNull String membershipId, @NonNull CompletionHandler<TeamMembership> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, membershipId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<TeamMembership> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.update(s, membershipId, Maps.makeMap("isModerator", isModerator)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.delete(s, membershipId).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface TeamMembershipService {
        @GET("team/memberships")
        Call<ListBody<TeamMembership>> list(@Header("Authorization") String authorization, @Query("teamId") String roomId, @Query("max") Integer max);

        @POST("team/memberships")
        Call<TeamMembership> create(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("team/memberships/{membershipId}")
        Call<TeamMembership> get(@Header("Authorization") String authorization, @Path("membershipId") String membershipId);

        @PUT("team/memberships/{membershipId}")
        Call<TeamMembership> update(@Header("Authorization") String authorization, @Path("membershipId") String membershipId, @Body Map parameters);

        @DELETE("team/memberships/{membershipId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("membershipId") String membershipId);
    }
}
