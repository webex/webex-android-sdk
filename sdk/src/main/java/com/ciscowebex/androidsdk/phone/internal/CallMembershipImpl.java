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

package com.ciscowebex.androidsdk.phone.internal;

import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;

import me.helloworld.utils.Checker;
import me.helloworld.utils.Objects;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CallMembershipImpl implements CallMembership {

    private static CallMembership.State fromLocusState(LocusParticipantModel.State state, boolean isInLobby) {
        if (state == LocusParticipantModel.State.IDLE) {
            return isInLobby ? State.WAITING : State.IDLE;
        } else if (state == LocusParticipantModel.State.NOTIFIED) {
            return State.NOTIFIED;
        } else if (state == LocusParticipantModel.State.JOINED) {
            return State.JOINED;
        } else if (state == LocusParticipantModel.State.LEFT) {
            return State.LEFT;
        } else if (state == LocusParticipantModel.State.DECLINED) {
            return State.DECLINED;
        } else if (state == LocusParticipantModel.State.LEAVING) {
            return State.LEFT;
        } else {
            return State.UNKNOWN;
        }
    }

    private final CallImpl call;

    private LocusParticipantModel model;

    private boolean self;

    private boolean initiator = false;

    private String personId;

    CallMembershipImpl(LocusParticipantModel participant, CallImpl call) {
        this.call = call;
        setModel(participant);
        Ln.d("CallMembership: " + getId() + " person: " + getPersonId() + " email: " + getEmail() + "  video: " +  isSendingVideo() + "   audio: " + isSendingAudio());
    }

    public String getId() {
        return model.getId();
    }

    public boolean isSelf() {
        return self;
    }

    public boolean isInitiator() {
        return initiator;
    }

    public String getPersonId() {
        return personId;
    }

    public State getState() {
        return fromLocusState(model.getState(), model.isInLobby());
    }

    public String getEmail() {
        return model.getPerson() == null ? null : model.getPerson().getEmail();
    }

    public String getSipUrl() {
        return model.getPerson() == null ? null : model.getPerson().getSipUrl();
    }

    public String getPhoneNumber() {
        return model.getPerson() == null ? null : model.getPerson().getPhoneNumber();
    }

    public boolean isSendingVideo() {
        return model.getStatus() != null && model.getStatus().getVideoStatus() == LocusMediaDirection.SENDRECV;
    }

    public boolean isSendingAudio() {
        return model.getStatus() != null && model.getStatus().getAudioStatus() == LocusMediaDirection.SENDRECV;
    }

    public boolean isSendingSharing() {
        LocusModel model = call.getModel();
        FloorModel floor = model.getGrantedFloor();
        return floor != null
                && floor.getBeneficiary() != null && Checker.isEqual(floor.getBeneficiary().getId(), getId());
    }

    @Override
    public boolean isActiveSpeaker() {
        return Checker.isEqual(getId(), ((CallMembershipImpl) call.getActiveSpeaker()).getId());
    }

    public LocusParticipantModel getModel() {
        synchronized (this) {
            return model;
        }
    }

    public void setModel(@NotNull LocusParticipantModel model) {
        synchronized (this) {
            this.model = model;
            this.self = Checker.isEqual(model.getId(), call.getModel().getSelfId());
            this.personId = new WebexId(WebexId.Type.PEOPLE_ID, model.getPerson().getId()).toHydraId();
            this.initiator = model.isCreator();
        }
    }

    public boolean containsCSI(long csi) {
        List<Long> CSIs = model.getStatus() == null ? null : model.getStatus().getCsis();
        if (!Checker.isEmpty(CSIs)) {
            return CSIs.contains(csi);
        }
        return false;
    }

    public String toString() {
        return "CallMembership: " + getId()
                + " status: " + getState()
                + " isSelf: " + isSelf()
                + " person: " + getPersonId()
                + " email: " + getEmail()
                + " video: " +  isSendingVideo()
                + " audio: " + isSendingAudio();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallMembershipImpl that = (CallMembershipImpl) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId());
    }
}
