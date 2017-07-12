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
import com.cisco.spark.android.callcontrol.events.CallControlDisableVideoEvent;
import com.cisco.spark.android.callcontrol.events.CallControlDisconnectedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndWhiteboardShare;
import com.cisco.spark.android.callcontrol.events.CallControlFloorGrantedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlFloorReleasedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlHeldEvent;
import com.cisco.spark.android.callcontrol.events.CallControlInvalidLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlJoinedLobbyEvent;
import com.cisco.spark.android.callcontrol.events.CallControlJoinedMeetingEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLeaveLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalAudioMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusCreatedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusPmrChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLostEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMediaDeviceListenersRegisteredEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsExpelEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsLockEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsStatusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingNotStartedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingRecordEvent;
import com.cisco.spark.android.callcontrol.events.CallControlModeratorMutedParticipantEvent;
import com.cisco.spark.android.callcontrol.events.CallControlNumericDialingPreventedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantAudioMuteEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantJoinedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlPhoneStateChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlReconnectEvent;
import com.cisco.spark.android.callcontrol.events.CallControlResumedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlViewWhiteboardShare;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationRemoveEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.CallNotificationUpdateEvent;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.locus.events.IncomingCallEvent;
import com.cisco.spark.android.locus.events.ParticipantJoinedEvent;
import com.cisco.spark.android.locus.events.ParticipantNotifiedEvent;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.sync.ActorRecord;
import com.ciscospark.Spark;
import com.ciscospark.common.SparkError;
import com.ciscospark.core.SparkApplication;
import com.webex.wseclient.WseSurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static com.ciscospark.phone.DialObserver.ErrorCallJoin;
import static com.ciscospark.phone.DialObserver.ErrorPermission;
import static com.ciscospark.phone.RegisterListener.ErrorNotAuthorized;
import static com.ciscospark.phone.RegisterListener.ErrorTimeout;

/**
 * The type Phone.
 */
public class Phone {

    private Spark mspark;

    private List<Call> calllist;

    private static final String TAG = "Phone";

    private ApplicationDelegate applicationDelegate;

    /**
     * The Application controller.
     */
    @Inject
    ApplicationController applicationController;

    /**
     * The Api token provider.
     */
    @Inject
    ApiTokenProvider apiTokenProvider;

    /**
     * The Api client provider.
     */
    @Inject
    ApiClientProvider apiClientProvider;

    /**
     * The Call control service.
     */
    @Inject
    CallControlService callControlService;

    /**
     * The Media engine.
     */
    @Inject
    MediaEngine mediaEngine;

    /**
     * The Bus.
     */
    @Inject
    EventBus bus;


    /**
     * The M remote surface view.
     */
    WseSurfaceView mRemoteSurfaceView;

    /**
     * The M local surface view.
     */
    WseSurfaceView mLocalSurfaceView;

    private CallContext callContext;

    //keep temp listener
    private RegisterListener mRegisterListener;

    //registered on WMD or not
    private boolean isRegisterInWDM;

    //as common lib doesn't provide timeout event, we use this to handle timeout
    private Timer mTimer;

    private Handler mTimeHandler;
    private Runnable mTimeRunnable;



    //keep activitied call reference

    private Call mActiveCall;


    //it is used to save call  period between dial and get first locus feed back.
    private Call mDialoutCall;

    private  DialObserver mdialObserver;


    private IncomingCallObserver incomingCallObserver;

