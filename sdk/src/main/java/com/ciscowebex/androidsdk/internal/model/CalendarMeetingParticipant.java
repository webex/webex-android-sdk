package com.ciscowebex.androidsdk.internal.model;

import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
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

    public void decrypt(KeyObject key) {
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

    @Override
    public CalendarMeetingParticipant clone() {
        try {
            return (CalendarMeetingParticipant) super.clone();
        } catch (CloneNotSupportedException e) {
            Ln.e(false, e, "Unable to clone CalendarMeetingParticipant");
        }
        return null;
    }
}
