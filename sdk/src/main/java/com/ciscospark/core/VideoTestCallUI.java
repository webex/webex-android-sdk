package com.ciscospark.core;

import android.support.annotation.StringRes;

import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.cisco.spark.android.callcontrol.CallUi;
import com.cisco.spark.android.callcontrol.model.Call;
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
    public void dismissRingback(Call call) {

    }

    @Override
    public void startRingback(Call call) {

    }

    @Override
    public int getRingbackTimeout() {
        return 30;
    }

    @Override
    public void requestUserToUploadLogs(Call call) {

    }

    @Override
    public void reportIceFailure(Call call) {

    }

    @Override
    public void showMessage(@StringRes int stringResourceId) {

    }









}
