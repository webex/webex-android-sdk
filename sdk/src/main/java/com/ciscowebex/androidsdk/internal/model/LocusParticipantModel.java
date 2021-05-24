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

package com.ciscowebex.androidsdk.internal.model;

import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.internal.Device;

import java.util.ArrayList;
import java.util.List;

public class LocusParticipantModel {

    public enum Type {
        USER, ANONYMOUS, RESOURCE_ROOM, MEETING_BRIDGE, MEDIA_NODE
    }

    public enum IntentType {
        HELD, HOLDING, JOIN, LEAVE, MOVE_MEDIA, NONE, OBSERVE, WAIT
    }

    public enum State {
        /* the participant is not in the locus */
        IDLE,
        /* one of more devices of the participant are alerting */
        NOTIFIED,
        /* the participant is in the locus */
        JOINED,
        /* the participant is not in the locus */
        LEFT,
        /* the participant declined to join */
        DECLINED,
        /* local client state indicating we're in process of leaving locus */
        LEAVING
    }

    public enum Reason {
        REMOTE,
        RESOURCE_BUSY,
        ANSWERED_ELSEWHERE,
        INVALID_PIN,
        MOVED,
        RESOURCE_TIMED_OUT,
        NOT_FOUND,
        FORCED,
        INVALID_JOIN_TIME,
        INTENT_CANCELED,
        MEDIA_RESOURCE_RECONNECT,
        INTENT_EXPIRED,
        JOURNAL_TOO_LARGE,
        UNKNOWN,
        INACTIVE,
        NOT_ACCEPTABLE,
        REPLACED,
        MEETING_FULL,
        UNREACHABLE,
        UNANSWERED,
        RESOURCE_ERROR,
        BUSY,
        DND,
        LOBBY_EXPIRED,
        GARBAGE_COLLECTED,
        LOGIN_REQUIRED,
        MEETING_LOCKED,
        CALL_MAX_DURATION,
        MEETING_ENDED,
        RESOURCE_DECLINED
    }

    public enum ProvisionalDeviceType {
        DIAL_IN("dialin"),
        DIAL_OUT("dialout");

        public final String value;

        ProvisionalDeviceType(String value) {
            this.value = value;
        }
    }

    public enum RequestedMedia {
        NONE,
        AUDIO,
        VIDEO,
        SLIDES
    }

    protected String id;
    protected String url;
    protected boolean isCreator;
    protected LocusParticipantInfoModel person;
    protected LocusParticipantModel.State state = LocusParticipantModel.State.IDLE;
    protected String deviceUrl;
    protected LocusParticipantModel.Type type;
    protected LocusParticipantModel.Reason reason;
    protected boolean guest;
    protected String invitedBy;
    protected List<LocusParticipantDeviceModel> devices = new ArrayList<>();
    protected LocusParticipantStatusModel status;
    protected List<IntentModel> intents;
    protected LocusParticipantControlsModel controls;
    protected List<SuggestedMediaModel> suggestedMedia;
    protected boolean moderator;
    protected boolean resourceGuest;
    protected boolean removed;
    protected boolean moderatorAssignmentNotAllowed;
    protected boolean hideInRoster;

    public LocusParticipantModel() {
    }

