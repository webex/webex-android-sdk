package com.cisco.spark.android.client;

import com.cisco.spark.android.status.HealthCheckResponse;
import com.cisco.spark.android.wdm.DeviceRegistration;
import android.net.Uri;
import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistrationBindingItems;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelItems;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.persistence.model.RegistrationRequestResource;
import com.cisco.spark.android.whiteboard.persistence.model.SpaceUrl;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface WhiteboardPersistenceClient {

    // channels
    @POST("channels")
    Call<Channel> createChannel(@Query("createOpenSpace") boolean createOpenSpace, @Query("createHiddenSpace") boolean createHiddenSpace, @Body Channel channel);

    @POST("channels")
    Call<Channel> createPrivateChannel(@Query("createOpenSpace") boolean createOpenSpace, @Query("createHiddenSpace") boolean createHiddenSpace, @Body Channel channel);

    @POST("channels/{channelId}/register")
    Call<BoardRegistration> sharedMercuryBoardRegistration(@Path("channelId") String channelId, @Body BoardRegistration br);

    @GET("registrations/bindings")
    Call<BoardRegistrationBindingItems> getBoardRegistrationBindings(@Query("webSocketUrl") Uri webSocketUrl);

    @PUT("channels/{channelId}")
    Call<Channel> updateChannel(@Path("channelId") String channelId, @Body Channel channel);

    @PATCH("channels/{channelId}")
    Call<Channel> patchChannel(@Path("channelId") String channelId, @Body Channel channel);

    @GET("channels/{channelId}")
    Call<Channel> getChannel(@Path("channelId") String channelId);

    @GET("channels/{channelId}")
    Observable<Channel> getChannelRx(@Path("channelId") String channelId);

    @GET("channels")
    Call<ChannelItems> getChannelChildren(@Query("parentId") String channelId);

    // content
    @POST("channels/{channelId}/contents")
    Call<ContentItems> addContents(@Path("channelId") String channelId, @Body List<Content> contents);

    @POST("channels/{channelId}/contents?clearBoard=true")
    Call<ContentItems> clearPartialContents(@Path("channelId") String channelId, @Body List<Content> contents);

    @GET("channels/{channelId}/contents")
    Call<ContentItems> getContents(@Path("channelId") String channelId, @Query("contentsLimit") int contentsLimit);

    @GET("channels/{channelId}/contents")
    Observable<Response<ContentItems>> getContentsRx(@Path("channelId") String channelId, @Query("contentsLimit") int contentsLimit);

    @GET
    Call<ContentItems> getContents(@Url String url);

    @GET
    Observable<Response<ContentItems>> getContentsRx(@Url String url);

    @GET("channels/{channelId}/contents/{contentId}")
    Call<Content> getContent(@Path("channelId") String channelId);

    @PUT("channels/{channelId}/contents/{contentId}")
    Call<Channel> updateContents(@Path("channelId") String channelId, @Path("contentId") String contentId, @Body Content content);

    @DELETE("channels/{channelId}/contents")
    Call<Void> deleteAllContents(@Path("channelId") String channelId);

    @DELETE("channels/{channelId}/contents/{contentId}")
    Call<Channel> deleteContents(@Path("channelId") String channelId, @Path("contentId") String contentId);

    //Spaces
    @PUT("channels/{channelId}/spaces/hidden")
    Call<SpaceUrl> getHiddenSpace(@Path("channelId") String channelId);

    @PUT("channels/{channelId}/spaces/open")
    Observable<SpaceUrl> getOpenSpace(@Path("channelId") String channelId);

    @PUT("channels/{channelId}/spaces/hidden")
    Observable<SpaceUrl> getHiddenSpaceRx(@Path("channelId") String channelId);

    @POST("registrations")
    Call<DeviceRegistration> registerChannelId(@Body RegistrationRequestResource registrationRequestResource);

    @POST("board/api/v1/registrations/{registrationId}/contextualbindings")
    Call<Void> addBindingRegistration(@Path("registrationId") String registrationId, @Body DeviceRegistration registrationResourceModel);

    @GET("channels")
    Call<ChannelItems<Channel>> getChannelsWithAcl(@Query("aclUrlLink") String aclUrlLink, @Query("channelsLimit") int channelsLimit);

    @GET
    Call<ChannelItems<Channel>> getChannels(@Url String url);

    @GET("channels/@me/private")
    Call<ChannelItems<Channel>> getPrivateChannels();

    @GET("channels/@me/private")
    Call<ChannelItems<Channel>> getPrivateChannels(@Query("channelsLimit") int limit);

    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();

    @POST("channels/{channelId}/deleteLock")
    Call<Response<ResponseBody>> lockChannel(@Path("channelId") String channelId);
}
