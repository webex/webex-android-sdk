package com.cisco.spark.android.meetings;

public class WhistlerRequestMetaData {

    private String emailAddress;
    private WhistlerLoginType loginType;

    public WhistlerRequestMetaData(String emailAddress, WhistlerLoginType loginType) {
        this.emailAddress = emailAddress;
        this.loginType = loginType;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public WhistlerLoginType getLoginType() {
        return loginType;
    }
}
