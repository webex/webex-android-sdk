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

package com.ciscospark.androidsdk.membership;

import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

public class MembershipClient {

    private Authenticator _authenticator;

    private MembershipService _service;

    public MembershipClient(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(MembershipService.class);
    }

    public void list(@Nullable String roomId, @Nullable String personId, @Nullable String personEmail, int max, @NonNull CompletionHandler<List<Membership>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, roomId, personId, personEmail, max <= 0 ? null : max).enqueue(new ListCallback<>(handler));
        });
    }

    public void create(@NonNull String roomId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.create(s, Maps.makeMap("roomId", roomId, "personId", personId, "personEmail", personEmail, "isModerator", isModerator)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void get(@NonNull String membershipId, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, membershipId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.update(s, membershipId, Maps.makeMap("isModerator", isModerator)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.delete(s, membershipId).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface MembershipService {
        @GET("memberships")
        Call<ListBody<Membership>> list(@Header("Authorization") String authorization,
                                        @Query("roomId") String roomId,
                                        @Query("personId") String personId,
                                        @Query("personEmail") String personEmail,
                                        @Query("max") Integer max);

        @POST("memberships")
        Call<Membership> create(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("memberships/{membershipId}")
        Call<Membership> get(@Header("Authorization") String authorization, @Path("membershipId") String membershipId);

        @PUT("memberships/{membershipId}")
        Call<Membership> update(@Header("Authorization") String authorization, @Path("membershipId") String membershipId, @Body Map parameters);

        @DELETE("memberships/{membershipId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("membershipId") String membershipId);
    }

}
