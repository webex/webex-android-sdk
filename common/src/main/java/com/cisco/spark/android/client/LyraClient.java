package com.cisco.spark.android.client;

import com.cisco.spark.android.lyra.LyraSpaceOccupantRequest;
import com.cisco.spark.android.lyra.AudioStateResponse;
import com.cisco.spark.android.lyra.BindingRequest;
import com.cisco.spark.android.lyra.BindingResponse;
import com.cisco.spark.android.lyra.BindingResponses;
import com.cisco.spark.android.lyra.VolumeRequest;
import com.cisco.spark.android.room.FindRoomByDeviceResponse;
import com.cisco.spark.android.room.LyraSpaceResponse;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface LyraClient {

    // bind
    @POST("spaces/{spaceId}/bindings")
    Call<BindingResponse> bind(@Path("spaceId") String spaceId, @Body BindingRequest request);

    //unbind
    @DELETE("spaces/{spaceId}/bindings/{bindingUrl}")
    Call<Void> unbind(@Path("spaceId") String spaceId, @Path("bindingUrl") String bindingUrl, @Query("kmsMessage") String kmsMessage);

    @GET("spaces/{spaceId}/bindings")
    Call<BindingResponses> getBindings(@Path("spaceId") String spaceId);

    @GET("spaces/{spaceId}/audio")
    Call<AudioStateResponse> getAudioState(@Path(value = "spaceId") String id);

    @POST("spaces/{spaceId}/audio/volume/actions/decrease/invoke")
    Call<Void> decreaseVolume(@Path(value = "spaceId") String id);

    @POST("spaces/{spaceId}/audio/volume/actions/increase/invoke")
    Call<Void> increaseVolume(@Path(value = "spaceId") String id);

    @POST("spaces/{spaceId}/audio/microphones/actions/mute/invoke")
    Call<Void> mute(@Path(value = "spaceId") String id);

    @POST("spaces/{spaceId}/audio/microphones/actions/un-mute/invoke")
    Call<Void> unMute(@Path(value = "spaceId") String id);

    @POST("spaces/{spaceId}/audio/volume/actions/set/invoke")
    Call<Void> setVolume(@Path(value = "spaceId") String id, @Body VolumeRequest content);

    @HTTP(method = "DELETE", path = "spaces/{spaceId}/occupants/@me", hasBody = true)
    Call<Void> leaveRoom(@Path(value = "spaceId") String spaceId, @Body LyraSpaceOccupantRequest request);

    @GET("rooms/proximity-status")
    Call<FindRoomByDeviceResponse> getLyraProximityStatus(@Query(value = "deviceUrl", encoded = true) String base64DeviceUrl);

    @PUT
    Call<Void> addMeToSpace(@Url String url, @Body LyraSpaceOccupantRequest request);

    @GET
    Call<LyraSpaceResponse> getSpaceStatus(@Url String url);


    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();
}


