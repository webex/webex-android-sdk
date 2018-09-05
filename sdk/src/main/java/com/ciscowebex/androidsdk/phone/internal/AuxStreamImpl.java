package com.ciscowebex.androidsdk.phone.internal;


import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.cisco.spark.android.locus.model.LocusKey;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.AuxStream;

/**
 * Created by qimdeng on 8/8/18.
 */

public class AuxStreamImpl implements AuxStream {
    private @NonNull
    LocusKey _key;

    private @NonNull PhoneImpl _phone;

    private @NonNull long _vid;

    private View _renderView;

    private boolean isSendingVideo;

    private CallMembership _person;

    private Rect _size;

    AuxStreamImpl(@NonNull LocusKey key, @NonNull PhoneImpl phone, @NonNull long vid, @Nullable View view)  {
        _key = key;
        _phone = phone;
        _vid = vid;
        _renderView = view;
    }

    public long getVid(){return _vid;}

    @Override
    public View getRenderView() {
        return _renderView;
    }

    @Override
    public void refresh() {
        if (_renderView != null){
            _phone.getCallService().updateRemoteWindowForVid(_key, _vid, _renderView);
        }
    }

    @Override
    public boolean isSendingVideo() {
        return isSendingVideo;
    }

    @Override
    public void close() {
        CallImpl call = _phone.getCall(_key);
        if (call != null){
            call.closeAuxStream(this, _renderView);
        }
    }

    public void setSendingVideo(boolean isSending){
        isSendingVideo = isSending;
    }

    @Override
    public Rect getSize() {
        return _size;
    }

    public void setSize(Rect size){
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