package com.cisco.spark.android.client;

import com.cisco.spark.android.model.KmsInfo;
import com.cisco.spark.android.model.KmsRequestResponseComplete;
import com.cisco.spark.android.model.ParticipantRequest;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface SecRestClient {
    @POST("/keys")
    void postRequestForKeys(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Query("count") long count, Callback<Void> cb);

    @GET("/kms/{userid}")
    KmsInfo getKmsInfo(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Path("userid") String userId);

    @POST("/keys/{base64KeyUri}/resources/{conversationId}/authorizations")
    void addNewKeyUriToConversation(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Path(value = "base64KeyUri", encode = false) String base64KeyUri, @Path(value = "conversationId", encode = false) String conversationId, @Body ParticipantRequest participantRequest, Callback<Void> cb);

    @POST("/kms/messages")
    void postKmsMessage(@Body KmsRequestResponseComplete kmsRequestComplete, Callback<Void> cb);
}
