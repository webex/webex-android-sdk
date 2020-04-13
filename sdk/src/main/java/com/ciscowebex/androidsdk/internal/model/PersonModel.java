/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import android.text.TextUtils;
import com.google.gson.annotations.SerializedName;

import java.util.*;

public class PersonModel extends ObjectModel {

    public static class RoomPropertiesModel {

        @SerializedName("isModerator")
        private boolean moderator;
        private Date lastAckDate;
        private Date lastSeenActivityDate;
        private String lastSeenActivityUUID;
        private long lastAckTime;

        public boolean isModerator() {
            return moderator;
        }

        public Date getLastAckDate() {
            return lastAckDate;
        }

        public Date getLastSeenActivityDate() {
            return lastSeenActivityDate;
        }

        public String getLastSeenActivityUUID() {
            return lastSeenActivityUUID;
        }
        public long getLastAckTime() {
            return lastAckTime;
        }
    }

    public static final String BOT = "BOT";
    public static final String ROBOT = "ROBOT";
    public static final String PERSON = "PERSON";
    public static final String RESOURCE_ROOM = "RESOURCE_ROOM";
    public static final String LYRA_SPACE = "LYRA_SPACE";
    public static final String MACHINE = "MACHINE";

    @SerializedName("tags")
    private Set<String> participantTagNames = new HashSet<>();
    private String orgId;
    private String entryUUID;
    private RoomPropertiesModel roomProperties;
    private boolean isLocalContact;
    private String emailAddress;
    private String type;

    public PersonModel() {
        super(ObjectModel.Type.person);
    }

    public PersonModel(String email, String entryUUID, String displayName) {
        this();
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(entryUUID)) {
            throw new IllegalArgumentException("Person must have an email or actor uuid");
        }
        setId(entryUUID == null ? email : entryUUID);
        this.emailAddress = email;
        this.entryUUID = entryUUID;
        setDisplayName(displayName);
    }

    public PersonModel(String emailOrUuid) {
        this();
        if (TextUtils.isEmpty(emailOrUuid)) {
            throw new IllegalArgumentException("Person must have an email or actor uuid");
        }
        setId(emailOrUuid);
        if (emailOrUuid.contains("@")) {
            emailAddress = emailOrUuid;
        }
        else {
            entryUUID = emailOrUuid;
        }
    }

    public PersonModel(String email, String displayName) {
        this(email);
        setDisplayName(displayName);
    }

    @Override
    public String getDisplayName() {
        String displayName = super.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = getEmail();
        }
        return displayName;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getUuid() {
        return entryUUID;
    }

    public String getEmail() {
        return emailAddress;
    }

    public void setEmail(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailOrUUID() {
        if (!TextUtils.isEmpty(entryUUID)) {
            return entryUUID;
        } else {
            return emailAddress;
        }
    }

    @Override
    public String getId() {
        if (TextUtils.isEmpty(getUuid())) {
            return super.getId();
        }
        return getUuid();
    }

    public RoomPropertiesModel getRoomProperties() {
        return roomProperties;
    }

    public void setUuid(String entryUUID) {
        this.entryUUID = entryUUID;
        if (!TextUtils.isEmpty(entryUUID))
            setId(entryUUID);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PersonModel)) {
            return false;
        }
        PersonModel that = (PersonModel) o;
        if (!TextUtils.isEmpty(entryUUID) && entryUUID.equals(that.entryUUID)) {
            return true;
        }
        return !TextUtils.isEmpty(getEmail()) && getEmail().equals(that.getEmail());
    }

}
