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

package com.ciscowebex.androidsdk.phone;

import android.util.Size;
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
    Size getSize();

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
     * @deprecated
     */
    @Deprecated
    void refresh();

    /**
     * Close this auxiliary stream. Client can manually invoke this API to close stream or SDK will automatically
     * close the last opened stream if needed.
     * @see Call#closeAuxStream(View view)
     * @since 2.0.0
     */
    void close();
}
