package com.cisco.spark.android.provisioning.model;

public class UserActivationResponse {
    private String id;
    private boolean userCreated;
    private boolean dirSync;
    private boolean sso;
    private boolean hasPassword;
    private boolean verificationEmailTriggered;
    private OrgPasswordPolicy orgPasswordPolicy;
    private String verifyEmailURL;

    public String getId() {
        return id;
    }

    public boolean isUserCreated() {
        return userCreated;
    }

    public boolean isDirSync() {
        return dirSync;
    }

    public boolean isSSO() {
        return sso;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public boolean isVerificationEmailTriggered() {
        return verificationEmailTriggered;
    }

    public OrgPasswordPolicy getOrgPasswordPolicy() {
        return orgPasswordPolicy;
    }

    public String getVerifyEmailURL() {
        return verifyEmailURL;
    }
}
