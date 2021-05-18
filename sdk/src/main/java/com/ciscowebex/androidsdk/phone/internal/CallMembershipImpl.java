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

package com.ciscowebex.androidsdk.phone.internal;

import com.ciscowebex.androidsdk.internal.model.FloorModel;
import com.ciscowebex.androidsdk.internal.model.LocusMediaDirection;
import com.ciscowebex.androidsdk.internal.model.LocusModel;
import com.ciscowebex.androidsdk.internal.model.LocusParticipantDeviceModel;
import com.ciscowebex.androidsdk.internal.model.LocusParticipantModel;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.helloworld.utils.Checker;

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
        Ln.d("CallMembership: " + getId() + " person: " + getPersonId() + " displayName: " + getDisplayName() + "  video: " + isSendingVideo() + "   audio: " + isSendingAudio());
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

    @Override
    public String getDisplayName() {
        return model.getPerson() == null ? null : model.getPerson().getDisplayName();
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

    @Override
    public boolean isAudioMutedControlled() {
        return model.isAudioRemotelyMuted();
    }

    @Override
    @Nullable
    public String audioModifiedBy() {
        String personId = null;
        if (model.getControls() != null && model.getControls().getAudio() != null && model.getControls().getAudio().getMeta() != null) {
            personId = model.getControls().getAudio().getMeta().getModifiedBy();
        }
        if (personId != null) {
            personId = new WebexId(WebexId.Type.PEOPLE, WebexId.DEFAULT_CLUSTER, personId).getBase64Id();
        }
        return personId;
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
            this.personId = new WebexId(WebexId.Type.PEOPLE, WebexId.DEFAULT_CLUSTER, model.getPerson().getId()).getBase64Id();
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

    List<String> getAssociatedUrls() {
        List<String> associatedUrls = new ArrayList<>();
        for (LocusParticipantDeviceModel device : model.getDevices()) {
            if (device.getIntent() != null && device.getIntent().getAssociatedWith() != null) {
                associatedUrls.add(device.getIntent().getAssociatedWith());
            }
        }
        return associatedUrls;
    }

    public String toString() {
        return "CallMembership: " + getId()
                + " status: " + getState()
                + " isSelf: " + isSelf()
                + " person: " + getPersonId()
                + " displayName: " + getDisplayName()
                + " video: " + isSendingVideo()
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
