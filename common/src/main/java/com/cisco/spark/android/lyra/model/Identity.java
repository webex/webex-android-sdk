package com.cisco.spark.android.lyra.model;

import java.util.UUID;

public class Identity {
    private UUID id;
    private String orgId;
    private String displayName;

    public Identity(UUID id, String orgId, String displayName) {
        this.id = id;
        this.orgId = orgId;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
