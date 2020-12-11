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

package com.ciscowebex.androidsdk.internal.media.device;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import com.github.benoitdion.ln.Ln;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    AudioDeviceConnectionManager audioDeviceConnectionManager;

    public BluetoothBroadcastReceiver(AudioDeviceConnectionManager manager) {
        audioDeviceConnectionManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int extraState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            Ln.i("BluetoothBroadcastReceiver, action: " + action + "; extraState: " + extraState);
            if (BluetoothHeadset.STATE_CONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothStateUpdated();
            }
            else if (BluetoothHeadset.STATE_DISCONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothStateUpdated();
            }
        }
        else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int extraState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
            Ln.i("BluetoothBroadcastReceiver, action: " + action + "; extraState: " + extraState);
            if (BluetoothA2dp.STATE_CONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothStateUpdated();
            }
            else if (BluetoothA2dp.STATE_DISCONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothStateUpdated();
            }
        }
        else if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
            int scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
            Ln.i("BluetoothBroadcastReceiver, sco state: " + scoState);
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == scoState) {
                audioDeviceConnectionManager.updateAudioVolume();
            }
        }
    }
}

