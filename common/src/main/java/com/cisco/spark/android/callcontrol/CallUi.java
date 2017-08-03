package com.cisco.spark.android.callcontrol;

import android.support.annotation.StringRes;

import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.meetings.GetMeetingInfoType;

public interface CallUi {
    void requestCallPermissions(CallContext callContext);

    void showInCallUi(CallContext callContext, boolean useRoomForMedia);

    void showMeetingLandingUi(String emailOrUuid, GetMeetingInfoType type, CallInitiationOrigin callInitiationOrigin);

    void showMeetingLobbyUi(LocusKey id);

    void dismissRingback(Call call);

    void startRingback(Call call);

    int getRingbackTimeout();

    void requestUserToUploadLogs(Call call);

    void reportIceFailure(Call call);

    void showMessage(@StringRes int stringResourceId);
}
