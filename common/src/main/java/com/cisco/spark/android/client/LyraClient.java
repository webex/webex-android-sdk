package com.cisco.spark.android.client;

import com.cisco.spark.android.lyra.AudioStateResponse;
import com.cisco.spark.android.lyra.BindingRequest;
import com.cisco.spark.android.lyra.BindingResponse;
import com.cisco.spark.android.lyra.BindingResponses;
import com.cisco.spark.android.lyra.VolumeRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
}