    /**
     * @deprecated
     *
     */
    public Phone(Spark spark) {
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


    /**
     * Gets active call.
     *
     * @return the active call
     */
    protected Call getActiveCall() {
        return this.mActiveCall;
    }

    /**
     * Sets active call.
     *
     * @param call the call
     */
    protected void setActiveCall(Call call) {
        this.mActiveCall = call;
    }

    /**
     * Sets incoming call observer.
     *
     * @param observer the observer
     */
    public void setIncomingCallObserver(IncomingCallObserver observer) {
        this.incomingCallObserver = observer;
    }


    /**
     * @deprecated
     * Close.
     */
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

    private boolean isAuthorized() {
        Log.i(TAG, "isAuthorized: ->");

        if (this.mspark.getStrategy().getToken() == null) {

            Log.i(TAG, "register: -> no valid Token");

            return false;

        }

        return true;

    }


    /**
     * Register.
     *
     * @param listener the listener
     */
    public void register(RegisterListener listener) {

        Log.i(TAG, "register: ->start");

        if (!isAuthorized()) {

            //not authorized
            SparkError error = new SparkError(null,ErrorNotAuthorized);

            listener.onFailed(error);


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

        this.mRegisterListener = listener;

        new AuthenticatedUserTask(applicationController).execute();


        //start to monitor register timeout
        startRegisterTimer(Constant.timeout);

        Log.i(TAG, "register: ->end ");
    }


    private void startRegisterTimer(int timeoutinSeconds) {

        this.mTimeRunnable = new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "run: -> register timeout");

                if (!Phone.this.isRegisterInWDM) {
                    if (Phone.this.mRegisterListener != null) {
                        SparkError error = new SparkError(null,ErrorTimeout);
                        Phone.this.mRegisterListener.onFailed(error);
                    }
                }
            }
        };

