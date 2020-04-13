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

import android.database.ContentObserver;
import android.os.Handler;
import com.github.benoitdion.ln.Ln;

public class VolumeSettingChangedListener extends ContentObserver {

    private AudioDeviceConnectionManager audioDeviceConnectionManager;

    public VolumeSettingChangedListener(Handler handler, AudioDeviceConnectionManager manager) {
        super(handler);
        this.audioDeviceConnectionManager = manager;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        logCurrentVolumeInfo("VolumeChange", audioDeviceConnectionManager.getAudioManager());
        audioDeviceConnectionManager.updateAudioVolume();
    }

    private static void logCurrentVolumeInfo(String tag, AudioManagerDelegate audioManager) {
        if (audioManager != null) {
            Ln.d(String.format("%s.VoiceCall Volume   : %s(%s)", tag, audioManager.getStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL), audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_VOICE_CALL)));
            Ln.d(String.format("%s.System Volume      : %s(%s)", tag, audioManager.getStreamVolume(android.media.AudioManager.STREAM_SYSTEM), audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_SYSTEM)));
            Ln.d(String.format("%s.Music Volume       : %s(%s)", tag, audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC), audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)));
            Ln.d(String.format("%s.Bluetooth Volume   : %s(%s)", tag, audioManager.getStreamVolume(AudioDeviceConnectionManager.STREAM_BLUETOOTH_SCO), audioManager.getStreamMaxVolume(AudioDeviceConnectionManager.STREAM_BLUETOOTH_SCO)));
        }
    }
}
