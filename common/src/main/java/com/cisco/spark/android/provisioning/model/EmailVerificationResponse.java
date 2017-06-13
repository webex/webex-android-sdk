package com.cisco.spark.android.provisioning.model;

import com.google.gson.annotations.SerializedName;

public class EmailVerificationResponse {
    private boolean showPasswordPage;
    private boolean isSSOUser;
    private boolean isUserCreated;
    private String messageId;

    @SerializedName("eqp")
    private String encryptedQueryString;

    // True if the user has a password set OR is in an SSO enabled domain
    public boolean shouldShowPasswordPage() {
        return showPasswordPage;
    }

    // True if the email domain is SSO enabled
    public boolean isSSOUser() {
        return isSSOUser;
    }

    // True if the user was created as a result of this request
    public boolean isUserCreated() {
        return isUserCreated;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getEncryptedQueryString() {
        return encryptedQueryString;
    }
}
