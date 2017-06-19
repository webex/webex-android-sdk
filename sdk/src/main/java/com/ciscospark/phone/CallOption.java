package com.ciscospark.phone;

import com.webex.wseclient.WseSurfaceView;

/**
 * Created by lm on 6/15/17.
 */

public class CallOption {

    public CallType mCalltype;

    public WseSurfaceView mRemoteView;

    public WseSurfaceView mLocalView;

    public CallOption(CallType type, WseSurfaceView remoteView, WseSurfaceView localView)
    {
        this.mCalltype = type;
        this.mLocalView = localView;
        this.mRemoteView = remoteView;
    }


    public WseSurfaceView getRemoteView(){
        return this.mRemoteView;
    }

    public void setRemoteView(WseSurfaceView view){
        this.mRemoteView = view;
    }


    public WseSurfaceView getLocalView(){
        return this.mLocalView;
    }

    public void setLocalView(WseSurfaceView view){
        this.mLocalView = view;
    }

    public enum CallType {
        AUDIO,
        VIDEO
    }
}
