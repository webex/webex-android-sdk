package com.cisco.spark.android.client;

import com.cisco.spark.android.model.KmsInfo;
import com.cisco.spark.android.model.KmsRequestResponseComplete;
import com.cisco.spark.android.model.ParticipantRequest;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface SecRestClient {
    @POST("keys")
    Call<Void> postRequestForKeys(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Query("count") long count);

    @GET("kms/{userid}")
    Call<KmsInfo> getKmsInfo(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Path("userid") String userId);

    @POST("keys/{base64KeyUri}/resources/{conversationId}/authorizations")
    Call<Void> addNewKeyUriToConversation(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Path(value = "base64KeyUri", encoded = true) String base64KeyUri, @Path(value = "conversationId", encoded = true) String conversationId, @Body ParticipantRequest participantRequest);

    @POST("kms/messages")
    Call<Void> postKmsMessage(@Body KmsRequestResponseComplete kmsRequestComplete);

    @GET("ping")
    Observable<HealthCheckResponse> ping();
}
