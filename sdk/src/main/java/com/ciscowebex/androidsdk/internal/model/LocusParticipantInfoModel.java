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

import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;

public class LocusParticipantInfoModel {

    private String id;
    private String email;
    private String name;
    private String phoneNumber;
    private String sipUrl;
    private String telUrl;
    private String ownerId;
    private String orgId;
    private boolean isExternal;
    private String primaryDisplayString;
    private String secondaryDisplayString;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        if (ownerId == null) {
            this.ownerId = null;
            return;
        }
        try {
            this.ownerId = ownerId;
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
        return Checker.isEmpty(id) ? email : id;
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

    private static String stripDialableProtocol(String dialable) {
        if (dialable == null) {
            return null;
        }
        return dialable.replaceFirst("^(sip:|tel:)", "");
    }

}
