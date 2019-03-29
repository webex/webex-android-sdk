/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.people;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Person contents.
 *
 * @since 0.1
 */
public class Person {

    @SerializedName("id")
    private String _id;

    @SerializedName("emails")
    private String[] _emails;

    @SerializedName("displayName")
    private String _displayName;

    @SerializedName("nickName")
    private String _nickName;

    @SerializedName("firstName")
    private String _firstName;

    @SerializedName("lastName")
    private String _lastName;

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

    public String get_lastActivity() {
        return _lastActivity;
    }

    /**
     * @return The id of this person.
     * @since 0.1
     */
    public String getId() {
        return _id;
    }

    /**
     * @return The emails of this person.
     * @since 0.1
     */
    public String[] getEmails() {
        return _emails;
    }

    /**
     * @return The display name of this person.
     * @since 0.1
     */
    public String getDisplayName() {
        return _displayName;
    }

    /**
     * @return The URL of this person's avatar.
     * @since 0.1
     */
    public String getAvatar() {
        return _avatar;
    }

    /**
     * @return The timestamp that this person being created.
     * @since 0.1
     */
    public Date getCreated() {
        return _created;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * @return The nick name of person
     * @since 1.4
     */
    public String getNickName() {
        return _nickName;
    }

    /**
     * @return The nick first name of person
     * @since 1.4
     */
    public String getFirstName() {
        return _firstName;
    }

    /**
     * @return The nick last name of person
     * @since 1.4
     */
    public String getLastName() {
        return _lastName;
    }

    /**
     * @return The nick org Id of person
     * @since 1.4
     */
    public String getOrgId() {
        return _orgId;
    }

    /**
     * @return The nick type of person, default is "person"
     * @since 1.4
     */
    public String getType() {
        return _type;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public void set_emails(String[] _emails) {
        this._emails = _emails;
    }

    public void set_displayName(String _displayName) {
        this._displayName = _displayName;
    }

    public void set_nickName(String _nickName) {
        this._nickName = _nickName;
    }

    public void set_firstName(String _firstName) {
        this._firstName = _firstName;
    }

    public void set_lastName(String _lastName) {
        this._lastName = _lastName;
    }

    public void set_avatar(String _avatar) {
        this._avatar = _avatar;
    }

    public void set_orgId(String _orgId) {
        this._orgId = _orgId;
    }

    public void set_created(Date _created) {
        this._created = _created;
    }

    public void set_lastActivity(String _lastActivity) {
        this._lastActivity = _lastActivity;
    }

    public void set_status(String _status) {
        this._status = _status;
    }

    public void set_type(String _type) {
        this._type = _type;
    }
}
