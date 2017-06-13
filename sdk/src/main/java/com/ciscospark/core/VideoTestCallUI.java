package com.ciscospark.core;

import android.support.annotation.StringRes;

import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.cisco.spark.android.callcontrol.CallUi;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.meetings.GetMeetingInfoType;
import com.cisco.spark.android.sync.ActorRecord;

import java.util.List;


class VideoTestCallUI implements CallUi {


    @Override
    public void requestCallPermissions(CallContext callContext) {

    }

    @Override
    public void showInCallUi(CallContext callContext, boolean useRoomForMedia) {

    }

    @Override
    public void showMeetingLandingUi(String emailOrUuid, GetMeetingInfoType type, CallInitiationOrigin callInitiationOrigin) {

    }

    @Override
    public void showMeetingLobbyUi(LocusKey id) {

    }

    @Override
    public void dismissRingback(LocusKey locusKey) {

    }

    @Override
    public void startRingback(LocusKey locusKey) {

    }

    @Override
    public int getRingbackTimeout() {
        return 30;
    }

    @Override
    public void requestUserToUploadLogs(LocusKey locusKey) {

    }

    @Override
    public void reportIceFailure(LocusKey locusKey) {

    }

    @Override
    public void showMessage(@StringRes int stringResourceId) {

    }

    @Override
    public void showMeetingPeopleUi(long eventId, List<ActorRecord> participantsList) {

    }
}
