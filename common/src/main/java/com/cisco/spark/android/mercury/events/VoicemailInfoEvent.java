package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.voicemail.model.VoicemailInfoData;


public class VoicemailInfoEvent extends MercuryData {
    private VoicemailInfoData vmInfo;

    public VoicemailInfoEvent() {
        super(MercuryEventType.VOICEMAIL_INFO);
    }

    @Override
    public String toString() {
        return "Event: " + vmInfo.toString();
    }

    public VoicemailInfoData getVmInfo() {
        return vmInfo;
    }
}
