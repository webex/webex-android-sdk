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

import com.ciscowebex.androidsdk.internal.model.ConversationModel;
import com.ciscowebex.androidsdk.internal.model.PersonModel;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.ciscowebex.androidsdk.message.Message;

import java.util.Date;

/**
 * The read status of the {@link Membership} for space.
 *
 * @since 2.3.0
 */
public class MembershipReadStatus {

    private Membership _membership;

    private String _lastSeenId;

    private Date _lastSeenDate;

    protected MembershipReadStatus(ConversationModel conversation, PersonModel person, String clusterId) throws IllegalArgumentException {
        PersonModel.RoomPropertiesModel roomProperties = person.getRoomProperties();
        if (roomProperties == null) {
            throw new IllegalArgumentException("RoomProperties is null");
        }
        _lastSeenId = new WebexId(WebexId.Type.MESSAGE, clusterId, roomProperties.getLastSeenActivityUUID()).getBase64Id();
        _lastSeenDate = roomProperties.getLastSeenActivityDate();
        _membership = new Membership(conversation, person, clusterId);
    }

    /**
     * The {@link Membership} of the space.
     */
    public Membership getMembership() {
        return _membership;
    }

    /**
     * The id of the last {@link Message} the member have read.
     */
    public String getLastSeenId() {
        return _lastSeenId;
    }

    /**
     * The last date and time the member have read messages.
     */
    public Date getLastSeenDate() {
        return _lastSeenDate;
    }
}