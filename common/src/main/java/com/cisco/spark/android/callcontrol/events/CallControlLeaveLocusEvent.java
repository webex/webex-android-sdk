package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusData;

public class CallControlLeaveLocusEvent {

    private final boolean wasMediaFlowing;
    private final boolean wasUCCall;
    private final LocusData locusData;
    private final boolean wasRoomCall;
    private final boolean wasRoomCallConnected;
    private final String locusTrackingID;

    public CallControlLeaveLocusEvent(LocusData locusData, boolean wasMediaFlowing, boolean wasUCCall,
                                      boolean wasRoomCall, boolean wasRoomCallConnected, String locusTrackingID) {
        this.wasMediaFlowing = wasMediaFlowing;
        this.wasUCCall = wasUCCall;
        this.wasRoomCall = wasRoomCall;
        this.wasRoomCallConnected = wasRoomCallConnected;
        this.locusData = locusData;
        this.locusTrackingID = locusTrackingID;
    }

    public static CallControlLeaveLocusEvent callDeclined(LocusData locusData) {
        return new CallControlLeaveLocusEvent(locusData, false, false, false, false, "");
    }

    public boolean wasMediaFlowing() {
        return wasMediaFlowing;
    }

    public LocusData locusData() {
        return locusData; }

    public boolean wasUCCall() {
        return wasUCCall;
    }

    public boolean wasBridgeCall() {
        return locusData.isBridge();
    }

    public boolean wasRoomCall() {
        return wasRoomCall;
    }

    public boolean wasRoomCallConnected() {
        return wasRoomCallConnected;
    }

    public String getLocusTrackingID() {
        return locusTrackingID;
    }
}
