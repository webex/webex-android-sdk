package com.cisco.spark.android.client;

import com.cisco.spark.android.meetings.WhistlerMeetingResponse;
import com.cisco.spark.android.meetings.WhistlerReservation;
import com.cisco.spark.android.meetings.WhistlerReservationResponse;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface WhistlerTestClient {

    String URL = "http://internal-testing-services.wbx2.com:8084/api/v1";

    @POST("/reservations")
    WhistlerReservationResponse reserveResource(@Body WhistlerReservation reservation);

    @GET("/cmrmeetings/{reservationId}")
    WhistlerMeetingResponse getCmrMeeting(@Path("reservationId") String reservationId);

    @DELETE("/reservations/{reservationId}")
    Void releaseResource(@Path("reservationId") String reservationId);
}
