package com.cisco.spark.android.lyra;

public class LyraSpaceOccupantRequest {
    private LyraSpaceOccupantPass pass;
    private String deviceUrl;

    public LyraSpaceOccupantRequest(String deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public LyraSpaceOccupantRequest(LyraSpaceOccupantPass pass, String deviceUrl) {
        this.pass = pass;
        this.deviceUrl = deviceUrl;
    }

    public LyraSpaceOccupantPass getPass() {
        return pass;
    }

    public void setPass(LyraSpaceOccupantPass pass) {
        this.pass = pass;
    }

    public String getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(String deviceUrl) {
        this.deviceUrl = deviceUrl;
    }
}
