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

import android.Manifest;
import android.os.Handler;
import android.util.Log;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLeaveLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusCreatedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantJoinedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.locus.events.ParticipantJoinedEvent;
import com.cisco.spark.android.locus.events.ParticipantNotifiedEvent;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.sync.ActorRecord;
import com.ciscospark.Spark;
import com.ciscospark.core.SparkApplication;
import com.webex.wseclient.WseSurfaceView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class Phone {

    private Spark mspark;

    private List<Call> calllist;

    private static final String TAG = "Phone";

    private ApplicationDelegate applicationDelegate;

    @Inject
    ApplicationController applicationController;

    @Inject
    ApiTokenProvider apiTokenProvider;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    CallControlService callControlService;

    @Inject
    MediaEngine mediaEngine;

    @Inject
    EventBus bus;



    WseSurfaceView mRemoteSurfaceView;

    WseSurfaceView mLocalSurfaceView;

    private CallContext callContext;

    //keep temp listener
    private RegisterListener mListener;

    //registered on WMD or not
    private boolean isRegisterInWDM;

    //as common lib doesn't provide timeout event, we use this to handle timeout
    private Timer mTimer;

    private Handler mTimeHandler;
    private Runnable mTimeRunnable;


    //keep activitied call reference

    private Call mActiveCall;


    private IncomingCallObserver incomingCallObserver;



    public Phone(Spark spark){
        Log.i(TAG, "Phone: ->start");
        SparkApplication.getInstance().inject(this);
        bus.register(this);

        this.mspark = spark;

        isRegisterInWDM = false;

        this.mTimeHandler = new Handler();

        //prevent common lib automatically register by using old data
        logout();

        calllist = new LinkedList<Call>();

        Log.i(TAG, "Phone: ->end");

    }


    public Call getActiveCall(){
        return this.mActiveCall;
    }

    public void setActiveCall(Call call){
        this.mActiveCall = call;
    }

    public void setIncomingCallObserver(IncomingCallObserver observer) {
        this.incomingCallObserver = observer;
    }



    //release eventbus.
    //clean data from common lib,to prevent automatically register
    public void close() {
        Log.i(TAG, "close: ->");

        if (bus.isRegistered(this)) {

            bus.unregister(this);

        }

        //prevent common lib automatically register by using old data
        logout();
    }

    private boolean isAuthorized()
    {
        Log.i(TAG, "isAuthorized: ->");

        if(this.mspark.getStrategy().getToken() == null) {

            Log.i(TAG, "register: -> no valid Token");

            return false;

        }

        return true;

    }



    public void register(RegisterListener listener) {

        Log.i(TAG, "register: ->start");

        if(!isAuthorized())
        {

            //not authorized

            listener.onFailed();


            return;
        }


        OAuth2Tokens tokens = new OAuth2Tokens();

        tokens.update(this.mspark.getStrategy().getToken());


        String email1 = "";
        String name1 = "";

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(email1, new ActorRecord.ActorKey(email1), name1, tokens, "Unknown", null, 0, null);

        Log.i(TAG, "->after new AuthenticatedUser");

        apiTokenProvider.setAuthenticatedUser(authenticatedUser);

        Log.i(TAG, "->after setAuthenticatedUser");

        this.mListener = listener;

        new AuthenticatedUserTask(applicationController).execute();



        //start to monitor register timeout
        startRegisterTimer(Constant.timeout);

        Log.i(TAG, "register: ->end ");
    }


    private void startRegisterTimer(int timeoutinSeconds){

        this.mTimeRunnable = new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "run: -> register timeout");

                if(!Phone.this.isRegisterInWDM){
                    if(Phone.this.mListener != null)
                    {
                        Phone.this.mListener.onFailed();
                    }
                }
            }
        };

        this.mTimeHandler.postDelayed(this.mTimeRunnable, timeoutinSeconds*1000);


    }


    public void deregister(DeregisterListener listener) {
        Log.i(TAG, "deregister: ->start");

        this.logout();

        listener.onSuccess();

    }

    //hang up current active call
    protected void hangup(){
        Log.i(TAG, "hangup: ->Start");
        if(this.mActiveCall == null)
        {
            Log.i(TAG, " no active call ");
            return;
        }

        if((this.mActiveCall.status == Call.CallStatus.INITIATED)||
                (this.mActiveCall.status == Call.CallStatus.RINGING)){

            //call is not setup
            Log.i(TAG, "cancelCall");

            this.callControlService.cancelCall(true);
            return;
        }
        if(this.mActiveCall.status == Call.CallStatus.CONNECTED){

            //call is setup
            Log.i(TAG, "leaveCall");

            this.callControlService.leaveCall(this.mActiveCall.locusKey);
            return;

        }


    }

    private void logout(){
        Log.i(TAG, "logout: ->1");
        applicationController.logout(null, false);
        this.isRegisterInWDM = false;

    }



    public void dial(String dialString, CallOption option, DialObserver observer) {
        Log.i(TAG, "dial: ->start");

        if(!this.isRegisterInWDM){
            Log.i(TAG, "register wdm failed");
            observer.onFailed(DialObserver.ErrorCode.ILLEGAL_STATUS);
            return;
        }


        if(this.mActiveCall != null ){

            Log.i(TAG, "isInActivitiedCall");
            observer.onFailed(DialObserver.ErrorCode.ILLEGAL_STATUS);
            return;
        }


        if (!setCallOption(option)) {
            Log.i(TAG, "setCallOption failed");
            observer.onFailed(DialObserver.ErrorCode.ILLEGAL_PARAMETER);
            return;
        }


        if(option.mCalltype == CallOption.CallType.VIDEO) {

            Log.i(TAG, "videoCall");

            Call call = new Call(this);


            call.status = Call.CallStatus.INITIATED;
            call.direction = Call.Direction.OUTGOING;
            call.calltype = Call.CallType.VIDEO;

            //add this call to list
            this.calllist.add(call);

            //set this call as active call
            this.mActiveCall = call;

            //joinCall will trigger permission synchronouslly
            observer.onSuccess(call);

            CallContext callContext = new CallContext.Builder(dialString).build();
            callControlService.joinCall(callContext);
            Log.i(TAG, "dial: ->sendout");
        }

        if(option.mCalltype == CallOption.CallType.AUDIO) {

            Log.i(TAG, "AudioCall");

            Call call = new Call(this);


            call.status = Call.CallStatus.INITIATED;
            call.direction = Call.Direction.OUTGOING;
            call.calltype = Call.CallType.AUDIO;

            //add this call to list
            this.calllist.add(call);

            //set this call as active call
            this.mActiveCall = call;

            //joinCall will trigger permission synchronouslly
            observer.onSuccess(call);

            callContext = new CallContext.Builder(dialString).setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioOnly).build();
            callControlService.joinCall(callContext);
            Log.i(TAG, "dial: ->sendout");
        }

    }

    protected boolean setCallOption(CallOption option) {
        if(option.mCalltype == CallOption.CallType.VIDEO) {

            if (option.mLocalView == null || option.mRemoteView == null) {
                //video call but not set remoteView or localView
                Log.i(TAG, "videoCall but Views are missed");
                return false;
            } else {
                Log.i(TAG, "videoCall");
                this.mLocalSurfaceView = option.mLocalView;
                this.mRemoteSurfaceView = option.mRemoteView;
                return true;
            }
        }

        if(option.mCalltype == CallOption.CallType.AUDIO) {

            Log.i(TAG, "AudioCall");

            return true;
        }

        // Other CallType
        return false;
    }

    //*.remove call from array
    //
    //*. set the removed call to disconnected
    //*. set phone.Activecall as null
    //*. inform UI call end reason
    protected void removeCallAndMarkIt(Call call, CallObserver.DisconnectedReason
            reason)
    {
        Log.i(TAG, "removeCallfromArray: ->start");


        for (int j = 0; j < this.calllist.size(); j++) {
            Call call2 = this.calllist.get(j);

            //same call object
            if(call == call2) {

                Log.i(TAG, "find the removed callobject from list");

                //set it to disconnected
                call2.status = Call.CallStatus.DISCONNECTED;

                //notify UI why this call is dead,
                if(this.mActiveCall.getObserver() != null) {
                    this.mActiveCall.getObserver().onDisconnected(reason);
                }

                this.calllist.remove(j);
                this.mActiveCall = null;

                break;
            }

        }



    }

    public void startPreView() {

    }

    public void stopPreView() {

    }

    public boolean isConnected() {
        return false;
    }

    //registed a WDM successfully
    public void onEventMainThread(DeviceRegistrationChangedEvent event) {

        Log.i(TAG, "DeviceRegistrationChangedEvent -> is received ");

        if(this.mListener == null)
        {
            //in case, even logout is called, common lib still send out event by using old date
            // to register
            Log.i(TAG, "this.mListener is null ");
            return;
        }

        if(!isAuthorized())
        {
            Log.i(TAG, "not authorized,something wrong! ");
            //not authorized,something wrong
            return;
        }

        this.mListener.onSuccess();

        //successfully registered
        this.isRegisterInWDM = true;

        //cancel register timer
        this.mTimeHandler.removeCallbacks(this.mTimeRunnable);

        Log.i(TAG, "onEventMainThread: Registered:" + event.getDeviceRegistration().getId());

    }

    //request camera and microphone permission
    public void onEventMainThread(RequestCallingPermissions event) {
        Log.i(TAG, "RequestCallingPermissions -> is received ");

        List<String> permissions = new ArrayList<String>();

        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CAMERA);

        if(this.mActiveCall == null)
        {
            Log.i(TAG, "Something is Wrong, how mActiveCall is null");
            return;
        }

        this.mActiveCall.getObserver().onPermissionRequired(permissions);

        //during the first dial, the permission event will happen.
        //it means the first dial end for permission reason

        //need to remove this ActiveCall and set it to DISCONNECTED


        for (int j = 0; j < this.calllist.size(); j++) {
            Call call = this.calllist.get(j);

            //same call object
            if(this.mActiveCall == call) {

                Log.i(TAG, "find the mActiveCall from list");

                this.mActiveCall.status = Call.CallStatus.DISCONNECTED;


                //notify UI why this call is dead,
                this.mActiveCall.getObserver().onDisconnected(CallObserver.DisconnectedReason.endForAndroidPermission);

                this.calllist.remove(j);
                this.mActiveCall = null;

                break;
            }

        }



        Log.i(TAG, "RequestCallingPermissions -> end");

    }

    //call is sent to locus,waiting remote to accept
    public void onEventMainThread(CallControlLocusCreatedEvent event) {
        Log.i(TAG, "CallControlLocusCreatedEvent -> is received ");
        Log.i(TAG, "call of locuskey " + event.getLocusKey() + " : is Created");

        //save locuskey into call object
        this.mActiveCall.locusKey = event.getLocusKey();


    }

    public void onEventMainThread(CallControlParticipantLeftEvent event) {

        Log.i(TAG, "CallControlParticipantLeftEvent is received ");

        //no Activecall
        if(this.mActiveCall == null)
        {
            return;
        }
        Log.i(TAG, "event.lockskey is " + event.getLocusKey().toString());
        Log.i(TAG, "this.mActiveCall.locusKey is " + this.mActiveCall.locusKey.toString());
        if(event.getLocusKey().toString().equals(this.mActiveCall.locusKey.toString()))
        {
            Log.i(TAG, "ActiveCall is end by remoted");

            this.removeCallAndMarkIt(this.mActiveCall,CallObserver.DisconnectedReason.remoteHangUP);

        }

    }

    public void onEventMainThread(CallControlSelfParticipantLeftEvent event) {

        Log.i(TAG, "CallControlSelfParticipantLeftEvent is received ");
        //no Activecall
        if(this.mActiveCall == null)
        {
            return;
        }
        Log.i(TAG, "event.lockskey is " + event.getLocusKey().toString());
        Log.i(TAG, "this.mActiveCall.locusKey is " + this.mActiveCall.locusKey.toString());

        if(event.getLocusKey().toString().equals(this.mActiveCall.locusKey.toString()))
        {
            Log.i(TAG, "ActiveCall is ended");

            this.removeCallAndMarkIt(this.mActiveCall,CallObserver.DisconnectedReason.selfHangUP);



        }

    }


    //case 1: call is rejected by remoted
    public void onEventMainThread(CallControlLeaveLocusEvent event) {

        Log.i(TAG, "CallControlLeaveLocusEvent is received ");

        //no Activecall
        if(this.mActiveCall == null)
        {
            return;
        }

        //new CallControlLeaveLocusEvent(locusData, false, false, false, false, ""); is declined
        //wasMediaFlowing,wasUCCall,wasRoomCall,wasRoomCallConnected all are false  is declined
        if (!(event.wasMediaFlowing() || event.wasUCCall() || event.wasRoomCall() || event
                .wasRoomCallConnected())) {

            Log.i(TAG, "Call is Rejected ");
            this.removeCallAndMarkIt(this.mActiveCall,CallObserver.DisconnectedReason.remoteReject);


        }

    }


    public void onEventMainThread(CallControlEndLocusEvent event) {

        Log.i(TAG, "CallControlEndLocusEvent is received ");
        //no Activecall
        if(this.mActiveCall == null)
        {
            return;
        }
        Log.i(TAG, "event.lockskey is " + event.getLocusKey().toString());
        Log.i(TAG, "this.mActiveCall.locusKey is " + this.mActiveCall.locusKey.toString());

        if(event.getLocusKey().toString().equals(this.mActiveCall.locusKey.toString()))
        {
            Log.i(TAG, "ActiveCall is ended");

            this.removeCallAndMarkIt(this.mActiveCall,CallObserver.DisconnectedReason.callEnd);



        }

    }

    public void onEventMainThread(CallControlCallJoinErrorEvent event) {

        Log.i(TAG, "CallControlCallJoinErrorEvent is received ");
        //no Activecall
        if(this.mActiveCall == null)
        {
            return;
        }


    }




    //remoted send acknowledge and it means it is RINGING
    public void onEventMainThread(ParticipantNotifiedEvent event) {
        Log.i(TAG, "ParticipantNotifiedEvent -> is received ");

        if (this.mActiveCall == null) return;

        Log.i(TAG, "ActiveCall is not null");
        //sync call status

        this.mActiveCall.status = Call.CallStatus.RINGING;


        //notify ui
        this.mActiveCall.getObserver().onRinging();

        //only show local and remoted video after remoted accept call
        /*
        if(this.mActiveCall.calltype == Call.CallType.VIDEO){
            Log.i(TAG, "set local view " + (mLocalSurfaceView == null));

            callControlService.setPreviewWindow(event.getLocusKey(), this.mLocalSurfaceView);
        }
        */
    }

    //remote accept call, call will be setup
    public void onEventMainThread(CallControlParticipantJoinedEvent event) {
        Log.i(TAG, "CallControlParticipantJoinedEvent -> is received ");


        //only show local and remoted video after remoted accept call
        if(this.mActiveCall.calltype == Call.CallType.VIDEO){

            callControlService.setRemoteWindow(event.getLocusKey(), this.mRemoteSurfaceView);
            callControlService.setPreviewWindow(event.getLocusKey(), this.mLocalSurfaceView);
        }

        //sync call status

        this.mActiveCall.status = Call.CallStatus.CONNECTED;

        //notify ui
        this.mActiveCall.getObserver().onConnected();
    }


    // receive INCOMING call
    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CallNotificationEvent event) {
        Log.i(TAG, "CallNotificationEvent " + event.getType());
        if (event.getType() == CallNotificationType.INCOMING) {
            Log.i(TAG, "InComing Call");
            Call call = new Call(this);
            call.direction = Call.Direction.INCOMING;
            call.status = Call.CallStatus.RINGING;
            call.calltype = Call.CallType.VIDEO;
            call.locusKey = event.getLocusKey();
            this.calllist.add(call);

            if (incomingCallObserver != null)
                incomingCallObserver.onIncomingCall(call);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ParticipantJoinedEvent event) {
        Log.i(TAG, "ParticipantJointed Event");
    }
}
