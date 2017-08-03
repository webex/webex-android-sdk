package com.cisco.spark.android.client;


import com.cisco.spark.android.Hecate.CMRMeetingClaimRequest;
import com.cisco.spark.android.locus.model.LocusMeetingInfo;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

public interface HecateClient {
    @GET("ping")
    Observable<HealthCheckResponse> ping();

    @POST("cmrmeetings/{meeting_uri}/claim")
    Call<LocusMeetingInfo> claimPMR(
            @Path(value = "meeting_uri") String meetingURI,
            @Body CMRMeetingClaimRequest cmrMeetingClaimRequest);

    @DELETE("cmrmeetings/{meeting_uri}/claim")
    Call<Void> releasePMR(@Path(value = "meeting_uri") String meetingURI);
}
