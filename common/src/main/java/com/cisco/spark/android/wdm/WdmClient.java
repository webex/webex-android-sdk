package com.cisco.spark.android.wdm;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Interface with the wdm service
 * https://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Spark-Client-Guidelines-for-WDM-&-Mercury
 */
public interface WdmClient {
    @GET("/devices")
    DeviceRegistrationList getDevices(@Header("Authorization") String authorizationHeader);

    @GET("/devices/{id}")
    DeviceRegistration getDevice(@Header("Authorization") String authorizationHeader, @Path("id") String deviceId);

    @POST("/devices")
    Response createDevice(@Header("Authorization") String authorizationHeader, @Body DeviceInfo info);

    @PUT("/devices/{id}")
    Response updateDevice(@Header("Authorization") String authorizationHeader, @Path("id") String deviceId, @Body DeviceInfo info);

    @DELETE("/devices/{id}")
    Void deleteDevice(@Header("Authorization") String authorizationHeader, @Path("id") String deviceId);
}
