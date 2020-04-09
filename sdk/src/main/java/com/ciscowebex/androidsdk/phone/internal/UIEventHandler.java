/*
 * Copyright 2016-2020 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.phone.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.ciscowebex.androidsdk.utils.Utils;

public class UIEventHandler {

    interface EventObserver {

        void onScreenRotation(int rotation);

        void onMediaPermission(boolean permission);

        void onScreenCapturePermission(Intent permission);
    }

    static class UIEventBroadcastReceiver extends BroadcastReceiver {

        private int rotation;

        private EventObserver observer;

        public UIEventBroadcastReceiver(EventObserver observer) {
            super();
            this.observer = observer;
        }

        EventObserver getObserver() {
            return observer;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int rotation = Utils.getScreenRotation(context);
            if (this.rotation != rotation) {
                this.rotation = rotation;
                if (observer != null) {
                    observer.onScreenRotation(rotation);
                }
            }
        }
    }

    private UIEventBroadcastReceiver receiver;

    private static UIEventHandler handler = new UIEventHandler();

    public static UIEventHandler get() {
        return handler;
    }

    private UIEventHandler(){}

    void registerUIEventHandler(Context context, EventObserver observer) {
        if (receiver == null) {
            receiver = new UIEventBroadcastReceiver(observer);
        }
        context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
    }

    void unregisterUIEventHandler(Context context) {
        if (receiver != null) {
            context.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    public void doMediaPermission(boolean permssion) {
        if (receiver != null) {
            receiver.getObserver().onMediaPermission(permssion);
        }
    }

    public void doScreenCapturePermission(Intent intent) {
        if (receiver != null) {
            receiver.getObserver().onScreenCapturePermission(intent);
        }
    }


}

