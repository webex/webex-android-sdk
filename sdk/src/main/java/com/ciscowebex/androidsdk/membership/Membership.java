/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.membership;

import java.util.Date;

import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.internal.model.ConversationModel;
import com.ciscowebex.androidsdk.internal.model.PersonModel;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Membership contents.
 *
 * @since 0.1
 */
public class Membership {

    @SerializedName("id")
    private String _id;

    @SerializedName("personId")
    private String _personId;

    @SerializedName("personEmail")
    private String _personEmail;

    @SerializedName("personDisplayName")
    private String _personDisplayName;

    @SerializedName("personOrgId")
    private String _personOrgId;

    @SerializedName(value = "roomId", alternate = "spaceId")
    private String _spaceId;

    @SerializedName("isModerator")
    private boolean _isModerator;

    @SerializedName("isMonitor")
    @Deprecated
    private boolean _isMonitor;

    @SerializedName("created")
    private Date _created;

    protected Membership(ConversationModel conversation, PersonModel person, String clusterId) {
        _id = new WebexId(WebexId.Type.MEMBERSHIP, clusterId, person.getId() + ":" + conversation.getId()).getBase64Id();
        _spaceId = new WebexId(WebexId.Type.ROOM, clusterId, conversation.getId()).getBase64Id();
        _personId = new WebexId(WebexId.Type.PEOPLE, WebexId.DEFAULT_CLUSTER, person.getId()).getBase64Id();
        _personEmail = person.getEmail();
        _personDisplayName = person.getDisplayName();
        _personOrgId = new WebexId(WebexId.Type.ORGANIZATION, WebexId.DEFAULT_CLUSTER, person.getOrgId()).getBase64Id();
        PersonModel.RoomPropertiesModel roomProperties = person.getRoomProperties();
        if (roomProperties != null) {
            _isModerator = roomProperties.isModerator();
            _isMonitor = _isModerator;
        }
        _created = person.getPublished();
    }

    protected Membership(ActivityModel activity, String clusterId) {
        this._created = activity.getPublished();
        if (activity.getVerb() == ActivityModel.Verb.hide) {
            this._spaceId = new WebexId(WebexId.Type.ROOM, clusterId, activity.getObject().getId()).getBase64Id();
        } else {
            this._spaceId = new WebexId(WebexId.Type.ROOM, clusterId, activity.getTarget().getId()).getBase64Id();
        }
        PersonModel person = null;
        if (activity.getVerb() == ActivityModel.Verb.acknowledge) {
            person = activity.getActor();
        } else if (activity.getObject() instanceof PersonModel) {
            person = (PersonModel) activity.getObject();
        }
        if (null != person) {
            this._id = new WebexId(WebexId.Type.MEMBERSHIP, clusterId, person.getId() + ":" + WebexId.uuid(this._spaceId)).getBase64Id();
            this._personId = new WebexId(WebexId.Type.PEOPLE, WebexId.DEFAULT_CLUSTER, person.getId()).getBase64Id();
            this._personEmail = person.getEmail();
            this._personDisplayName = person.getDisplayName();
            this._personOrgId = new WebexId(WebexId.Type.ORGANIZATION, WebexId.DEFAULT_CLUSTER, person.getOrgId()).getBase64Id();
            this._isModerator = person.getRoomProperties() != null && person.getRoomProperties().isModerator();
            this._isMonitor = _isModerator;
        }
    }

    /**
     * @return The id of this membership.
     * @since 0.1
     */
    public String getId() {
        return _id;
    }

    /**
     * @return The id of the person.
     * @since 0.1
     */
    public String getPersonId() {
        return _personId;
    }

    /**
     * @return The email address of the person.
     * @since 0.1
     */
    public String getPersonEmail() {
        return _personEmail;
    }

    /**
     * @return The display name of the person.
     * @since 0.1
     */
    public String getPersonDisplayName() {
        return _personDisplayName;
    }

    /**
     * @return The id of the space.
     * @since 0.1
     */
    public String getSpaceId() {
        return _spaceId;
    }

    /**
     * @return True if this member is a moderator of the space in this membership. Otherwise false.
     * @since 0.1
     */
    public boolean isModerator() {
        return _isModerator;
    }

    /**
     * @return True if this member is a monitor of the space in this membership. Otherwise false.
     * @deprecated
     * @since 0.1
     */
    @Deprecated
    public boolean isMonitor() {
        return _isMonitor;
    }

    /**
     * @return The timestamp that the membership being created.
     * @since 0.1
     */
    public Date getCreated() {
        return _created;
    }

    /**
     * @return The personOrgId name of the person
     * @since 1.4
     */
    public String getPersonOrgId() {
        return _personOrgId;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
