package com.cisco.spark.android.client;


import com.cisco.spark.android.Hecate.CMRMeetingClaimRequest;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.POST;
import retrofit.http.Path;

public interface HecateClient {

    @POST("/cmrmeetings/{meeting_uri}/claim")
    Response claimPMR(
            @Path(value = "meeting_uri") String meetingURI,
            @Body CMRMeetingClaimRequest cmrMeetingClaimRequest);

    @DELETE("/cmrmeetings/{meeting_uri}/claim")
    Void releasePMR(@Path(value = "meeting_uri") String meetingURI);
}
