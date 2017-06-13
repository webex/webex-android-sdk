package com.cisco.spark.android.client;

import com.cisco.spark.android.model.ParticipantRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

@Deprecated
public interface HyperSecRestClient {

    @Deprecated // still used in integration tests
    @PATCH("/{keyId}")
    Call<Void> authorizeNewParticipant(@Header("Cisco-Request-Id") String requestId, @Header("Cisco-Device-Url") String deviceId, @Path(value = "keyId", encoded = true) String keyId, @Body ParticipantRequest particpantRequest);
}
