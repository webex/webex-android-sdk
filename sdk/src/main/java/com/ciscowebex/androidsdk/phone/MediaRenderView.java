/*
 * Copyright 2016-2017 Cisco Systems Inc
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

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;

import com.webex.wseclient.WseSurfaceView;


/**
 * Spark media view for local, remote and screen share
 * <p>
 * This view is use by {@link MediaOption} and should be placed in Android layout xml file.
 * <p>
 * e.g. <MediaRenderView android:id="@+id/localView" />
 *
 * @see MediaOption
 * @since 1.4
 */
public class MediaRenderView extends WseSurfaceView {
    public MediaRenderView(Context context) {
        super(context);
    }

    public MediaRenderView(Context var1, AttributeSet var2) {
        super(var1, var2);
    }

    public MediaRenderView(Context var1, AttributeSet var2, int var3) {
        super(var1, var2, var3);
    }

    @TargetApi(21)
    public MediaRenderView(Context var1, AttributeSet var2, int var3, int var4) {
        super(var1, var2, var3, var4);
    }
}
