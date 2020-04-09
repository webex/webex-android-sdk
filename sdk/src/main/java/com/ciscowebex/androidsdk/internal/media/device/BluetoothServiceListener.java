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

package com.ciscowebex.androidsdk.internal.media.device;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import com.github.benoitdion.ln.Ln;

public class BluetoothServiceListener implements BluetoothProfile.ServiceListener {

    private AudioDeviceConnectionManager audioDeviceConnectionManager;

    public BluetoothServiceListener(AudioDeviceConnectionManager audioDeviceConnectionManager) {
        this.audioDeviceConnectionManager = audioDeviceConnectionManager;
    }

    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        Ln.d("BluetoothServiceListener.onServiceConnected, profile = " + (profile == BluetoothProfile.HEADSET ? "HEADSET" : "A2DP"));
        if (BluetoothProfile.HEADSET == profile) {
            audioDeviceConnectionManager.setBluetoothHeadset((BluetoothHeadset) proxy);
        }
        else if (BluetoothProfile.A2DP == profile) {
            audioDeviceConnectionManager.setBluetoothA2dp((BluetoothA2dp) proxy);
        }
    }

    public void onServiceDisconnected(int profile) {
        Ln.d("BluetoothServiceListener.onServiceDisconnected, profile = " + (profile == BluetoothProfile.HEADSET ? "HEADSET" : "A2DP"));
        if (BluetoothProfile.HEADSET == profile) {
            audioDeviceConnectionManager.setBluetoothHeadset(null);
        }
        else if (BluetoothProfile.A2DP == profile) {
            audioDeviceConnectionManager.setBluetoothA2dp(null);
        }
    }
}
