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

import android.util.Log;

import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;
import com.cisco.spark.android.media.MediaRequestSource;
import com.ciscospark.core.SparkApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class Call {
    private static final String TAG = "Call";

    public enum FacingMode {
        USER, ENVIROMENT
    }

    public enum CallStatus {
        INITIATED, RINGING, CONNECTED, DISCONNECTED
    }

    public enum CallType {
        VIDEO, AUDIO
    }

    public enum Direction {
        INCOMING, OUTGOING
    }



    protected Phone mPhone;
    protected CallStatus status;
    protected CallType calltype;
    protected Direction direction;
    private CallObserver mObserver;
    public LocusKey locusKey;


    private boolean  isremoteSendingVideo;
    private boolean  isremoteSendingAudio;

    private boolean  isreceivingAudio;
    private boolean  isreceivingVideo;

    private boolean  issendingAudio;
    private boolean  issendingVideo;
    
    
    private boolean isCameraUserFacingSelected;

    @Inject
    CallControlService callControlServcie;

    private List<CallMembership> callMemberships;

    public void setObserver(CallObserver observer) {
        this.mObserver = observer;
    }

    public CallObserver getObserver() {
        return this.mObserver;
    }

    public Call(Phone phone) {
        this.mPhone = phone;
        this.status = CallStatus.INITIATED;
        SparkApplication.getInstance().inject(this);
        callMemberships = new ArrayList<>();


        this.isreceivingAudio = true;
        this.isreceivingVideo = true;

        this.isremoteSendingAudio = true;
        this.isremoteSendingVideo = true;

        this.issendingAudio = true;
        this.issendingVideo = true;

        isCameraUserFacingSelected = true;
    }

    protected void setisremoteSendingAudio(boolean setting){
        Log.i(TAG, "setisremoteSendingAudio: get  "+ setting);

        this.isremoteSendingAudio = setting;
    }

    protected void setisremoteSendingVideo(boolean setting){
        Log.i(TAG, "setisremoteSendingVideo: get  "+ setting);
        this.isremoteSendingVideo = setting;
    }

    @SuppressWarnings("unused")
    public void answer(CallOption option) {
        Log.i(TAG, "answer call");
        if (direction == Direction.INCOMING) {
            if (status == CallStatus.INITIATED || status == CallStatus.RINGING) {
                Log.i(TAG, "locuskey: " + locusKey.toString());
                CallContext callContext = new CallContext.Builder(locusKey).build();
                Log.i(TAG, "active call is null " + (mPhone.getActiveCall() != null));
                if (mPhone.getActiveCall() != null) {
                    mPhone.getActiveCall().hangup();
                }
                mPhone.setCallOption(option);
                mPhone.setActiveCall(this);
                mPhone.callControlService.joinCall(callContext);
            }
        }
    }

    @SuppressWarnings("unused")
    public void reject() {
        Log.i(TAG, "reject call");
        if (direction == Direction.INCOMING && status == CallStatus.RINGING) {
            Log.i(TAG, "decline call");
            mPhone.callControlService.declineCall(locusKey);
            status = CallStatus.DISCONNECTED;
            /*
            mPhone.callControlService.leaveCall(locusKey);
            mPhone.callControlService.endCall();
            */
        }
    }

    //handup ongoing active call(both in dialing and incall status)
    public void hangup() {
        Log.i(TAG, "hangup call");
        if (this.mPhone.getActiveCall() != this) {
            Log.i(TAG, "this Call is not active call");
            reject();
            return;
        }
        this.mPhone.hangup();
    }

    public void send(String dtmf) {

    }

    public void acknowledge() {

    }

    public List<CallMembership> getMembership() {
        LocusData locusData = callControlServcie.getLocusData(locusKey);
        if (locusData == null) {
            return callMemberships;
        }
        List<LocusParticipant> participants = locusData.getJoinedParticipants();
        callMemberships.clear();
        for (LocusParticipant p : participants) {
            CallMembership member = new CallMembership(p);
            Log.d(TAG, "add member: " + member.toString());
            callMemberships.add(member);
        }
        return callMemberships;
    }

    public int getLocalVideoSize() {
        return 0;
    }

    public int getRemoteVideoSize() {
        return 0;
    }

    public boolean isSendingDTMFEnabled() {

        return false;
    }



    //isRemoteSending Locus
    public boolean isRemoteSendingVideo() {

        //handle CallControlParticipantAudioMuteEvent

        return this.isremoteSendingVideo;
    }


    public boolean isRemoteSendingAudio() {

        return this.isremoteSendingAudio;
    }

    //Sending Locus+WME
    public boolean isSendingVideo() {
        Log.i(TAG, "isSendingVideo: ->start");

        //isSendingVideo's true is isVideoMuted's false
        return !mPhone.callControlService.isVideoMuted(this.locusKey);

    }

    public void setSendingVideo(boolean setting){

        Log.i(TAG, "setSendingVideo: ->start");

        if(this.calltype == CallType.AUDIO ){
            Log.i(TAG, "Audio call can not set sending video");
            return;
        }

        if(setting){
            //true-> sending;
            mPhone.callControlService.unMuteVideo(this.locusKey, MediaRequestSource.USER);

            mPhone.callControlService.setPreviewWindow(this.locusKey, this.mPhone.mLocalSurfaceView);

            if(this.getObserver() != null) {
                Log.i(TAG, "sending ");
                //this.getObserver().onMediaChanged(this, CallObserver.MediaChangedEvent.sendingVideoUnMuted);
            }else
            {
                Log.i(TAG, "Observer is null ");
            }

        }else{

            //false-> not sending
            mPhone.callControlService.muteVideo(this.locusKey, MediaRequestSource.USER);

            mPhone.callControlService.setPreviewWindow(this.locusKey, null);

            if(this.getObserver() != null) {
                Log.i(TAG, "Not sending ");
                //this.getObserver().onMediaChanged(this, CallObserver.MediaChangedEvent.sendingVideoMuted);
            }else
            {
                Log.i(TAG, "Observer is null ");
            }

        }




        //this.callControlService.muteVideo(mCall.getLocusKey(), MediaRequestSource.USER);
        //callControlService.setPreviewWindow(mCall.getLocusKey(), null);

        //this.callControlService.unMuteVideo(mCall.getLocusKey(), MediaRequestSource.USER);
        //callControlService.setPreviewWindow(mCall.getLocusKey(), this.getLocalVideoView());


        //handle CallControlLocalVideoMutedEvent




    }

    public boolean isSendingAudio() {
        Log.i(TAG, "isSendingAudio: ->start");

        //isSendingAudio's true is isAudioMuted's false
        return !mPhone.callControlService.isAudioMuted(this.locusKey);
    }

    public void setSendingAudio(boolean setting) {

        Log.i(TAG, "setSendingAudio: ->start");

        if (setting) {
            //true-> sending;
            mPhone.callControlService.unmuteAudio(this.locusKey);

            if (this.getObserver() != null) {
                Log.i(TAG, "sending ");
                this.getObserver().onMediaChanged(this, CallObserver.MediaChangedEvent.sendingAudioUnMuted);
            } else {
                Log.i(TAG, "Observer is null ");
            }

        } else {

            //false-> not sending
            mPhone.callControlService.muteAudio(this.locusKey);

            if (this.getObserver() != null) {
                Log.i(TAG, "Not sending ");
                this.getObserver().onMediaChanged(this, CallObserver.MediaChangedEvent.sendingVideoMuted);
            } else {
                Log.i(TAG, "Observer is null ");
            }

        }
    }

    //receivingAV wme

    public boolean isReceivingVideo() {
        Log.i(TAG, "isReceivingAudio: " + this.isreceivingVideo);
        return this.isreceivingVideo;
    }

    public void setReceivingVideo(boolean setting){
        Log.i(TAG, "setReceivingVideo: ->start");

        //callControlService.removeRemoteVideoWindows(mCall.getLocusKey());
        //callControlService.setRemoteWindow(mCall.getLocusKey(), this.getRemoteVideoView());

    }

    public boolean isReceivingAudio() {
        Log.i(TAG, "isReceivingAudio: " + this.isreceivingAudio);

        return this.isreceivingAudio;
    }

    public void setReceivingAudio(boolean setting){

        Log.i(TAG, "setReceivingAudio: ->start");

        //self managed
        //this.callControlService.muteRemoteAudio(mCall.getLocusKey(), true);

        if (setting) {
            //true-> receiving;
            //setReceivingAudio's true is muteRemoteAudio's false
            mPhone.callControlService.muteRemoteAudio(this.locusKey,!setting);

            if (this.getObserver() != null) {
                Log.i(TAG, "receiving ");
                this.isreceivingAudio = true;
                this.getObserver().onMediaChanged(this, CallObserver.MediaChangedEvent.receivingAudioUnMuted);
            } else {
                Log.i(TAG, "Observer is null ");
            }

        } else {

            //false-> not receiving
            //setReceivingAudio's true is muteRemoteAudio's false
            mPhone.callControlService.muteRemoteAudio(this.locusKey,!setting);

            if (this.getObserver() != null) {
                Log.i(TAG, "Not receiving ");
                this.isreceivingAudio = false;
                this.getObserver().onMediaChanged(this, CallObserver.MediaChangedEvent.receivingAudioMuted);
            } else {
                Log.i(TAG, "Observer is null ");
            }

        }

    }

    public Boolean isFacingModeSelected() {
        Log.i(TAG, "isFacingModeSelected: " + this.isCameraUserFacingSelected);

        return this.isCameraUserFacingSelected;
    }

    public void setFacingMode(FacingMode mode){
        Log.i(TAG, "setFacingMode: ->start");
        //this.callControlService.switchCamera(mCall.getLocusKey());

        FacingMode currentMode;

        if(this.isCameraUserFacingSelected){
            currentMode = FacingMode.USER;
        }else{
            currentMode = FacingMode.ENVIROMENT;
        }

        if(currentMode == mode){
            Log.i(TAG, "setFacingMode: ->no change");
            return;

        }else{
            Log.i(TAG, "setFacingMode: -> change");

            mPhone.callControlService.switchCamera(this.locusKey);
            if(mode == FacingMode.USER){

                //set flag to user
                this.isCameraUserFacingSelected = true;

            }else{
                //set flag to environment
                this.isCameraUserFacingSelected = false;
            }

        }

    }

    public boolean isLoudSpeakerSelected(){
        //self managed
        return false;
    }

    public void setLoudSpeaker(boolean setting){
        //need to implemente
    }

}
