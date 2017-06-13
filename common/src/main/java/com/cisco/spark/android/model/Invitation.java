package com.cisco.spark.android.model;

public final class Invitation {
    private String id;
    private String email;
    private String displayName;
    private String message;
    private String orgId;
    private String eqp;
    private int status;

    public Invitation(String email, String name) {
        this.email = email;
        this.displayName = name;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessage() {
        return message;
    }

    public String getId() {
        return id;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getEqp() {
        return eqp;
    }

    public int getStatus() {
        return status;
    }
}
