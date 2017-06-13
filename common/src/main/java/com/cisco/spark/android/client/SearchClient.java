package com.cisco.spark.android.client;

import com.cisco.spark.android.model.RemoteSearchQueryRequest;
import com.cisco.spark.android.model.ActivitySearchResponse;
import com.cisco.spark.android.model.DirectorySearchQuery;
import com.cisco.spark.android.model.User;

import java.util.List;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

public interface SearchClient {
        @GET("/ping")
        Object pingSearchService();
        @POST("/search")
        ActivitySearchResponse querySearchService(@Body RemoteSearchQueryRequest remoteSearchQueryRequest);
        @POST("/directory")
        List<User> queryDirectory(@Body DirectorySearchQuery directorySearchQuery);

}
