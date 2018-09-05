package com.ciscowebex.androidsdk.phone;

import android.graphics.Rect;
import android.view.View;


/**
 * Created by qimdeng on 8/9/18.
 *
 * An AuxStream instance represents an auxiliary stream.
 *
 * @since 2.0.0
 */

public interface AuxStream {
    /**
     * @return the view rendering upon this auxiliary stream
     * @since 2.0.0
     */
    View getRenderView();

    /**
     * @return the video dimensions of this auxiliary stream in pixels
     * @since 2.0.0
     */
    Rect getSize();

    /**
     * @return the person represented this auxiliary stream
     * @since 2.0.0
     */
    CallMembership getPerson();

    /**
     * @return whether the auxiliary stream is sending video
     * @since 2.0.0
     */
    boolean isSendingVideo();

    /**
     * Update the auxiliary stream view. When the view size is changed, you may need to refresh the view.
     * @since 2.0.0
     */
    void refresh();

    /**
     * Close this auxiliary stream. Client can manually invoke this API to close stream or SDK will automatically
     * close the last opened stream if needed.
     * @see {@link Call#closeAuxStream}
     * @since 2.0.0
     */
    void close();
}
