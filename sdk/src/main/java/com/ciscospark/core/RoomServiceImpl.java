package com.ciscospark.core;


import android.support.annotation.Nullable;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.room.RoomCallController;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;
import com.cisco.spark.android.room.model.RoomState;
import com.cisco.spark.android.sproximity.SProximityPairingCallback;

import java.util.List;


class RoomServiceImpl implements RoomService {

    private RoomCallController roomCallController = new RoomCallControllerImpl();


    class RoomCallControllerImpl extends RoomCallController {

    }


    @Override
    public void setApplicationController(ApplicationController applicationController) {

    }

    @Override
    public void startRoomService(boolean forceOverride) {

    }

    @Override
    public void startStopRoomService(boolean start) {

    }

    @Override
    public String getRoomName() {
        return null;
    }

    @Override
    public boolean isInRoom() {
        return false;
    }

    @Override
    public boolean isPairedRoomAlreadyInThisLocus(@Nullable LocusData call) {
        return false;
    }

    @Override
    public LocusParticipant pickFirstRemoteJoinedParticipant(List<LocusParticipant> participants, LocusSelfRepresentation self) {
        return null;
    }

    @Override
    public boolean isRoomAvailable() {
        return false;
    }

    @Override
    public RoomState getRoomState() {
        return null;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public void setUseRoomsForMedia(boolean value) {

    }

    @Override
    public boolean useRoomsForMedia() {
        return false;
    }

    @Override
    public void leaveRoom() {

    }

    @Override
    public boolean canHasJoinWithRoom(LocusData call) {
        return false;
    }

    @Override
    public boolean wasRoomCallConnected() {
        return false;
    }

    @Override
    public RoomCallController getCallController() {
        return roomCallController;
    }

    @Override
    public void uploadRoomLogs(String feedbackId) {

    }

    @Override
    public void uploadRoomLogs(LocusKey locusKey, String timestamp) {

    }

    @Override
    public void announceProximityWithToken(String token) {

    }

    @Override
    public void stopLocalDetectorHandoffToMediaEngine(boolean handOff) {

    }

    @Override
    public String getRoomStatusString(String ongoingCallString, String ongoingScreenShareString, String roomAvailableString) {
        return null;
    }

    @Override
    public boolean shouldStart() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void newTokenWithCallback(String token, UltrasoundMetrics ultrasoundMetrics, boolean firstToken, SProximityPairingCallback callback) {

    }

    @Override
    public void newToken(String token, UltrasoundMetrics ultrasoundMetrics, boolean firstToken) {

    }
}
