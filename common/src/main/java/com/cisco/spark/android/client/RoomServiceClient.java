package com.cisco.spark.android.client;

import com.cisco.spark.android.locus.responses.LocusResponse;
import com.cisco.spark.android.room.AnnounceProximityRequest;
import com.cisco.spark.android.room.AnnounceProximityResponse;
import com.cisco.spark.android.room.FindRoomByDeviceResponse;
import com.cisco.spark.android.room.LeaveRoomRequest;
import com.cisco.spark.android.room.RequestTpLogsRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface RoomServiceClient {

    /**
     * Announce that we are in proximity of a room
     *
     * @param proximityRequest token and deviceUrl
     */
    @POST("rooms/announce-proximity")
    Call<AnnounceProximityResponse> announceProximity(@Body AnnounceProximityRequest proximityRequest);

    /** request proximity status for the room
     *
     * @param base64DeviceUrl this client's base64 encoded deviceUrl
     */
    @GET("rooms/proximity-status")
    Call<FindRoomByDeviceResponse> requestProximityStatus(
            @Query(value = "deviceUrl", encoded = true) String base64DeviceUrl);

    /**
     * Request a paired device to upload its logs
     * If this device is not currently paired to the room, the response will be 403 permission denied
     */
    @POST
    Call<LocusResponse> requestTpLogs(@Url String url, @Body RequestTpLogsRequest request);

    /**
     * The client announces it is leaving the room, needing to re-gain pairing to be in proximity again
     *
     * It is not part of the standard client api, but is used when the user manually override
     * proximity toggle to immediately notify RoomsService and the Room that the client is no
     * longer in proximity
     */
    @POST("rooms/leave-room")
    Call<ResponseBody> leaveRoom(@Body LeaveRoomRequest leaveRoomRequest);
}
