package com.ciscowebex.androidsdk.phone;

import android.graphics.Rect;
import android.view.View;


/**
 * Created by qimdeng on 8/9/18.
 */

public interface RemoteAuxVideo {
    long getVid();

    void addRenderView(View view);

    void removeRenderView();

    void updateRenderView();

    void setReceivingVideo(boolean isReceiving);

    boolean isSendingVideo();

    boolean isReceivingVideo();

    Rect getAuxVideoSize();

    CallMembership getPerson();
}
