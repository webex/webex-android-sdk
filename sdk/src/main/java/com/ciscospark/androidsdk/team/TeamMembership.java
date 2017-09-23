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

package com.ciscospark.androidsdk.team;


import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class TeamMembership {

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

    @SerializedName("teamId")
    private String _teamId;

    @SerializedName("isModerator")
    private boolean _isModerator;

    @SerializedName("created")
    private Date _created;

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getId() {
        return _id;
    }

    public String getPersonId() {
        return _personId;
    }

    public String getPersonEmail() {
        return _personEmail;
    }

    public String getPersonDisplayName() {
        return _personDisplayName;
    }

    public String getPersonOrgId() {
        return _personOrgId;
    }

    public String getTeamId() {
        return _teamId;
    }

    public boolean isModerator() {
        return _isModerator;
    }

    public Date getCreated() {
        return _created;
    }
}
