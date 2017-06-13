package com.cisco.spark.android.authenticator;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OAuth2Client {

    @POST("authorize")
    @FormUrlEncoded
    Call<ResponseBody> postCookie(@Header("Cookie") String cookieHeader, @Field("response_type") String responseType, @Field("redirect_uri") String redirectUri, @Field("scope") String scope, @Field("client_id") String clientId, @Field("state") String state);

    @POST("access_token")
    @FormUrlEncoded
    Call<OAuth2Tokens> postCode(@Header("Authorization") String authorizationHeader, @Field("grant_type") String grantType, @Field("redirect_uri") String redirectUri, @Field("scope") String scope, @Field("code") String code);

    @POST("access_token")
    @FormUrlEncoded
    Call<OAuth2AccessToken> refreshToken(@Field("grant_type") String grantType, @Field("refresh_token") String refreshToken, @Field("client_id") String clientId, @Field("client_secret") String clientSecret);

    @POST("access_token")
    @FormUrlEncoded
    Call<OAuth2AccessToken> reduceScope(@Field("grant_type") String grantType, @Field("token") String token, @Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("scope") String scope);

    @POST("access_token")
    @FormUrlEncoded
    Call<OAuth2AccessToken> clientCredentials(@Field("grant_type") String grantType, @Field("scope") String scope, @Field("client_id") String clientId, @Field("client_secret") String clientSecret);

    @POST("revoke")
    @FormUrlEncoded
    Call<ResponseBody> revokeToken(@Header("Authorization") String authorizationHeader, @Field("token") String token, @Field("token_type_hint") String tokenTypeHint);
}
