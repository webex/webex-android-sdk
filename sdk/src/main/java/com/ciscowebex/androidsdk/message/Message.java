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

package com.ciscowebex.androidsdk.message;


import java.util.Date;
import java.util.List;

import com.ciscowebex.androidsdk.space.Space;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * This class represents a Message on Cisco Webex.
 *
 * @since 0.1
 */
public class Message {

    private String _id;

    private String _spaceId;

    private Space.SpaceType _spaceType;

    private String _personId;

    private String _personEmail;

    private String _toPersonId;

    private String _toPersonEmail;

    private Date _created;

    private String _text;

    private transient List<RemoteFile> _remoteFiles;

    private boolean isSelfMentioned;

    public void setId(String id) {
        this._id = id;
    }

    public void setSpaceId(String spaceId) {
        this._spaceId = spaceId;
    }

    public void setSpaceType(Space.SpaceType spaceType) {
        this._spaceType = spaceType;
    }

    public void setPersonId(String personId) {
        this._personId = personId;
    }

    public void setPersonEmail(String personEmail) {
        this._personEmail = personEmail;
    }

    public void setText(String text) {
        this._text = text;
    }

    public void setToPersonId(String toPersonId) {
        this._toPersonId = toPersonId;
    }

    public void setToPersonEmail(String toPersonEmail) {
        this._toPersonEmail = toPersonEmail;
    }

    public void setCreated(Date created) {
        this._created = created;
    }

    /**
     * Return a list of remote files attached to this message.
     * @return A list of remote files attached to this message.
     */
    public List<RemoteFile> getRemoteFiles() {
        return _remoteFiles;
    }

    public void setRemoteFiles(List<RemoteFile> remoteFiles) {
        this._remoteFiles = remoteFiles;
    }

    /**
     * Return The identifier of this message.
     * @return The identifier of this message.
     * @since 0.1
     */
    public String getId() {
        return _id;
    }

    /**
     * Return the identifier of the person who sent this message.
     * @return The identifier of the person who sent this message.
     * @since 0.1
     */
    public String getPersonId() {
        return _personId;
    }

    /**
     * @return The email address of the person who sent this message.
     * @since 0.1
     */
    public String getPersonEmail() {
        return _personEmail;
    }

    /**
     * Return the identifier of the space where this message was posted.
     * @return The identifier of the space where this message was posted.
     * @since 0.1
     */
    public String getSpaceId() {
        return _spaceId;
    }

    /**
     * @return The type of the space where this message was posted.
     * @since 0.1
     */
    public Space.SpaceType getSpaceType() {
        return _spaceType;
    }

    /**
     * Return the content of the message in plain text.
     * @return The content of the message in plain text.
     * @since 0.1
     */
    public String getText() {
        return _text;
    }

    /**
     * Return the identifier of the recipient when sending a private 1:1 message.
     * @return The identifier of the recipient when sending a private 1:1 message.
     * @since 0.1
     */
    public String getToPersonId() {
        return _toPersonId;
    }

    /**
     * Return the email address of the recipient when sending a private 1:1 message
     * @return The email address of the recipient when sending a private 1:1 message.
     * @since 0.1
     */
    public String getToPersonEmail() {
        return _toPersonEmail;
    }

    /**
     * Return the {@link java.util.Date} that the message being created.
     * @return The {@link java.util.Date} that the message being created.
     * @since 0.1
     */
    public Date getCreated() {
        return _created;
    }

    /**
     * Return true if the message is the recepient of the message is mentioned.
     * @return true if the message is the recepient of the message is mentioned.
     */
    public boolean isSelfMentioned() {
        return isSelfMentioned;
    }

    public void setSelfMentioned(boolean selfMentioned) {
        isSelfMentioned = selfMentioned;
    }

    /**
     * @return the message in JSON string format.
     */
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
