package com.cisco.spark.android.wdm;


import com.cisco.spark.android.status.HealthCheckResponse;

import rx.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Interface with the wdm service https://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Spark-Client-Guidelines-for-WDM-&-Mercury
 */
public interface WdmClient {
    @GET("devices")
    Call<DeviceRegistrationList> getDevices(@Header("Authorization") String authorizationHeader);

    @GET("devices/{id}")
    Call<DeviceRegistration> getDevice(@Header("Authorization") String authorizationHeader, @Path("id") String deviceId);

    @POST("devices")
    Call<ResponseBody> createDevice(@Header("Authorization") String authorizationHeader, @Body DeviceInfo info);

    @PUT("devices/{id}")
    Call<ResponseBody> updateDevice(@Header("Authorization") String authorizationHeader, @Path("id") String deviceId, @Body DeviceInfo info);

    @DELETE("devices/{id}")
    Call<Void> deleteDevice(@Header("Authorization") String authorizationHeader, @Path("id") String deviceId);

    @GET("ping")
    Observable<HealthCheckResponse> ping();
}
