package com.ciscowebex.androidsdk.phone;

import android.graphics.Rect;
import android.view.View;


/**
 * Created by qimdeng on 8/9/18.
 */

public interface AuxStream {
    View getRenderView();

    Rect getSize();

    CallMembership getPerson();

    void refresh();

    boolean isSendingVideo();
}
