package com.cisco.spark.android.metrics.value;

import com.google.gson.annotations.SerializedName;

public class TokenDecodedUnpairedToPairedFlowValue {

    @SerializedName("decoded-announced")
    private Integer decodedAnnounced;

    @SerializedName("announced-roomevent")
    private Integer announcedRoomevent;

    @SerializedName("roomevent-gotroomstate")
    private Integer roomeventGotroomstate;

    private String roomUser;
    private String roomUrl;

    // Other fields used by iOS
    // network-type - TODO: add the network type used, check what values are used

    public TokenDecodedUnpairedToPairedFlowValue(int decodedAnnounced, int announcedRoomevent, int roomeventGotroomstate, String roomUser, String roomUrl) {
        this.decodedAnnounced = decodedAnnounced;
        this.announcedRoomevent = announcedRoomevent;
        this.roomeventGotroomstate = roomeventGotroomstate;

        this.roomUser = roomUser;
        this.roomUrl = roomUrl;
    }

}
