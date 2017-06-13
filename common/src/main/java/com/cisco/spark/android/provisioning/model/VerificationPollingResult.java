package com.cisco.spark.android.provisioning.model;

public class VerificationPollingResult {
    private boolean otpValidated;
    private String cookieName;
    private String cookieValue;
    private String cookieDomain;
    private String orgId;

    private PollingEmailStatus emailStatus;

    public boolean isOtpValidated() {
        return otpValidated;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getCookieValue() {
        return cookieValue;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public String getOrgId() {
        return orgId;
    }

    public PollingEmailStatus getEmailStatus() {
        return emailStatus;
    }

    public String getMessageStatus() {
        if (getEmailStatus() != null)
            return getEmailStatus().getMessageStatus();
        return null;
    }
}
