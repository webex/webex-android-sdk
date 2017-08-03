package com.cisco.spark.android.provisioning;

import com.cisco.spark.android.model.LogMetadataRequest;
import com.cisco.spark.android.model.UrlResponse;
import com.cisco.spark.android.provisioning.model.UserActivationRequest;
import com.cisco.spark.android.provisioning.model.UserActivationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.cisco.spark.android.client.AdminClient.LogURLResponse;
import static com.cisco.spark.android.client.AdminClient.LogUploadRequest;

public interface ProvisioningClient {

    /**
     * Fetch a URL where we can upload a file (usually logs) to be saved next to this user in the admin service.
     * @param logRequest - the filename passed here should match the filename passed to the upload
     */
    @POST("logs/url")
    Call<LogURLResponse> getUploadFileUrl(@Body LogUploadRequest logRequest);

    /**
     * Associate name/value pair metadata with an uploaded log filename
     * @param request
     * @return
     */
    @POST("logs/meta")
    public Call<UrlResponse> setLogMetadata(@Body LogMetadataRequest request);

    @POST("users/activations")
    public Call<UserActivationResponse> postActivation(@Header("X-Prelogin-UserId") String header, @Body UserActivationRequest request);
}
