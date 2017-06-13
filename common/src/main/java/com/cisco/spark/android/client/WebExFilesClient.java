package com.cisco.spark.android.client;

import com.cisco.spark.android.model.CompleteContentUploadSession;
import com.cisco.spark.android.model.ContentUploadSession;
import com.cisco.spark.android.model.File;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Url;

public interface WebExFilesClient {
    @PUT
    Call<Void> uploadFile(@Url String url, @Body RequestBody data);

    @POST
    Call<ContentUploadSession> createUploadSession(@Url String url);

    @POST
    Call<File> updateUploadSession(@Url String url, @Body CompleteContentUploadSession completeContentUploadSession);
}
