package com.cisco.spark.android.locus.model;

import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.util.UUID;

import static com.cisco.spark.android.util.NameUtils.stripDialableProtocol;

public class LocusParticipantInfo {
    private String id;
    private String email;
    private String name;
    private String phoneNumber;
    private String sipUrl;
    private String telUrl;
    private UUID ownerId;
    private String orgId;
    private boolean isExternal;
    private String primaryDisplayString;
    private String secondaryDisplayString;

    public LocusParticipantInfo() {
    }

    public LocusParticipantInfo(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        if (ownerId == null) {
            this.ownerId = null;
            return;
        }
        try {
            UUID uuid = UUID.fromString(ownerId);
            this.ownerId = uuid;
        } catch (IllegalArgumentException exception) {
            Ln.w(exception, "Wrong format for uuid");
            this.ownerId = null;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSipUrl() {
        return sipUrl;
    }

    public void setSipUrl(String sipUrl) {
        this.sipUrl = sipUrl;
    }

    public String getTelUrl() {
        return telUrl;
    }

    public void setTelUrl(String telUrl) {
        this.telUrl = telUrl;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }


    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean external) {
        isExternal = external;
    }

    public String getPrimaryDisplayString() {
        return primaryDisplayString;
    }

    public void setPrimaryDisplayString(String primaryDisplayString) {
        this.primaryDisplayString = primaryDisplayString;
    }

    public String getSecondaryDisplayString() {
        return secondaryDisplayString;
    }

    public void setSecondaryDisplayString(String secondaryDisplayString) {
        this.secondaryDisplayString = secondaryDisplayString;
    }

    public String getIdOrEmail() {
        return Strings.isEmpty(id) ? email : id;
    }

    public String getDisplayName() {
        String displayName = "";

        if (!isExternal) {
            displayName = getName();
        } else {
            displayName = getPrimaryDisplayString();
        }

        return stripDialableProtocol(displayName);
    }
}
