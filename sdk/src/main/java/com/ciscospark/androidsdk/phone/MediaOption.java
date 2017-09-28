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

package com.ciscospark.androidsdk.phone;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 *  A data type represents the media options of a call.
 *  
 *  @since 0.1
 */
public class MediaOption {

    /**
     * Constructs an audio only media option.
     
     * @since 0.1
     */
    public static MediaOption audioOnly() {
        return new MediaOption(null, null);
    }

    /**
     * Constructs an audio and video media option.
     * 
     * @param localView Video view for self.
     * @param remoteView Video view for remote
     * @since 0.1
     */
    public static MediaOption audioVideo(@NonNull View localView, @NonNull View remoteView) {
        return new MediaOption(localView, remoteView);
    }

    private View _remoteView;

    private View _localView;

    private MediaOption(@Nullable View localView, @Nullable  View remoteView) {
        _localView = localView;
        _remoteView = remoteView;
    }
	
    public boolean hasVideo() {
        return _localView != null || _remoteView != null;
    }
	
    public View getRemoteView() {
        return _remoteView;
    }
	
    public View getLocalView() {
        return _localView;
    }
}