    public LocusParticipantModel(String id, LocusParticipantModel.State state) {
        this.id = id;
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public String getDeviceUrl() {
        return this.deviceUrl;
    }

    public LocusParticipantModel.State getState() {
        return this.state;
    }

    public void setState(LocusParticipantModel.State state) {
        this.state = state;
    }

    public LocusParticipantModel.Reason getReason() {
        return reason;
    }

    public void setReason(LocusParticipantModel.Reason reason) {
        this.reason = reason;
    }

    public String getId() {
        return this.id;
    }

    public boolean isCreator() {
        return this.isCreator;
    }

    public LocusParticipantInfoModel getPerson() {
        return this.person;
    }

    public @Nullable
    LocusParticipantModel.Type getType() {
        return type;
    }

    public List<LocusParticipantDeviceModel> getDevices() {
        return devices;
    }

    public LocusParticipantStatusModel getStatus() {
        return status;
    }

    public void setStatus(LocusParticipantStatusModel newStatus) {
        status = newStatus;
    }

    public boolean isGuest() {
        return guest;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public boolean isModeratorAssignmentNotAllowed() {
        return moderatorAssignmentNotAllowed;
    }

    public boolean isRoom() {
        return LocusParticipantModel.Type.RESOURCE_ROOM == type;
    }

    public boolean isJoined() {
        return LocusParticipantModel.State.JOINED == state;
    }

    public boolean isDeclined() {
        return State.DECLINED == state;
    }

    public boolean isNotified() {
        return getState() == LocusParticipantModel.State.NOTIFIED;
    }

    public LocusParticipantControlsModel getControls() {
        return controls;
    }

    public void setControls(LocusParticipantControlsModel controls) {
        this.controls = controls;
    }

    public boolean isModerator() {
        return moderator;
    }

    public void setModerator(boolean moderator) {
        this.moderator = moderator;
    }

    public boolean isResourceGuest() {
        return resourceGuest;
    }

    public boolean isRemoved() {
        return removed;
    }

    public boolean isWebExUser() {
        return isDeviceType(Device.Type.WEBEX);
    }

    public boolean isSipDevice() {
        return isDeviceType(Device.Type.SIP);
    }

    public boolean isDeviceType(Device.Type type) {
        List<LocusParticipantDeviceModel> deviceList = this.getDevices();
        if (deviceList != null) {
            for (LocusParticipantDeviceModel d : deviceList) {
                if (type.getTypeName().equalsIgnoreCase(d.getDeviceType())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public String getRoomIdentifier() {
        LocusParticipantInfoModel person = getPerson();
        if (person != null) {
            return person.getId();
        }
        return null;
    }

    public boolean isValidCiUser() {
        return getType() == LocusParticipantModel.Type.USER || getType() == LocusParticipantModel.Type.RESOURCE_ROOM;
    }

    public boolean isUserOrResourceRoom() {
        return getType() == LocusParticipantModel.Type.USER || getType() == LocusParticipantModel.Type.RESOURCE_ROOM;
    }

    public boolean isVideoMuted() {
        return LocusMediaDirection.RECVONLY.equals(getStatus().getVideoStatus());
    }

    public boolean isAudioMuted() {
        return isAudioLocallyMuted() || isAudioRemotelyMuted();
    }

    public boolean isAudioLocallyMuted() {
        return LocusMediaDirection.RECVONLY.equals(getStatus().getAudioStatus());
    }

    public boolean isAudioRemotelyMuted() {
        return getControls() != null && getControls().getAudio() != null && getControls().getAudio().isMuted();
    }

    public List<SuggestedMediaModel> getSuggestedMedia() {
        return suggestedMedia;
    }

    public List<IntentModel> getIntents() {
        return intents;
    }

    public boolean isWaiting() {
        return hasIntent(LocusParticipantModel.IntentType.WAIT);
    }

    public boolean isWaitingOnDevice(String deviceUrl) {
        return hasIntentOnDevice(deviceUrl, LocusParticipantModel.IntentType.WAIT);
    }

    public boolean isObserving() {
        return hasIntent(LocusParticipantModel.IntentType.OBSERVE);
    }

    public boolean isObservingOnDevice(String deviceUrl) {
        return hasIntentOnDevice(deviceUrl, LocusParticipantModel.IntentType.OBSERVE);
    }

    private boolean hasIntent(LocusParticipantModel.IntentType intentType) {
        for (LocusParticipantDeviceModel device : devices) {
            if (device.getIntent() != null && device.getIntent().getType() != null &&
                    device.getIntent().getType().equals(intentType)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIntentOnDevice(String deviceUrl, LocusParticipantModel.IntentType intentType) {
        boolean hasIntent = false;
        if (devices != null && deviceUrl != null && intentType != null) {
            for (LocusParticipantDeviceModel device : devices) {
                if (deviceUrl.equals(device.getUrl()) && device.getIntent() != null) {
                    if (intentType.equals(device.getIntent().getType())) {
                        hasIntent = true;
                    }
                }
            }
        }

        return hasIntent;
    }

    public boolean isInLobby() {
        return isIdle() && isWaiting();
    }

    public boolean isInLobbyOnDevice(String deviceUrl) {
        return isIdle() && isWaitingOnDevice(deviceUrl);
    }

    public boolean isMySelf(String myId) {
        return getId().equals(myId);
    }

    public boolean isHideInRoster() {
        return hideInRoster;
    }

    public boolean isIdle() {
        return LocusParticipantModel.State.IDLE == state;
    }

    public boolean isLefted(String deviceUrl) {
        LocusParticipantDeviceModel device = getDevice(deviceUrl);
        return state == State.LEFT || device == null || device.getState() == State.LEFT;
    }

    public boolean isJoined(String deviceUrl) {
        if (state != State.JOINED) {
            return false;
        }
        LocusParticipantDeviceModel device = getDevice(deviceUrl);
        return device != null && device.getState() == State.JOINED;
    }

    public boolean isDeclined(String deviceUrl) {
        return isDeclined() && deviceUrl.equals(getDeviceUrl());
    }

    private LocusParticipantDeviceModel getDevice(String deviceUrl) {
        if (devices != null && deviceUrl != null) {
            for (LocusParticipantDeviceModel device : devices) {
                if (deviceUrl.equals(device.getUrl())) {
                    return device;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof LocusParticipantModel) {
            LocusParticipantModel that = (LocusParticipantModel) object;
            return this.id.equals(that.id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
