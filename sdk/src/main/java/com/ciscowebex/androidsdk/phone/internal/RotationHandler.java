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

package com.ciscowebex.androidsdk.phone.internal;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.WindowManager;

public class RotationHandler {
    private static BroadcastReceiver _receiver;

    private RotationHandler(){}

    static int getRotation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return windowManager.getDefaultDisplay().getRotation();
    }


    static class RotationBroadcastReceiver extends BroadcastReceiver {
        PhoneImpl _phoneImpl;

        public RotationBroadcastReceiver(PhoneImpl phoneImpl) {
            super();
            _phoneImpl = phoneImpl;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            _phoneImpl.setDisplayRotation(getRotation(context));
        }
    }

    static void registerRotationReceiver(Context context, PhoneImpl phoneImpl) {
        if (_receiver == null) {
            _receiver = new RotationBroadcastReceiver(phoneImpl);
            context.registerReceiver(_receiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
        }
    }

    static void unregisterRotationReceiver(Context context) {
        if (_receiver != null) {
            context.unregisterReceiver(_receiver);
            _receiver = null;
        }
    }

    public static void setScreenshotPermission(final Intent permissionIntent) {
        if (_receiver != null) {
            ((RotationBroadcastReceiver) _receiver)._phoneImpl.setScreenshotPermission(permissionIntent);
        }
    }

    public static void makeCall(Bundle data, boolean permission) {
        if (_receiver != null) {
            ((RotationBroadcastReceiver) _receiver)._phoneImpl.makeCall(data, permission);
        }
    }
}

