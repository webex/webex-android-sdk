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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.media.WmeSession;
import com.ciscowebex.androidsdk.internal.media.WmeTrack;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.DeviceManager;

public class MediaDeviceMananger {

    private final Context context;
    private final DeviceManager deviceManager;
    private AudioDeviceConnectionManager audioDeviceConnectionManager;
    private BluetoothBroadcastReceiver bluetoothBroadcastReceiver;
    private BluetoothServiceListener bluetoothServiceListener;
    private HeadsetIntentReceiver headsetIntentReceiver;
    private VolumeSettingChangedListener volumeSettingChangedListener;
    private ProximitySensor proximitySensor;
    private boolean videoMutedByProximity = false;
    private boolean speakerOnBeforeProximity = false;

    public MediaDeviceMananger(Context context, WMEngine engine) {
        this.context = context;
        this.deviceManager = new DeviceManager();
        this.audioDeviceConnectionManager = new AudioDeviceConnectionManager(context, engine);
        this.bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(audioDeviceConnectionManager);
        this.proximitySensor = new ProximitySensor(context, event -> Queue.main.run(() -> {
            AudioManagerDelegate delegate = audioDeviceConnectionManager.getAudioManager();
            Ln.d("ProximitySensor.onEvent: " + event + ", " + delegate.isWiredHeadsetOn());
            if (!delegate.isWiredHeadsetOn()) {
                if (event == ProximitySensor.Listener.ProximityEvent.NEAR) {
                    speakerOnBeforeProximity = delegate.isSpeakerphoneOn();
                    if (speakerOnBeforeProximity) {
                        delegate.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
                        delegate.setSpeakerphoneOn(false);
                        audioDeviceConnectionManager.updateAudioVolume();
                    }
                    WmeSession session = engine.getSession();
                    if (session != null && session.getState() == WmeSession.State.CONNECTED) {
                        boolean muteStatus = session.isMutedByLocal(WmeTrack.Type.LocalVideo) || session.isMutedByRemote(WmeTrack.Type.LocalVideo);
                        Ln.d("ProximitySensor muteStatus: " + muteStatus);
                        if (!muteStatus) {
                            session.mute(WmeTrack.Type.LocalVideo);
                            videoMutedByProximity = true;
                            proximitySensor.disableScreen();
                        }
                    }
                } else if (event == ProximitySensor.Listener.ProximityEvent.FAR) {
                    if (speakerOnBeforeProximity) {
                        delegate.setMode(android.media.AudioManager.MODE_NORMAL);
                        delegate.setSpeakerphoneOn(speakerOnBeforeProximity);
                        audioDeviceConnectionManager.updateAudioVolume();
                        speakerOnBeforeProximity = false;
                    }
                    WmeSession session = engine.getSession();
                    if (session != null && session.getState() == WmeSession.State.CONNECTED
                            && session.isMutedByLocal(WmeTrack.Type.LocalVideo) && videoMutedByProximity) {
                        session.unmute(WmeTrack.Type.LocalVideo);
                        videoMutedByProximity = false;
                        proximitySensor.enableScreen();
                    }
                }
            }
        }));
    }

    public void register() {
        Ln.d("registerMediaDevice");
        audioDeviceConnectionManager.requestAudioFocus();
        try {
            audioDeviceConnectionManager.initialize();
            headsetIntentReceiver = new HeadsetIntentReceiver(audioDeviceConnectionManager);
            context.registerReceiver(headsetIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            proximitySensor.onResume();

            bluetoothServiceListener = new BluetoothServiceListener(audioDeviceConnectionManager);
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                bluetoothAdapter.getProfileProxy(context, bluetoothServiceListener, BluetoothProfile.HEADSET);
                bluetoothAdapter.getProfileProxy(context, bluetoothServiceListener, BluetoothProfile.A2DP);
                context.registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));
                context.registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
                context.registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(android.media.AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            }

            volumeSettingChangedListener = new VolumeSettingChangedListener(new Handler(), audioDeviceConnectionManager);
            context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeSettingChangedListener);
        } catch (Throwable t) {
            Ln.e(t);
        }
    }

    public void unregister() {
        Ln.d("unregisterMediaDevice");
        audioDeviceConnectionManager.abandonAudioFocus();
        proximitySensor.onPause();

        // unregister for headset notifications
        if (headsetIntentReceiver != null) {
            context.unregisterReceiver(headsetIntentReceiver);
            headsetIntentReceiver = null;
        }

        bluetoothServiceListener = null;
        try {
            context.unregisterReceiver(bluetoothBroadcastReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        if (volumeSettingChangedListener != null) {
            context.getContentResolver().unregisterContentObserver(volumeSettingChangedListener);
            volumeSettingChangedListener = null;
        }
        if (audioDeviceConnectionManager != null) {
            audioDeviceConnectionManager.uninitialize();
        }
    }

    public DeviceManager.MediaDevice getCamera(WMEngine.Camera camera) {
        return deviceManager.getCamera(camera.toDeviceCamera());
    }

    public AudioDeviceConnectionManager getAudioDeviceConnectionManager() {
        return audioDeviceConnectionManager;
    }
}
