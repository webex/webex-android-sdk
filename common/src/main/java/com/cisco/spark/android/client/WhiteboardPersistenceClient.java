package com.cisco.spark.android.client;

import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelItems;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.RegistrationRequestResource;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.SpaceUrl;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface WhiteboardPersistenceClient {

    // channels
    @POST("channels")
    Call<Channel> createChannel(@Body Channel channel);

    @POST("channels")
    Call<Channel> createPrivateChannel(@Body Channel channel);

    @POST("channels/{channelId}/register")
    Call<BoardRegistration> sharedMercuryBoardRegistration(@Path("channelId") String channelId, @Body BoardRegistration br);

    @PUT("channels/{channelId}")
    Call<Channel> updateChannel(@Path("channelId") String channelId, @Body Channel channel);

    @PATCH("channels/{channelId}")
    Call<Channel> patchChannel(@Path("channelId") String channelId, @Body Channel channel);

    @GET("channels/{channelId}")
    Call<Channel> getChannel(@Path("channelId") String channelId);

    @GET("channels")
    Call<ChannelItems> getChannelChildren(@Query("parentId") String channelId);

    // content
    @POST("channels/{channelId}/contents")
    Call<ContentItems> addContents(@Path("channelId") String channelId, @Body List<Content> contents);

    @GET("channels/{channelId}/contents")
    Call<ContentItems> getContents(@Path("channelId") String channelId, @Query("contentsLimit") int contentsLimit);

    @GET
    Call<ContentItems> getContents(@Url String url);

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

    @POST("registrations")
    Call<DeviceRegistration> registerChannelId(@Body RegistrationRequestResource registrationRequestResource);

    @POST("board/api/v1/registrations/{registrationId}/contextualbindings")
    Call<Void> addBindingRegistration(@Path("registrationId") String registrationId, @Body DeviceRegistration registrationResourceModel);

    @GET("channels")
    Call<ChannelItems<Channel>> getChannelsWithAcl(@Query("aclUrlLink") String aclUrlLink, @Query("channelsLimit") int channelsLimit);

    @GET("channels")
    Call<ChannelItems<Channel>> getChannels(@Query("conversationId") String conversationId, @Query("channelsLimit") int channelsLimit);

    @GET
    Call<ChannelItems<Channel>> getChannels(@Url String url);

    @GET("channels/@me/private")
    Call<ChannelItems<Channel>> getPrivateChannels();

    @GET("channels/@me/private")
    Call<ChannelItems<Channel>> getPrivateChannels(@Query("channelsLimit") int limit);
}
