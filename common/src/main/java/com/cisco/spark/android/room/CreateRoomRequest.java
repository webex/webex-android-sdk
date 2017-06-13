package com.cisco.spark.android.room;

import android.net.Uri;

/**
 * Create a SX10 room representation in rooms service using the deviceUrl and setting a name
 */
public class CreateRoomRequest extends DeviceRequest {

    private final String name;

    public CreateRoomRequest(final Uri deviceUrl, final String name) {
        super(deviceUrl);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
