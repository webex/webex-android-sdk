package com.cisco.spark.android.voicemail.model;


public class VoicemailInfoData {
    private String userId;
    private boolean mwiStatus;
    private String voicemailPilot;
    private int countUnread;
    private int countRead;


    public VoicemailInfoData() {
        userId = voicemailPilot = "";
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getUserId() {
        return userId;
    }

    public void setMwiStatus(boolean mwiStatus) {
        this.mwiStatus = mwiStatus;
    }
    public boolean getMwiStatus() {
        return mwiStatus;
    }

    public void setVoicemailPilot(String voicemailPilot) {
        this.voicemailPilot = voicemailPilot;
    }
    public String getVoicemailPilot() {
        return voicemailPilot;
    }

    public void setCountUnread(int countUnread) {
        this.countUnread = countUnread;
    }
    public int getCountUnread() {
        return countUnread;
    }

    public void setCountRead(int countRead) {
        this.countRead = countRead;
    }
    public int getCountRead() {
        return countRead;
    }

    public String toString() {
        return "VoicemailInfoData: user ID=" + getUserId() +
                ", mwiStatus=" + (getMwiStatus() ? "ON" : "OFF") +
                ", pilot number=" + getVoicemailPilot() +
                ", unread count=" + getCountUnread() +
                ", read count=" + getCountRead();
    }
}
