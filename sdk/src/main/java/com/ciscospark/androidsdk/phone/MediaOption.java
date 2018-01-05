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
import android.util.Pair;
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
        return new MediaOption(null, null,null,false,false);
    }

    /**
     * Constructs an audio and video media option.
     * 
     * @param localView Video view for self.
     * @param remoteView Video view for remote
     * @since 0.1
     */
    public static MediaOption audioVideo(@NonNull View localView, @NonNull View remoteView) {
        return new MediaOption(localView, remoteView,null,false,true);
    }

    /**
     * Constructs an audio and video media option.
     *
     * @param videoRenderViews Local video view and remote video view.
     * @since 1.3
     */
    public static MediaOption audioVideo(@Nullable Pair<View,View> videoRenderViews) {
        if (videoRenderViews == null || videoRenderViews.first == null || videoRenderViews.second == null) {
            return new MediaOption(null, null,null,false,true);
        }
        return new MediaOption(videoRenderViews.first, videoRenderViews.second,null,false,true);
    }

    /**
     * Constructs an audio/video and screen share media option.
     *
     * @param videoRenderViews Local video view and remote video view.
     * @param screenShareView Screen share view for remote.
     * @since 1.3
     */
    public static MediaOption audioVideoScreenShare(@Nullable Pair<View,View> videoRenderViews,@Nullable View screenShareView) {
        if (videoRenderViews == null || videoRenderViews.first == null || videoRenderViews.second == null) {
            return new MediaOption(null, null,screenShareView,true,true);
        }
        return new MediaOption(videoRenderViews.first,videoRenderViews.second,screenShareView,true,true);
    }

    private View _remoteView;

    private View _localView;

    private View _screenShareView;

    private boolean _hasScreenShare;

    private boolean _hasVideo;

    private MediaOption(@Nullable View localView, @Nullable  View remoteView,@Nullable View screenShareView,boolean hasScreenShare,boolean hasVideo) {
        _localView = localView;
        _remoteView = remoteView;
        _screenShareView = screenShareView;
        _hasScreenShare = hasScreenShare;
        _hasVideo = hasVideo;
    }
	
    /**
     * Whether video is enabled.
     *
     * @return False if neither local or remote video is enabled. Otherwise, true.
     * @since 0.1
     */
    public boolean hasVideo() {
        return _hasVideo;
    }

    /**
     * Whether video is enabled.
     *
     * @return true if screen share is enabled. Otherwise, false.
     * @since 1.3
     */
    public boolean hasScreenShare() {
        return _hasScreenShare;
    }

    /**
     * @return The remote video view
     * @since 0.1
     */
    public View getRemoteView() {
        return _remoteView;
    }
	
    /**
     * @return The local video view
     * @since 0.1
     */
    public View getLocalView() {
        return _localView;
    }

    /**
     * @return The screen share view
     * @since 1.3
     */
    public View getScreenShareView() {
        return _screenShareView;
    }

}
