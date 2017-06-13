package com.cisco.spark.android.client;

import com.cisco.spark.android.room.CreateRoomRequest;
import com.cisco.spark.android.room.model.TpRoomState;
import com.cisco.spark.android.room.model.drone.UltrasoundTokenBatch;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface RoomEmulatorServiceClient {

    /**
     * Retrieves a batch of tokens, that can be played back to emulate a sx10 room
     *
     * @return a set of tokens and the interval to play them in
     */
    @POST
    Call<UltrasoundTokenBatch> retrieveTokens(@Url String url);

    /**
     * This represents a SX10 Room as a client and is used for emulating the room to create a
     * Room in RoomsService to be able to request tokens for playback
     */

    /**
     * Used by SX10 room emulation to check if there is a room representation already created for
     * the device
     *
     * @param base64DeviceUrl base64 encoded device url for the device that should emulate a room
     * @return The room state
     */
    @GET("rooms/roomForDevice/{base64DeviceUrl}")
    Call<TpRoomState> findRoomForDevice(@Path(value = "base64DeviceUrl", encoded = true) String base64DeviceUrl);

    /**
     * Used by SX10 room emulation to create a new room representation
     *
     * @param createRoomRequest deviceUrl and name for the room
     * @return The room state
     */
    @POST("rooms")
    Call<TpRoomState> createRoom(@Body CreateRoomRequest createRoomRequest);
}
