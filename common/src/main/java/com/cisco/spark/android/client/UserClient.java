package com.cisco.spark.android.client;

import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.model.CreateTestUserRequest;
import com.cisco.spark.android.model.DeleteTestUserRequest;
import com.cisco.spark.android.model.LoginTestUserRequest;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.model.UserEmailRequest;
import com.cisco.spark.android.model.UserIdentityKey;
import com.cisco.spark.android.model.UserSession;
import com.cisco.spark.android.status.HealthCheckResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface UserClient {

    @POST("users/test_users_s")
    Call<UserSession> createTestUser(@Header("Authorization") String authorization, @Body CreateTestUserRequest request);

    @POST("users/test_users/login")
    Call<OAuth2Tokens> loginTestUser(@Body LoginTestUserRequest request);

    @POST("users/test_users/delete")
    Call<Void> deleteTestUser(@Header("Authorization") String authorization, @Body DeleteTestUserRequest request);

    @GET("users")
    Call<User> getUser(@Header("Authorization") String authorization);

    @GET("users/directory")
    Call<List<User>> getUserForSpecificEmailID(@Header("Authorization") String authorization, @Query("q") String emailId);

    @GET("users/{email}")
    Call<User> getUserID(@Header("Authorization") String authorization, @Path("email") String email);

    @POST("users?shouldCreateUsers=1")
    Call<Map<String, UserIdentityKey>> getOrCreateUserID(@Header("Authorization") String authorization, @Body List<UserEmailRequest> emails);

    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();
}
