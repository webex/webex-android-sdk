package com.cisco.spark.android.client;

import com.cisco.spark.android.model.ActivitySearchResponse;
import com.cisco.spark.android.model.DirectorySearchQuery;
import com.cisco.spark.android.model.RemoteSearchQueryRequest;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.status.HealthCheckResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Observable;

public interface SearchClient {
    @GET("ping")
    Object pingSearchService();

    @POST("search")
    Call<ActivitySearchResponse> querySearchService(@Body RemoteSearchQueryRequest remoteSearchQueryRequest);

    @POST("directory")
    Call<List<User>> queryDirectory(@Body DirectorySearchQuery directorySearchQuery);

    @GET("ping")
    Observable<HealthCheckResponse> ping();
}
