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

package com.ciscowebex.androidsdk.space;

import java.util.Date;

import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.internal.model.ConversationModel;
import com.ciscowebex.androidsdk.internal.model.ObjectModel;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * A data type represents a Space at Cisco Webex cloud.
 * <p>
 * Room has been renamed to Space in Cisco Webex.
 *
 * @since 0.1
 */
public class Space {

    /**
     * The enumeration of the types of a space.
     *
     * @since 0.1
     */
    public enum SpaceType {
        /**
         * Group space among multiple people
         *
         * @since 0.1
         */
        @SerializedName("group")
        GROUP,

        /**
         * 1-to-1 space between two people
         *
         * @since 0.1
         */
        @SerializedName("direct")
        DIRECT;

        /**
         * Return serialized name
         *
         * @since 2.1.1
         */
        public String serializedName() {
            return super.name().toLowerCase();
        }
    }

    @SerializedName("id")
    private String _id;

    @SerializedName("title")
    private String _title;

    @SerializedName("type")
    private SpaceType _type;

    @SerializedName("teamId")
    private String _teamId;

    @SerializedName("isLocked")
    private boolean _isLocked;

    @SerializedName("lastActivity")
    private Date _lastActivity;

    @SerializedName("created")
    private Date _created;

    @SerializedName("sipAddress")
    private String _sipAddress;

    protected Space(ActivityModel activity) {
        _lastActivity = activity.getPublished();
        ObjectModel object = activity.getVerb().equals(ActivityModel.Verb.create) ? activity.getObject() : activity.getTarget();
        if (object instanceof ConversationModel) {
            _id = new WebexId(WebexId.Type.ROOM_ID, object.getId()).toHydraId();
            _isLocked = ((ConversationModel) object).isLocked();
            _type = ((ConversationModel) object).isOneOnOne() ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
        }
    }

    /**
     * @return The identifier of this space.
     * @since 0.1
     */
    public String getId() {
        return _id;
    }

    /**
     * @return The title of this space.
     * @since 0.1
     */
    public String getTitle() {
        return _title;
    }

    /**
     * @return The type of this space.
     * @since 0.1
     */
    public SpaceType getType() {
        return _type;
    }

    /**
     * @return The team Id that this space associated with.
     * @since 0.1
     */
    public String getTeamId() {
        return _teamId;
    }

    /**
     * @return Indicate if this space is locked.
     * @since 0.1
     */
    public boolean isLocked() {
        return _isLocked;
    }

    /**
     * @return Last activity of this space.
     * @since 0.1
     */
    public Date getLastActivity() {
        return _lastActivity;
    }

    /**
     * @return The timestamp that this space being created.
     * @since 0.1
     */
    public Date getCreated() {
        return _created;
    }

    /**
     * @return The sipAddress that this space associated with.
     * @since 1.4
     */
    public String getSipAddress() {
        return _sipAddress;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}


