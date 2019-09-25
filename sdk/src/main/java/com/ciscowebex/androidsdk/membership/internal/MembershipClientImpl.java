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

package com.ciscowebex.androidsdk.membership.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.model.conversation.Conversation;
import com.cisco.spark.android.processing.ActivityListener;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipClient;
import com.ciscowebex.androidsdk.membership.MembershipObserver;
import com.ciscowebex.androidsdk.membership.MembershipReadStatus;
import com.ciscowebex.androidsdk.message.internal.WebexId;
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

public class MembershipClientImpl implements MembershipClient {

    private Authenticator _authenticator;

    private MembershipService _service;

    private MembershipObserver _observer;

    private Context _context;

    @Inject
    ActivityListener activityListener;

    @Inject
    ApiTokenProvider _provider;

    @Inject
    ApiClientProvider _client;

    public MembershipClientImpl(Context context, Authenticator authenticator, SDKCommon common) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(MembershipService.class);
        _context = context;
        common.inject(this);
        activityListener.register(activity -> {
            processorActivity(activity);
            return null;
        });
    }

    @Override
    public void setMembershipObserver(MembershipObserver observer) {
        _observer = observer;
    }

    @Override
    public void list(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, int max, @NonNull CompletionHandler<List<Membership>> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.list(s, spaceId, spaceId, personId, personEmail, max <= 0 ? null : max), new ListCallback<>(handler));
    }

    @Override
    public void create(@NonNull String spaceId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.create(s, Maps.makeMap("roomId", spaceId, "spaceId", spaceId, "personId", personId, "personEmail", personEmail, "isModerator", isModerator)), new ObjectCallback<>(handler));
    }

    @Override
    public void get(@NonNull String membershipId, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.get(s, membershipId), new ObjectCallback<>(handler));
    }

    @Override
    public void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.update(s, membershipId, Maps.makeMap("isModerator", isModerator)), new ObjectCallback<>(handler));
    }

    @Override
    public void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.delete(s, membershipId), new ObjectCallback<>(handler));
    }

    @Override
    public void listWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<List<MembershipReadStatus>> handler) {
        _client.getConversationClient().getConversationParticipantsReadStatus(WebexId.translate(spaceId)).enqueue(new Callback<Conversation>() {
            @Override
            public void onResponse(Call<Conversation> call, Response<Conversation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Conversation conversation = response.body();
                    List<MembershipReadStatus> result = new ArrayList<>();
                    for (Person person : conversation.getParticipants().getItems()) {
                        try {
                            result.add(new InternalMembershipReadStatus(conversation, person));
                        }
                        catch (Throwable ignored) {
                        }
                    }
                    handler.onComplete(ResultImpl.success(result));
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

    private void processorActivity(Activity activity) {
        if (null == _observer) {
            return;
        }
        if (null == activity) {
            Ln.e("MembershipClientImpl.processorActivity() activity is null");
            return;
        }
        Membership membership = new InternalMembership(activity);
        MembershipObserver.MembershipEvent event;
        switch (activity.getVerb()) {
            case Verb.add:
                event = new InternalMembership.InternalMembershipCreated(membership, activity);
                break;
            case Verb.leave:
                event = new InternalMembership.InternalMembershipDeleted(membership, activity);
                break;
            case Verb.assignModerator:
            case Verb.unassignModerator:
                event = new InternalMembership.InternalMembershipUpdated(membership, activity);
                break;
            case Verb.acknowledge:
                event = new InternalMembership.InternalMembershipSeen(membership, activity, new WebexId(WebexId.Type.MESSAGE_ID, activity.getObject().getId()).toHydraId());
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

    private interface MembershipService {
        @GET("memberships")
        Call<ListBody<Membership>> list(@Header("Authorization") String authorization,
                                        @Query("roomId") String roomId,
                                        @Query("spaceId") String spaceId,
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
