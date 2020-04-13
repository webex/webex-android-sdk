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

import com.ciscowebex.androidsdk.internal.model.ConversationModel;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Read status about the date of last activity in the space and the date of current user last presence in the space.
 *
 * For spaces where lastActivityDate > lastSeenDate the space can be considered to be "unread".
 *
 * @since 2.3.0
 */
public class SpaceReadStatus {

    @SerializedName("id")
    private String _id;

    @SerializedName("tags")
    private Space.SpaceType _type;

    @SerializedName(value = "lastReadableActivityDate", alternate = "lastRelevantActivityDate")
    private Date _lastActivityDate;

    @SerializedName("lastSeenActivityDate")
    private Date _lastSeenActivityDate;

    protected SpaceReadStatus(ConversationModel conversation) {
        _id =  new WebexId(WebexId.Type.ROOM_ID, conversation.getId()).toHydraId();
        _type = conversation.isOneOnOne() ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
        _lastSeenActivityDate = conversation.getLastSeenActivityDate();
        _lastActivityDate = conversation.getLastReadableActivityDate();
        if (_lastActivityDate == null) {
            _lastActivityDate = conversation.getLastRelevantActivityTimestamp();
        }
    }

    /**
     * The identifier of this space.
     *
     * @return the identifier of this space.
     */
    public String getId() {
        return _id;
    }

    /**
     * The type of this space.
     *
     * @return the type of this space.
     */
    public Space.SpaceType getType() {
        return _type;
    }

    /**
     * The date of last activity in the space
     *
     * @return the date of last activity in the space
     */
    public Date getLastActivityDate() {
        return _lastActivityDate;
    }

    /**
     * The date of the last message in the space that login user has read.
     *
     * @return the date of the last message in the space that login user has read.
     */
    public Date getLastSeenDate() {
        return _lastSeenActivityDate;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
