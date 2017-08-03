package com.cisco.spark.android.client;

import android.net.Uri;

import com.cisco.spark.android.meetings.WhistlerMeetingResponse;
import com.cisco.spark.android.meetings.WhistlerReservation;
import com.cisco.spark.android.meetings.WhistlerReservationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WhistlerTestClient {

    Uri URL = Uri.parse("http://internal-testing-services.wbx2.com:8084/api/v1");

    @POST("reservations")
    Call<WhistlerReservationResponse> reserveResource(@Body WhistlerReservation reservation);

    @GET("cmrmeetings/{reservationId}")
    Call<WhistlerMeetingResponse> getCmrMeeting(@Path("reservationId") String reservationId);

    @DELETE("reservations/{reservationId}")
    Call<Void> releaseResource(@Path("reservationId") String reservationId);
}
