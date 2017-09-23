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

package com.ciscospark.androidsdk.phone.internal;

import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;
import com.cisco.spark.android.locus.model.MediaDirection;
import com.ciscospark.androidsdk.phone.CallMembership;
import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

/**
 * Created on 12/06/2017.
 */

public class CallMembershipImpl implements CallMembership {

    private static CallMembership.State fromLocusState(LocusParticipant.State state) {
        if (state == LocusParticipant.State.IDLE) {
            return CallMembership.State.IDLE;
        }
        else if (state == LocusParticipant.State.NOTIFIED) {
            return State.NOTIFIED;
        }
        else if (state == LocusParticipant.State.JOINED) {
            return State.JOINED;
        }
        else if (state == LocusParticipant.State.LEFT) {
            return State.LEFT;
        }
        else if (state == LocusParticipant.State.DECLINED) {
            return State.DECLINED;
        }
        else if (state == LocusParticipant.State.LEAVING) {
            return State.LEFT;
        }
        else {
            return State.UNKNOWN;
        }
    }

    @StringPart
    private boolean _isInitiator = false;

    @StringPart
    private String _personId;

    @StringPart
    private State _state = State.UNKNOWN;

    @StringPart
    private String _email;

    @StringPart
    private String _sipUrl;

    @StringPart
    private String _phoneNumber;

    @StringPart
    private boolean _sendingVideo = false;

    @StringPart
    private boolean _sendingAudio = false;

    CallMembershipImpl(LocusParticipant participant) {
        LocusParticipantInfo person = participant.getPerson();
        _personId = person.getId();
        _email = person.getEmail();
        _phoneNumber = person.getPhoneNumber();
        _sipUrl = person.getPhoneNumber();
        _isInitiator = participant.isCreator();
        _state = fromLocusState(participant.getState());
        _sendingVideo = MediaDirection.SENDRECV.equals(participant.getStatus().getVideoStatus());
        _sendingAudio = MediaDirection.SENDRECV.equals(participant.getStatus().getAudioStatus());
    }

    public boolean isInitiator() {
        return _isInitiator;
    }

    public String getPersonId() {
        return _personId;
    }

    public State getState() {
        return _state;
    }

    public String getEmail() {
        return _email;
    }

    public String getSipUrl() {
        return _sipUrl;
    }

    public String getPhoneNumber() {
        return _phoneNumber;
    }

    public boolean isSendingVideo() {
        return _sendingVideo;
    }

    public boolean isSendingAudio() {
        return _sendingAudio;
    }

    public String toString() {
        return Objects.toStringByAnnotation(this);
    }

}
