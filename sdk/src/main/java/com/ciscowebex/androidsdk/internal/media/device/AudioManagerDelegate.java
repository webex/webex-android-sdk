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

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import com.github.benoitdion.ln.Ln;

public class AudioManagerDelegate {

    private AudioManager delegate;
    private boolean hasAudioFocus;

    public AudioManagerDelegate(Context context) {
        delegate = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setSpeakerphoneOn(boolean on) {
        Ln.i("AudioManager.setSpeakerphoneOn(%b)", on);
        delegate.setSpeakerphoneOn(on);
    }

    public boolean isSpeakerphoneOn() {
        return delegate.isSpeakerphoneOn();
    }

    public void setMode(int mode) {
        Ln.i("AudioManager.setMode(%d) from (%d)", mode, delegate.getMode());
        delegate.setMode(mode);
    }

    public int getMode() {
        return delegate.getMode();
    }

    public boolean isMusicActive() {
        return delegate.isMusicActive();
    }

    public boolean isWiredHeadsetOn() {
        boolean isWiredHeadsetOn = delegate.isWiredHeadsetOn();
        Ln.i("AudioManager.isWiredHeadsetOn() = %b", isWiredHeadsetOn);
        return isWiredHeadsetOn;
    }

    public boolean isBluetoothScoOn() {
        boolean isBluetoothScoOn = delegate.isBluetoothScoOn();
        Ln.i("AudioManager.isBluetoothScoOn() = %b", isBluetoothScoOn);
        return isBluetoothScoOn;
    }

    public boolean isWiredHeadsetOrBluetoothConnected() {
        boolean isWiredHeadsetOrBluetoothConnected = isBluetoothScoOn() || isWiredHeadsetOn();
        Ln.i("AudioManager.isWiredHeadsetOrBluetoothConnected() = %b", isWiredHeadsetOrBluetoothConnected);
        return isWiredHeadsetOrBluetoothConnected;
    }

    public boolean isBluetoothScoAvailableOffCall() {
        boolean isBluetoothScoAvailableOffCall = delegate.isBluetoothScoAvailableOffCall();
        Ln.i("AudioManager.isBluetoothScoAvailableOffCall() = %b", isBluetoothScoAvailableOffCall);
        return isBluetoothScoAvailableOffCall;
    }

    public int getStreamVolume(int streamType) {
        int volume = delegate.getStreamVolume(streamType);
        Ln.i("AudioManager.getStreamVolume() type = %d, volume = %d", streamType, volume);
        return volume;
    }

    public int getStreamMaxVolume(int streamType) {
        int maxVolume = delegate.getStreamMaxVolume(streamType);
        Ln.i("AudioManager.getStreamMaxVolume() type = %d, volume = %d", streamType, maxVolume);
        return maxVolume;
    }

    public int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l, int streamType, int durationHint) {
        Ln.i("AudioManager.requestAudioFocus() streamType = %d", streamType);
        hasAudioFocus = true;
        return delegate.requestAudioFocus(l, streamType, durationHint);
    }

    public int abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l) {
        Ln.i("AudioManager.abandonAudioFocus()");
        hasAudioFocus = false;
        return delegate.abandonAudioFocus(l);
    }

    public boolean hasAudioFocus() {
        return hasAudioFocus;
    }

    public void startBluetoothSco() {
        Ln.i("AudioManager.startBluetoothSco()");
        // sleep 1 second to wait BT device is completely initialized.
        new Handler().postDelayed(() -> {
            Ln.i("AudioManager.startBluetoothSco() executes after 1 second");
            delegate.startBluetoothSco();
        }, 1000);
    }

    public void stopBluetoothSco() {
        Ln.i("AudioManager.stopBluetoothSco()");
        if (isBluetoothScoOn()) {
            delegate.setBluetoothScoOn(false);
            delegate.stopBluetoothSco();
        }
    }

    public void setBluetoothScoAvailableOffCall(boolean bluetoothScoAvailableOffCall) {
        Ln.d("AudioManager.setBluetoothScoAvailableOffCall() Wrong call! This is for test use.");
    }

}
