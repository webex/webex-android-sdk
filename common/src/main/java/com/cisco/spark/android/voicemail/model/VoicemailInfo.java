package com.cisco.spark.android.voicemail.model;

public class VoicemailInfo {

    private VoicemailInfoData vmInfo;


    public VoicemailInfo() {
        vmInfo = new VoicemailInfoData();
    }

    public VoicemailInfo(String userId, boolean mwiStatus, String voicemailPilot, int countUnread, int countRead) {
        this();
        vmInfo.setUserId(userId);
        vmInfo.setMwiStatus(mwiStatus);
        vmInfo.setVoicemailPilot(voicemailPilot);
        vmInfo.setCountUnread(countUnread);
        vmInfo.setCountRead(countRead);
    }

    public VoicemailInfoData getVmInfoData() {
        return vmInfo;
    }

    public String toString() {
        return vmInfo.toString();
    }
}
