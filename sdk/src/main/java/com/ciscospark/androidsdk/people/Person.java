/*
 * Copyright (c) 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.people;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Person {

    @SerializedName("id")
    private String _id;

    @SerializedName("emails")
    private String[] _emails;

    @SerializedName("displayName")
    private String _displayName;

    @SerializedName("nickName")
    private String _nickName;   // may not exist

    @SerializedName("firstName")
    private String _firstName;  // may not exist

    @SerializedName("lastName")
    private String _lastName;   // may not exist

    @SerializedName("avatar")
    private String _avatar;

    @SerializedName("orgId")
    private String _orgId;

    @SerializedName("created")
    private Date _created;

    @SerializedName("lastActivity")
    private String _lastActivity;  // may not exist

    @SerializedName("status")
    private String _status;        // may not exist

    @SerializedName("type")
    private String _type;          // bot/person

    public String getId() {
        return _id;
    }

    public String[] getEmails() {
        return _emails;
    }

    public String getDisplayName() {
        return _displayName;
    }

    public String getNickName() {
        return _nickName;
    }

    public String getFirstName() {
        return _firstName;
    }

    public String getLastName() {
        return _lastName;
    }

    public String getAvatar() {
        return _avatar;
    }

    public String getOrgId() {
        return _orgId;
    }

    public Date getCreated() {
        return _created;
    }

    public String getLastActivity() {
        return _lastActivity;
    }

    public String getStatus() {
        return _status;
    }

    public String getType() {
        return _type;
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
