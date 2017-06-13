package com.cisco.spark.android.client;

import com.cisco.spark.android.model.AvatarSession;
import com.cisco.spark.android.model.GetAvatarUrlsRequest;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface AvatarClient {
    @POST("profile")
    Call<AvatarSession> createUploadSession(@Body EmptyBody body);

    @PUT("profile/{id}")
    Call<AvatarUrl> updateUploadSession(@Path(value = "id", encoded = false) String id, @Body AvatarUrl url);

    @PUT
    Call<ResponseBody> uploadFile(@Url String url, @Body RequestBody data);

    @POST("profiles/urls")
    Call<JsonObject> getAvatarUrls(@Body List<GetAvatarUrlsRequest.SingleAvatarUrlRequestInfo> avatarUrlRequestInfoList);
}
