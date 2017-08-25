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

package com.ciscospark.phone;

import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipant.State;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;

/**
 * Created on 12/06/2017.
 */

public class CallMembership {
    private String personId = "";
    private String name = "";
    private String email = "";
    private String phoneNumber = "";
    private String sipUrl = "";
    private boolean isInitiator = false;
    private State state;

    CallMembership(String id, String name, String email, String phoneNumber, String sipUrl, State state, boolean isInitiator) {
        this.setName(name);
        this.setPersonId(id);
        this.setEmail(email);
        this.setPhoneNumber(phoneNumber);
        this.setSipUrl(sipUrl);
        this.setState(state);
        this.setInitiator(isInitiator);
    }

    CallMembership(LocusParticipant participant) {
        LocusParticipantInfo person = participant.getPerson();
        this.personId = person.getId();
        this.name = person.getName();
        this.email = person.getEmail();
        this.phoneNumber = person.getPhoneNumber();
        this.sipUrl = person.getSipUrl();
        this.isInitiator = participant.isCreator();
        this.state = participant.getState();
    }

    public String toString() {
        return getPersonId() + ":" + getName() + ":" + getEmail() + ":" + getPhoneNumber() + ":" + getSipUrl() + ":" + getState() + ":" + this.isInitiator();
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSipUrl() {
        return sipUrl;
    }

    public void setSipUrl(String sipUrl) {
        this.sipUrl = sipUrl;
    }

    public boolean isInitiator() {
        return isInitiator;
    }

    public void setInitiator(boolean initiator) {
        isInitiator = initiator;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
