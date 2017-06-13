package com.cisco.spark.android.metrics.value;

import com.google.gson.annotations.SerializedName;

public class AltoMetricsAppToForegroundUnpairedToPairedFlowValue {

    @SerializedName("foreground-listen")
    private Integer foregroundListen;

    @SerializedName("listen-decoded")
    private Integer listenDecoded;

    @SerializedName("decoded-announced")
    private Integer decodedAnnounced;

    @SerializedName("announced-roomevent")
    private Integer announcedRoomevent;

    @SerializedName("roomevent-gotroomstate")
    private Integer roomeventGotroomstate;

    @SerializedName("total-pairing")
    private Integer totalPairing;

    private String roomUser;
    private String roomUrl;

    // Other fields used by iOS
    // network-type - TODO: add the network type used, check what values are used
    // duckedBeforeListen - ios only for now (this is used to indicate that ios could not use the microhpone since another app is playing music

    public AltoMetricsAppToForegroundUnpairedToPairedFlowValue(int foregroundListen, int listenDecoded, int decodedAnnounced, int announcedRoomevent, int roomeventGotroomstate, int totalPairing, String roomUser, String roomUrl) {
        this.foregroundListen = foregroundListen;
        this.listenDecoded = listenDecoded;
        this.decodedAnnounced = decodedAnnounced;
        this.announcedRoomevent = announcedRoomevent;
        this.roomeventGotroomstate = roomeventGotroomstate;
        this.totalPairing = totalPairing;
        this.roomUser = roomUser;
        this.roomUrl = roomUrl;
    }

}
