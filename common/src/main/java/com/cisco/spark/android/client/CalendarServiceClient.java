package com.cisco.spark.android.client;

import com.cisco.spark.android.model.CalendarMeeting;
import com.cisco.spark.android.model.EventToRoomMapping;
import com.cisco.spark.android.status.HealthCheckResponse;

import rx.Observable;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CalendarServiceClient {
    @POST("conversations")
    Call<ResponseBody> mapEventToRoom(@Body EventToRoomMapping body);

    @GET("ping")
    Observable<HealthCheckResponse> ping();

    @GET("conversations/{base64EncodedEventId}")
    Call<EventToRoomMapping> getRoomForEvent(@Path("base64EncodedEventId") String base64EncodedEventId);

    @GET("calendarEvents/{id}")
    Observable<CalendarMeeting> getMeetingResource(@Path("id") String id);
}
