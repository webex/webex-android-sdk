package com.cisco.spark.android.client;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.requests.CallLocusRequest;
import com.cisco.spark.android.locus.responses.GetLocusListResponse;
import com.cisco.spark.android.locus.responses.JoinLocusResponse;
import com.cisco.spark.android.meetings.GetMeetingInfoType;
import com.cisco.spark.android.locus.model.LocusMeetingInfo;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;


public interface LocusClient {

    @GET("loci")
    Call<GetLocusListResponse> getLoci(@Query("includePMRs") boolean includePMRs);

    @GET("loci/{id}")
    Observable<Locus> getLocusById(@Path("id") String id);

    @POST("loci/call")
    Call<JoinLocusResponse> callLocus(@Body CallLocusRequest callLocusRequest);

    @GET("devices")
    Call<Void> getDevices();

    @GET("loci/meetingInfo/{id}")
    Call<LocusMeetingInfo> getMeetingInfo(@Path("id") String id, @Query("type") GetMeetingInfoType type);

    @PUT("loci/{lid}/meetingInfo")
    Call<LocusMeetingInfo> getOrCreateMeetingInfo(@Path("lid") String locusId);

    // Intended for Integration Test usage only.
    @DELETE("invalidateTestUser/{id}")
    Call<Void> invalidateUserCache(@Path("id") String id);

    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();
}
