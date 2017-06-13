package com.cisco.spark.android.room;

import android.support.annotation.Nullable;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.room.model.RoomState;

import java.util.List;

public interface RoomService extends Component, TokenListener {

    // Seems this should move to Component or a separate interface
    void setApplicationController(ApplicationController applicationController);

    /**
     *
     * @param forceOverride used to force override the user disable as it might not have been set yet by the operation.
     */
    void startRoomService(boolean forceOverride);

    void startStopRoomService(boolean start);

    String getRoomName();

    boolean isInRoom();

    boolean isPairedRoomAlreadyInThisLocus(@Nullable LocusData call);

    LocusParticipant pickFirstRemoteJoinedParticipant(List<LocusParticipant> participants, LocusSelfRepresentation self);

    boolean isRoomAvailable();

    RoomState getRoomState();

    boolean isStarted();

    void setUseRoomsForMedia(boolean value);

    boolean useRoomsForMedia();

    void leaveRoom();

    boolean canHasJoinWithRoom(LocusData call);

    boolean wasRoomCallConnected();

    RoomCallController getCallController();

    void uploadRoomLogs(String feedbackId);

    void uploadRoomLogs(LocusKey locusKey, String timestamp);

    void announceProximityWithToken(String token);

    /**
     * Do an immediate release of the microphone to potentially hand over to media engine for
     * audio recording
     * @param handOff if handing of to media engine or not.
     */
    void stopLocalDetectorHandoffToMediaEngine(boolean handOff);

    String getRoomStatusString(String ongoingCallString, String ongoingScreenShareString, String roomAvailableString);
}
