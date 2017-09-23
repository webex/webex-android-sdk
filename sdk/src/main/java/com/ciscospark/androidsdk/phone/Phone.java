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

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.View;
import com.ciscospark.androidsdk.CompletionHandler;

/**
 * Created by zhiyuliu on 04/09/2017.
 */
public interface Phone {

    enum FacingMode {
        USER, ENVIROMENT
    }

    interface IncomingCallListener {
        void onIncomingCall(Call call);
    }

    IncomingCallListener getIncomingCallListener();

    void setIncomingCallListener(IncomingCallListener listener);

    FacingMode getDefaultFacingMode();

    void setDefaultFacingMode(FacingMode mode);

    void register(@NonNull CompletionHandler<Void> callback);

    void deregister(@NonNull CompletionHandler<Void> callback);

    void dial(@NonNull String dialString, @NonNull CallOption option, @NonNull CompletionHandler<Call> callback);

    void startPreview(View view);

    void stopPreview();

    void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Boolean> callback);

    void disableVideoCodecActivation();
}
