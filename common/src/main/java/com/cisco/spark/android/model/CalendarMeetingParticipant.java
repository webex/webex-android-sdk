package com.cisco.spark.android.model;

import com.cisco.spark.android.util.CryptoUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.text.ParseException;

public class CalendarMeetingParticipant implements Cloneable {
    private String id;
    private String type;
    private String responseType;
    private String participantType;
    private String orgId;
    @SerializedName("encryptedEmailAddress")
    private String emailAddress;
    @SerializedName("encryptedName")
    private String name;

    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        String plainEmailAddress = CryptoUtils.decryptFromJwe(key, emailAddress);
        String plainName = CryptoUtils.decryptFromJwe(key, name);
        emailAddress = plainEmailAddress;
        name = plainName;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getParticipantType() {
        return participantType;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public void setParticipantType(String participantType) {
        this.participantType = participantType;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected CalendarMeetingParticipant clone() {
        try {
            return (CalendarMeetingParticipant) super.clone();
        } catch (CloneNotSupportedException e) {
            Ln.e(false, e, "Unable to clone CalendarMeetingParticipant");
        }
        return null;
    }
}