        this.mTimeHandler.postDelayed(this.mTimeRunnable, timeoutinSeconds * 1000);


    }


    /**
     * Deregister.
     *
     * @param listener the listener
     */
    public void deregister(DeregisterListener listener) {
        Log.i(TAG, "deregister: ->start");

        this.logout();

        listener.onSuccess();

    }

    /**
     * Hangup.
     */
    //hang up current active call
    protected void hangup() {
        Log.i(TAG, "hangup: ->Start");
        if ((this.mActiveCall == null)&&(this.mDialoutCall == null) ){
            Log.i(TAG, " no active call neither dialout call");
            return;
        }

        if (this.mDialoutCall != null) {

            //call is setup
            Log.i(TAG, "DialoutCall");

            this.callControlService.cancelCall(true);
            return;

        }

        if ((this.mActiveCall.status == Call.CallStatus.INITIATED) || (this.mActiveCall.status == Call.CallStatus.RINGING)) {

            //call is not setup
            Log.i(TAG, "cancelCall");

            this.callControlService.cancelCall(true);
            return;
        }
        if (this.mActiveCall.status == Call.CallStatus.CONNECTED) {

            //call is setup
            Log.i(TAG, "leaveCall");

            this.callControlService.leaveCall(this.mActiveCall.locusKey);
            return;

        }


    }

    private void logout() {
        Log.i(TAG, "logout: ->1");
        applicationController.logout(null, false);
        this.isRegisterInWDM = false;

    }


    /**
     * Dial.
     *
     * @param dialString the dial string
     * @param option     the option
     * @param observer   the observer
     */
    public void dial(String dialString, CallOption option, DialObserver observer) {
        Log.i(TAG, "dial: ->start");

        if (!this.isRegisterInWDM) {
            Log.i(TAG, "register wdm failed");
            SparkError error = new SparkError(null,DialObserver.ErrorStatus);

            observer.onFailed(error);
            return;
        }

        //an active call is ongoing , or a dialout is ongoning but not get locus feedback yet
        if ((this.mActiveCall != null) ||(this.mDialoutCall != null)) {

            Log.i(TAG, "isInActivitiedCall");
            SparkError error = new SparkError(null,DialObserver.ErrorStatus);

            observer.onFailed(error);
            return;
        }


        if (!setCallOption(option)) {
            Log.i(TAG, "setCallOption failed");
            SparkError error = new SparkError(null,DialObserver.ErrorParameter);

            observer.onFailed(error);
            //observer.onFailed(DialObserver.ErrorCode.ERROR_PARAMETER);
            return;
        }


        if (option.mCalltype == CallOption.CallType.VIDEO) {

            Log.i(TAG, "videoCall");

            Call call = new Call(this);


            call.status = Call.CallStatus.INITIATED;
            call.direction = Call.Direction.OUTGOING;
            call.calltype = Call.CallType.VIDEO;

            //add this call to list
            //this.calllist.add(call);

            //set this call as active call
            //this.mActiveCall = call;

            //save call
            this.mDialoutCall = call;

            //joinCall will trigger permission synchronouslly
            //observer.onSuccess(call);

            //save dialObserver
            this.mdialObserver = observer;

            CallContext callContext = new CallContext.Builder(dialString).build();
            callControlService.joinCall(callContext);
            Log.i(TAG, "dial: ->sendout");
        }

        if (option.mCalltype == CallOption.CallType.AUDIO) {

            Log.i(TAG, "AudioCall");

            Call call = new Call(this);


            call.status = Call.CallStatus.INITIATED;
            call.direction = Call.Direction.OUTGOING;
            call.calltype = Call.CallType.AUDIO;

            //add this call to list
            //this.calllist.add(call);

            //set this call as active call
            //this.mActiveCall = call;

            //save call
            this.mDialoutCall = call;



            //joinCall will trigger permission synchronouslly
            //observer.onSuccess(call);

            //save dialObserver
            this.mdialObserver = observer;

            callContext = new CallContext.Builder(dialString).setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioOnly).build();
            callControlService.joinCall(callContext);
            Log.i(TAG, "dial: ->sendout");
        }

    }

    /**
     * Sets call option.
     *
     * @param option the option
     * @return the call option
     */
    protected boolean setCallOption(CallOption option) {
        if (option.mCalltype == CallOption.CallType.VIDEO) {

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

        if (option.mCalltype == CallOption.CallType.AUDIO) {

            Log.i(TAG, "AudioCall");

            return true;
        }

        // Other CallType
        return false;
    }

    /**
     * Remove call and mark it.
     *
     * @param call      the call
     * @param reason    the reason
     * @param errorInfo the error info
     */
    //*.remove call from array
    //
    //*. set the removed call to disconnected
    //*. set phone.Activecall as null
    //*. inform UI call end reason
    protected void removeCallAndMarkIt(Call call, CallObserver.DisconnectedReason reason,
                                       SparkError errorInfo) {
        Log.i(TAG, "removeCallfromArray: ->start");


        for (int j = 0; j < this.calllist.size(); j++) {
            Call call2 = this.calllist.get(j);

            //same call object
            if (call == call2) {

                Log.i(TAG, "find the removed callobject from list");

                //set it to disconnected
                call2.status = Call.CallStatus.DISCONNECTED;

                //notify UI why this call is dead,
                if (this.mActiveCall.getObserver() != null) {
                    this.mActiveCall.getObserver().onDisconnected(this.mActiveCall, reason,errorInfo);
                }

                this.calllist.remove(j);
                this.mActiveCall = null;

                break;
            }

        }


    }

    /**
     * Start pre view.
     */
    public void startPreView() {

    }

    /**
     * Stop pre view.
     */
    public void stopPreView() {

    }



    /**
     *
     * @deprecated
     * @param event the event
     */
    //registed a WDM successfully
    public void onEventMainThread(DeviceRegistrationChangedEvent event) {

        Log.i(TAG, "DeviceRegistrationChangedEvent -> is received ");

        if (this.mRegisterListener == null) {
            //in case, even logout is called, common lib still send out event by using old date
            // to register
            Log.i(TAG, "this.mRegisterListener is null ");
            return;
        }

        if (!isAuthorized()) {
            Log.i(TAG, "not authorized,something wrong! ");
            //not authorized,something wrong
            return;
        }

        this.mRegisterListener.onSuccess();

        //successfully registered
        this.isRegisterInWDM = true;

        //cancel register timer
        this.mTimeHandler.removeCallbacks(this.mTimeRunnable);

        Log.i(TAG, "onEventMainThread: Registered:" + event.getDeviceRegistration().getId());

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    //request camera and microphone permission
    public void onEventMainThread(RequestCallingPermissions event) {
        Log.i(TAG, "RequestCallingPermissions -> is received ");

        List<String> permissions = new ArrayList<String>();

        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CAMERA);

        if (this.mDialoutCall == null) {
            Log.i(TAG, "Something is Wrong, how mDialoutCall is null");
            return;
        }

        //notify dialobserver to approval permission
        if(this.mdialObserver == null){
            Log.i(TAG, "Something is Wrong, how mdialObserver is null");
            return;
        }

        SparkError error = new SparkError(null,ErrorPermission);

        this.mdialObserver.onFailed(error);

        this.mdialObserver.onPermissionRequired(permissions);

        this.mDialoutCall = null;

        this.mdialObserver = null;


        //remove dialOut call

        //this.mActiveCall.getObserver().onPermissionRequired(permissions);

        //during the first dial, the permission event will happen.
        //it means the first dial end for permission reason

        //need to remove this ActiveCall and set it to DISCONNECTED
        /*
        for (int j = 0; j < this.calllist.size(); j++) {
            Call call = this.calllist.get(j);

            //same call object
            if (this.mActiveCall == call) {

                Log.i(TAG, "find the mActiveCall from list");

                this.mActiveCall.status = Call.CallStatus.DISCONNECTED;


                //notify UI why this call is dead,
                this.mActiveCall.getObserver().onDisconnected(this.mActiveCall, CallObserver
                        .DisconnectedReason.endForAndroidPermission,null);

                this.calllist.remove(j);
                this.mActiveCall = null;

                break;
            }

        }
        */


        Log.i(TAG, "RequestCallingPermissions -> end");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    //locus has create call,waiting remote to accept
    public void onEventMainThread(CallControlLocusCreatedEvent event) {
        Log.i(TAG, "CallControlLocusCreatedEvent -> is received ");
        Log.i(TAG, "call of locuskey " + event.getLocusKey() + " : is Created");

        //save locuskey into call object
        //this.mActiveCall.locusKey = event.getLocusKey();
        if(this.mDialoutCall == null){
            //sometimes same locus creation event will be sent twice or more

            if(event.getLocusKey() == this.mActiveCall.locusKey){
                Log.i(TAG, "Get same locus event again and SAME locus key! ");
            }
            else
            {
                Log.i(TAG, "Get same locus event again But NOT SAME locus key!" );
            }

            return;

        }
        this.mDialoutCall.locusKey = event.getLocusKey();

        //add this call to list
        this.calllist.add(this.mDialoutCall);

        //set this call as active call
        this.mActiveCall = this.mDialoutCall;



        this.mdialObserver.onSuccess(this.mActiveCall);

        this.mdialObserver = null;
        this.mDialoutCall = null;


    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlParticipantLeftEvent event) {

        Log.i(TAG, "CallControlParticipantLeftEvent is received ");

        //no Activecall
        if (this.mActiveCall == null) {
            return;
        }
        Log.i(TAG, "event.lockskey is " + event.getLocusKey().toString());
        Log.i(TAG, "this.mActiveCall.locusKey is " + this.mActiveCall.locusKey.toString());
        if (event.getLocusKey().toString().equals(this.mActiveCall.locusKey.toString())) {
            Log.i(TAG, "ActiveCall is end by remoted");

            this.removeCallAndMarkIt(this.mActiveCall, CallObserver.DisconnectedReason
                    .remoteHangUP,null);

        }

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlSelfParticipantLeftEvent event) {

        Log.i(TAG, "CallControlSelfParticipantLeftEvent is received ");
        //no Activecall
        if (this.mActiveCall == null) {
            return;
        }
        Log.i(TAG, "event.lockskey is " + event.getLocusKey().toString());
        Log.i(TAG, "this.mActiveCall.locusKey is " + this.mActiveCall.locusKey.toString());

        if (event.getLocusKey().toString().equals(this.mActiveCall.locusKey.toString())) {
            Log.i(TAG, "ActiveCall is ended");

            this.removeCallAndMarkIt(this.mActiveCall, CallObserver.DisconnectedReason
                    .selfHangUP,null);


        }

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    //case 1: call is rejected by remoted
    public void onEventMainThread(CallControlLeaveLocusEvent event) {

        Log.i(TAG, "CallControlLeaveLocusEvent is received ");

        //no Activecall
        if (this.mActiveCall == null) {
            return;
        }

        //new CallControlLeaveLocusEvent(locusData, false, false, false, false, ""); is declined
        //wasMediaFlowing,wasUCCall,wasRoomCall,wasRoomCallConnected all are false  is declined
        if (!(event.wasMediaFlowing() || event.wasUCCall() || event.wasRoomCall() || event.wasRoomCallConnected())) {

            Log.i(TAG, "Call is Rejected ");
            this.removeCallAndMarkIt(this.mActiveCall, CallObserver.DisconnectedReason
                    .remoteReject,null);
        }
    }


    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlEndLocusEvent event) {

        Log.i(TAG, "CallControlEndLocusEvent is received ");
        //no Activecall
        if (this.mActiveCall == null) {
            return;
        }
        Log.i(TAG, "event.lockskey is " + event.getLocusKey().toString());
        Log.i(TAG, "this.mActiveCall.locusKey is " + this.mActiveCall.locusKey.toString());

        if (event.getLocusKey().toString().equals(this.mActiveCall.locusKey.toString())) {
            Log.i(TAG, "ActiveCall is ended");

            this.removeCallAndMarkIt(this.mActiveCall, CallObserver.DisconnectedReason.callEnd,
                    null);
        }

    }


    //before call is setup, something worng happen.
    //for exampple, self calling.

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlCallJoinErrorEvent event) {

        Log.i(TAG, "CallControlCallJoinErrorEvent is received ");
        //no Activecall and no dialoutcall
        if( (this.mActiveCall == null)&&(this.mDialoutCall == null)) {
            Log.i(TAG, "no Activecall neither dialoutcall,something wrong");
            return;
        }


        //

        if(this.mDialoutCall != null){

            Log.i(TAG, "notify dial observer failed ");
            SparkError error = new SparkError(null,ErrorCallJoin);
            this.mdialObserver.onFailed(error);

            this.mdialObserver = null;
            this.mDialoutCall = null;

            return;

        }

        if(this.mActiveCall != null ){

            Log.i(TAG, "during Activecall,receive CallJoinError ");

            return;

        }


        /*
        //self calling.
        if ((this.mActiveCall.status == Call.CallStatus.RINGING) || (this.mActiveCall.status == Call.CallStatus.INITIATED)) {
            Log.i(TAG, "self calling");
            SparkError errorinfo = new SparkError(null,Constant
                    .ErrorCallJoinError);
            this.removeCallAndMarkIt(this.mActiveCall, CallObserver.DisconnectedReason
                    .Error_serviceFailed_CallJoinError,errorinfo);
        }

        */

    }


    /**
     *
     * @deprecated
     * @param event the event
     */
    //remoted send acknowledge and it means it is RINGING
    public void onEventMainThread(ParticipantNotifiedEvent event) {
        Log.i(TAG, "ParticipantNotifiedEvent -> is received ");

        if (this.mActiveCall == null) return;

        Log.i(TAG, "ActiveCall is not null");
        //sync call status

        this.mActiveCall.status = Call.CallStatus.RINGING;


        //notify ui
        this.mActiveCall.getObserver().onRinging(this.mActiveCall);

        //only show local and remoted video after remoted accept call
        /*
        if(this.mActiveCall.calltype == Call.CallType.VIDEO){
            Log.i(TAG, "set local view " + (mLocalSurfaceView == null));

            callControlService.setPreviewWindow(event.getLocusKey(), this.mLocalSurfaceView);
        }
        */
    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    //remote accept call, call will be setup
    public void onEventMainThread(CallControlParticipantJoinedEvent event) {
        Log.i(TAG, "CallControlParticipantJoinedEvent -> is received ");


        //only show local and remoted video after remoted accept call
        if (this.mActiveCall.calltype == Call.CallType.VIDEO) {

            callControlService.setRemoteWindow(event.getLocusKey(), this.mRemoteSurfaceView);
            callControlService.setPreviewWindow(event.getLocusKey(), this.mLocalSurfaceView);
        }

        //sync call status

        this.mActiveCall.status = Call.CallStatus.CONNECTED;

        //notify ui
        this.mActiveCall.getObserver().onConnected(this.mActiveCall);
    }


    /**
     *
     * @deprecated
     * @param event the event
     */
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

            if (incomingCallObserver != null) incomingCallObserver.onIncomingCall(call);
        }
    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ParticipantJoinedEvent event) {
        Log.i(TAG, "ParticipantJointed Event " + event.getLocusKey());
    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(IncomingCallEvent event) {
        Log.i(TAG, "IncomingCallEvent " + event.getLocusKey());
    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallNotificationRemoveEvent event) {
        Log.i(TAG, "CallNotification remove event" + event.getLocusKey());
        LocusKey locusKey = event.getLocusKey();
        Iterator<Call> it = calllist.iterator();
        while (it.hasNext()) {
            Call call = it.next();
            if (call.locusKey.equals(locusKey)) {
                call.status = Call.CallStatus.DISCONNECTED;
                it.remove();
            }
        }
    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallNotificationUpdateEvent event) {
        Log.i(TAG, "CallNotification update event" + event.getLocusKey());
    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlDisableVideoEvent event) {

        Log.i(TAG, "CallControlDisableVideoEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlDisconnectedEvent event) {

        Log.i(TAG, "CallControlDisconnectedEvent is received ");

    }


    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlEndWhiteboardShare event) {

        Log.i(TAG, "CallControlEndWhiteboardShare is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlFloorGrantedEvent event) {

        Log.i(TAG, "CallControlFloorGrantedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlFloorReleasedEvent event) {

        Log.i(TAG, "CallControlFloorReleasedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlHeldEvent event) {

        Log.i(TAG, "CallControlHeldEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlInvalidLocusEvent event) {

        Log.i(TAG, "CallControlInvalidLocusEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlJoinedLobbyEvent event) {

        Log.i(TAG, "CallControlJoinedLobbyEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlJoinedMeetingEvent event) {

        Log.i(TAG, "CallControlJoinedMeetingEvent is received ");

    }


    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlLocalAudioMutedEvent event) {

        Log.i(TAG, "CallControlLocalAudioMutedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlLocalVideoMutedEvent event) {

        Log.i(TAG, "CallControlLocalVideoMutedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlLocusChangedEvent event) {

        Log.i(TAG, "CallControlLocusChangedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlLocusPmrChangedEvent event) {

        Log.i(TAG, "CallControlLocusPmrChangedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlLostEvent event) {

        Log.i(TAG, "CallControlLostEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMediaDeviceListenersRegisteredEvent event) {

        Log.i(TAG, "CallControlMediaDeviceListenersRegisteredEvent is received ");

    }

    /**
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMeetingControlsEvent event) {

        Log.i(TAG, "CallControlMeetingControlsEvent is received ");

    }

    /**
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMeetingControlsExpelEvent event) {

        Log.i(TAG, "CallControlMeetingControlsExpelEvent is received ");

    }

    /**
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMeetingControlsLockEvent event) {

        Log.i(TAG, "CallControlMeetingControlsLockEvent is received ");

    }

    /**
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMeetingControlsStatusEvent event) {

        Log.i(TAG, "CallControlMeetingControlsStatusEvent is received ");

    }

    /**
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMeetingNotStartedEvent event) {

        Log.i(TAG, "CallControlMeetingNotStartedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlMeetingRecordEvent event) {

        Log.i(TAG, "CallControlMeetingRecordEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlModeratorMutedParticipantEvent event) {

        Log.i(TAG, "CallControlModeratorMutedParticipantEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlNumericDialingPreventedEvent event) {

        Log.i(TAG, "CallControlNumericDialingPreventedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlParticipantAudioMuteEvent event) {

        Log.i(TAG, "CallControlParticipantAudioMuteEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlParticipantChangedEvent event) {

        Log.i(TAG, "CallControlParticipantChangedEvent is received ");

    }


    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlParticipantVideoMutedEvent event) {

        Log.i(TAG, "CallControlParticipantVideoMutedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlPhoneStateChangedEvent event) {

        Log.i(TAG, "CallControlPhoneStateChangedEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlReconnectEvent event) {

        Log.i(TAG, "CallControlReconnectEvent is received ");

    }

    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlResumedEvent event) {

        Log.i(TAG, "CallControlResumedEvent is received ");

    }


    /**
     *
     * @deprecated
     * @param event the event
     */
    public void onEventMainThread(CallControlViewWhiteboardShare event) {

        Log.i(TAG, "CallControlViewWhiteboardShare is received ");

    }
}

