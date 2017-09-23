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

package com.ciscospark.androidsdk.room;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Room {

    public enum RoomType {
        @SerializedName("group")
        group,

        @SerializedName("direct")
        direct
    }

    @SerializedName("id")
    private String _id;

    @SerializedName("title")
    private String _title;

    @SerializedName("type")
    private RoomType _type;

    @SerializedName("teamId")
    private String _teamId;

    @SerializedName("isLocked")
    private boolean _isLocked;

    @SerializedName("lastActivity")
    private Date _lastActivity;

    @SerializedName("created")
    private Date _created;

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getId() {
        return _id;
    }

    public String getTitle() {
        return _title;
    }

    public RoomType getType() {
        return _type;
    }

    public String getTeamId() {
        return _teamId;
    }

    public boolean isLocked() {
        return _isLocked;
    }

    public Date getLastActivity() {
        return _lastActivity;
    }

    public Date getCreated() {
        return _created;
    }
}


