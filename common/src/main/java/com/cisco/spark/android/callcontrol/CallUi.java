package com.cisco.spark.android.callcontrol;

import android.support.annotation.StringRes;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.meetings.GetMeetingInfoType;
import com.cisco.spark.android.sync.ActorRecord;

import java.util.List;

public interface CallUi {
    void requestCallPermissions(CallContext callContext);

    void showInCallUi(CallContext callContext, boolean useRoomForMedia);

    void showMeetingLandingUi(String emailOrUuid, GetMeetingInfoType type, CallInitiationOrigin callInitiationOrigin);

    void showMeetingLobbyUi(LocusKey id);

    void dismissRingback(LocusKey locusKey);

    void startRingback(LocusKey locusKey);

    int getRingbackTimeout();

    void requestUserToUploadLogs(LocusKey locusKey);

    void reportIceFailure(LocusKey locusKey);

    void showMessage(@StringRes int stringResourceId);

    void showMeetingPeopleUi(long eventId, List<ActorRecord> participantsList);

}
