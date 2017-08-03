package com.cisco.spark.android.Hecate;

import java.util.UUID;


public class CMRMeetingClaimRequest {

    private UUID userId;
    private String passcode;
    private boolean preferred;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    @Override
    public String toString() {
        return "CMRMeetingClaimRequest{" +
                "userId='" + userId + '\'' +
                ",passcode= ****" +
                ",preferred='" + preferred + '\'' +
                '}';
    }
}
