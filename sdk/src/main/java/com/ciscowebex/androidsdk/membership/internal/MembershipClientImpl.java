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

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.processing.ActivityListener;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.WebexEventPayload;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.WebexEventPayloadImpl;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipClient;
import com.ciscowebex.androidsdk.membership.MembershipObserver;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk_commlib.SDKCommon;
import com.github.benoitdion.ln.Ln;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import me.helloworld.utils.collection.Maps;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class MembershipClientImpl implements MembershipClient {
    @Inject
    volatile Settings _settings;

    private DeviceRegistration _device;

    private Authenticator _authenticator;

    private MembershipService _service;

    private MembershipObserver _observer;

    private Context _context;

    @Inject
    ActivityListener activityListener;

    @Inject
    ApiTokenProvider _provider;

    @Deprecated
    public MembershipClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(MembershipService.class);
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

    public void list(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, int max, @NonNull CompletionHandler<List<Membership>> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.list(s, spaceId, spaceId, personId, personEmail, max <= 0 ? null : max), new ListCallback<>(handler));
    }

    public void create(@NonNull String spaceId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.create(s, Maps.makeMap("roomId", spaceId, "spaceId", spaceId, "personId", personId, "personEmail", personEmail, "isModerator", isModerator)), new ObjectCallback<>(handler));
    }

    public void get(@NonNull String membershipId, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.get(s, membershipId), new ObjectCallback<>(handler));
    }

    public void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.update(s, membershipId, Maps.makeMap("isModerator", isModerator)), new ObjectCallback<>(handler));
    }

    public void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
                _service.delete(s, membershipId), new ObjectCallback<>(handler));
    }

    @Override
    public void listWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<List<MembershipReadStatus>> handler) {
        if (null == _cService) {
            handler.onComplete(ResultImpl.error("Device not registered."));
            return;
        }
        CompletionHandler<ResponseBody> resHandler = new CompletionHandler<ResponseBody>() {
            @Override
            public void onComplete(Result<ResponseBody> result) {
                if (!result.isSuccessful()) {
                    handler.onComplete(ResultImpl.error(result.getError()));
                    return;
                }
                if (null != result.getData())
                    try {
                        List<MembershipReadStatus> results = new ArrayList<>();
                        String json = result.getData().string();
                        JSONArray array = new JSONObject(json).getJSONObject("participants").getJSONArray("items");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject object = array.getJSONObject(i);
                            String personId = object.getString("entryUUID");
                            String personEmail = object.getString("emailAddress");
                            String personDisplayName = object.getString("displayName");
                            String personOrgId = object.getString("orgId");
                            Date created = null;// created is not available in the conversations payload
                            boolean isMonitor = false;
                            boolean isModerator = false;
                            String lastSeenId = null;
                            Date lastSeenDate = null;
                            JSONObject roomProperties = object.optJSONObject("roomProperties");
                            if (null != roomProperties) {
                                isModerator = roomProperties.optBoolean("isModerator");
                                lastSeenId = roomProperties.optString("lastSeenActivityUUID", null);
                                String date = roomProperties.optString("lastSeenActivityDate", null);
                                if (null != date) {
                                    lastSeenDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).parse(date);
                                }
                            }
                            String id = personId + ":" + spaceId;
                            Membership membership = new MembershipInternal(new WebexId(WebexId.Type.MEMBERSHIP_ID, id).toHydraId(),
                                    new WebexId(WebexId.Type.PEOPLE_ID, personId).toHydraId(),
                                    personEmail, personDisplayName,
                                    new WebexId(WebexId.Type.ORGANIZATION_ID, personOrgId).toHydraId(),
                                    new WebexId(WebexId.Type.ROOM_ID, spaceId).toHydraId(),
                                    isModerator, isMonitor, created);
                            MembershipReadStatus membershipReadStatus = new MembershipReadStatusInternal(membership,
                                    new WebexId(WebexId.Type.MESSAGE_ID, lastSeenId).toHydraId(), lastSeenDate);
                            results.add(membershipReadStatus);
                        }
                        handler.onComplete(ResultImpl.success(results));
                        return;
                    } catch (IOException | JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                handler.onComplete(ResultImpl.error(""));
            }
        };
        ServiceBuilder.async(_authenticator, resHandler, s ->
                _cService.listWithReadStatus(s, spaceId, 0, "all"), new ObjectCallback<>(resHandler));
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

    private void runOnUiThread(Runnable r, Object conditioner) {
        if (conditioner == null) return;
        Handler handler = new Handler(_context.getMainLooper());
        handler.post(r);
    }

    private void processorActivity(Activity activity) {
        if (null == _observer)
            return;
        if (null == activity) {
            Ln.e("MembershipClientImpl.processorActivity() activity is null");
            return;
        }
        WebexEventPayload eventPayload = new WebexEventPayloadImpl(activity, _provider.getAuthenticatedUserOrNull(), "membership");
        MembershipObserver.MembershipEvent event;
        switch (activity.getVerb()) {
            case Verb.add:
                event = new MembershipObserver.MembershipCreated(eventPayload, new MembershipImpl(activity));
                break;
            case Verb.leave:
                event = new MembershipObserver.MembershipDeleted(eventPayload, new MembershipImpl(activity));
                break;
            case Verb.assignModerator:
            case Verb.unassignModerator:
                event = new MembershipObserver.MembershipUpdated(eventPayload, new MembershipImpl(activity));
                break;
            case Verb.acknowledge:
                event = new MembershipObserver.MembershipSeen(eventPayload, new MembershipImpl(activity), new WebexId(WebexId.Type.MESSAGE_ID, activity.getObject().getId()).toHydraId());
                break;
            default:
                return;
        }
        runOnUiThread(() -> _observer.onEvent(event), _observer);
      
    private interface ConversationService {
        @GET("conversations/{spaceId}")
        Call<ResponseBody> listWithReadStatus(@Header("Authorization") String authorization, @Nullable @Path("spaceId") String spaceId,
                                              @Query("activitiesLimit") int activitiesLimit,
                                              @Query("participantAckFilter") String participantAckFilter);
    }
}
