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
 * Created by zhiyuliu on 01/09/2017.
 */

public class CallOption {

    public static CallOption audioOnly() {
        return new CallOption(null, null);
    }

    public static CallOption audioVideo(@NonNull View localView, @NonNull View remoteView) {
        return new CallOption(localView, remoteView);
    }

    private View _remoteView;

    private View _localView;

    private CallOption(@Nullable View localView, @Nullable  View remoteView) {
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
