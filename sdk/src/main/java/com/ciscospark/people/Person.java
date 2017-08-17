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

package com.ciscospark.people;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Person implements Parcelable {

    @SerializedName("id")
    private String id;

    @SerializedName("emails")
    private String[] emails;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("nickName")
    private String nickName;   // may not exist

    @SerializedName("firstName")
    private String firstName;  // may not exist

    @SerializedName("lastName")
    private String lastName;   // may not exist

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("orgId")
    private String orgId;

    @SerializedName("created")
    private String created;

    @SerializedName("lastActivity")
    private String lastActivity;  // may not exist

    @SerializedName("status")
    private String status;        // may not exist

    @SerializedName("type")
    private String type;          // bot/person

    protected Person(Parcel in) {
        id = in.readString();
        emails = in.createStringArray();
        displayName = in.readString();
        nickName = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        avatar = in.readString();
        orgId = in.readString();
        created = in.readString();
        lastActivity = in.readString();
        status = in.readString();
        type = in.readString();
    }

    public Person()
    {

    }

    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeStringArray(emails);
        dest.writeString(displayName);
        dest.writeString(nickName);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(avatar);
        dest.writeString(orgId);
        dest.writeString(created);
        dest.writeString(lastActivity);
        dest.writeString(status);
        dest.writeString(type);
    }

    public String getId() {
        return id;
    }

    public String[] getEmails() {
        return emails;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNickName() {
        return nickName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getCreated() {
        return created;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
}
