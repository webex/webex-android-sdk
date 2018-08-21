package com.ciscowebex.androidsdk.phone.internal;


import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.View;

import com.cisco.spark.android.locus.model.LocusKey;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.people.Person;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.RemoteAuxVideo;

/**
 * Created by qimdeng on 8/8/18.
 */

public class RemoteAuxVideoImpl implements RemoteAuxVideo {
    private @NonNull
    LocusKey _key;

    private @NonNull PhoneImpl _phone;

    private @NonNull long _vid;

    private View _renderView;

    private boolean isSendingVideo;

    private boolean isReceivingVideo = true;

    private CallMembership _person;

    private Rect _size;

    RemoteAuxVideoImpl(@NonNull LocusKey key, @NonNull PhoneImpl phone, @NonNull long vid, @Nullable View view)  {
        _key = key;
        _phone = phone;
        _vid = vid;
        _renderView = view;
    }

    @Override
    public long getVid() {
        return _vid;
    }

    @Override
    public void addRenderView(View view){
        if (_renderView == null){
            _renderView = view;
            _phone.getCallService().setRemoteWindowForVid(_key, _vid, view);
        }
    }

    @Override
    public void removeRenderView(){
        if (_renderView != null){
            _phone.getCallService().removeRemoteWindowForVid(_key, _vid, _renderView);
        }
    }

    @Override
    public void updateRenderView() {
        if (_renderView != null){
            _phone.getCallService().updateRemoteWindowForVid(_key, _vid, _renderView);
        }
    }

    @Override
    public boolean isSendingVideo() {
        return isSendingVideo;
    }

    public void setSendingVideo(boolean isSending){
        isSendingVideo = isSending;
    }

    @Override
    public boolean isReceivingVideo() {
        return isReceivingVideo;
    }

    @Override
    public void setReceivingVideo(boolean isReceiving){
        isReceivingVideo = isReceiving;
        _phone.getCallService().muteRemoteVideoForVid(_key, _vid, !isReceiving);
    }

    @Override
    public Rect getAuxVideoSize() {
        return _size;
    }

    public void setAuxVideoSize(Rect size){
        _size = size;
    }

    @Override
    public CallMembership getPerson() {
        return _person;
    }

    public void setPerson(CallMembership person) {
        _person = person;
    }
}