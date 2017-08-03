package com.cisco.spark.android.callcontrol.events;


import com.cisco.spark.android.voicemail.model.VoicemailInfoData;

public class CallControlVoicemailInfoEvent {
    private VoicemailInfoData vmInfo;

    public CallControlVoicemailInfoEvent(VoicemailInfoData vmInfo) {
        this.vmInfo = vmInfo;
    }

    public VoicemailInfoData getVmInfo() {
        return vmInfo;
    }
}
