package com.cisco.spark.android.client;

import com.cisco.spark.android.acl.Acl;
import com.cisco.spark.android.acl.AclLinkRequest;
import com.cisco.spark.android.whiteboard.WhiteboardKmsMessage;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Url;
import retrofit2.http.HTTP;
import retrofit2.http.Path;

public interface AclClient {

    @GET
    Call<Acl> get(@Url String url);

    @PUT("links")
    Call<Response<ResponseBody>> linkAcl(@Body AclLinkRequest request);

    @HTTP(method = "DELETE", path = "acls/{aclId}/people/{userId}", hasBody = true)
    Call<Void> removeUserFromAcl(@Path("aclId") String aclId, @Path("userId") String userId, @Body WhiteboardKmsMessage kmsMessage);

}
