package com.cisco.spark.android.client;

import com.cisco.spark.android.model.EventToRoomMapping;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface CalendarServiceClient {
    @POST("/conversations")
    Response mapEventToRoom(@Body EventToRoomMapping body);

    @GET("/conversations/{base64EncodedEventId}")
    EventToRoomMapping getRoomForEvent(@Path("base64EncodedEventId") String base64EncodedEventId);
}
