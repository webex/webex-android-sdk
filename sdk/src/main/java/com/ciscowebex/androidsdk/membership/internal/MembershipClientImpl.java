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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipClient;
import com.ciscowebex.androidsdk.membership.MembershipReadStatus;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.space.internal.SpaceClientImpl;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk_commlib.SDKCommon;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private ConversationService _cService;

    public MembershipClientImpl(Authenticator authenticator, SDKCommon common) {
        common.inject(this);
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

    private interface ConversationService {
        @GET("conversations/{spaceId}")
        Call<ResponseBody> listWithReadStatus(@Header("Authorization") String authorization, @Nullable @Path("spaceId") String spaceId,
                                              @Query("activitiesLimit") int activitiesLimit,
                                              @Query("participantAckFilter") String participantAckFilter);
    }
}
