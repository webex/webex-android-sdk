package com.cisco.spark.android.model;

import com.cisco.spark.android.authenticator.OAuth2;

import java.util.*;

public class CreateTestUserRequest {
    private final Set<String> entitlements;
    private final String displayName;
    private final String type;
    private final String machineType;
    private final String password;
    private final String orgId;
    private final String scopes;
    private final String clientId;
    private final String clientSecret;

    private String emailTemplate;

    public CreateTestUserRequest(String email, String name, String password, Set<String> entitlements, String orgId) {
        this(email, name, password, entitlements, orgId, null, null, OAuth2.UBER_SCOPES, null, null);
    }

    public CreateTestUserRequest(String email, String name, String password, Set<String> entitlements, String orgId, String type, String machineType, String scopes, String clientId, String clientSecret) {
        this.entitlements = entitlements;
        this.emailTemplate = email;
        this.displayName = name;
        this.type = type;
        this.machineType = machineType;
        this.password = password;
        this.orgId = orgId;
        this.scopes = scopes;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getEmail() {
        return emailTemplate;
    }

    public void setEmail(String email) {
        this.emailTemplate = email;
    }

    public String getName() {
        return displayName;
    }
}
