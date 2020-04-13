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

import android.bluetooth.*;
import android.content.Context;
import android.media.AudioManager;
import com.ciscowebex.androidsdk.internal.media.*;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.MediaSessionAPI;

import java.util.List;

public class AudioDeviceConnectionManager {

    enum ConnectionStatus {
        INITIALIZE, NONE, WIRED_HEADSET, BLUETOOTH
    }

    enum DevicePref {
        SPEAKER, EARPIECE
    }

    public static final int STREAM_BLUETOOTH_SCO = 6;

    protected AudioManagerDelegate audioManager;
    protected WMEngine mediaEngine;

    private ConnectionStatus status = ConnectionStatus.NONE;
    private ConnectionStatus previousStatus = ConnectionStatus.INITIALIZE;
    private DevicePref devicePreference = DevicePref.SPEAKER;

    private BluetoothHeadset bluetoothHeadset = null;
    private BluetoothA2dp bluetoothA2dp = null;

    public AudioDeviceConnectionManager(Context context, WMEngine engine) {
        this.audioManager = new AudioManagerDelegate(context);
        this.mediaEngine = engine;
    }

    public void initialize() {
        status = ConnectionStatus.NONE;
        previousStatus = ConnectionStatus.INITIALIZE;
        bluetoothHeadset = null;
        bluetoothA2dp = null;
    }

    public void uninitialize() {
        status = ConnectionStatus.NONE;
        previousStatus = ConnectionStatus.INITIALIZE;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
        }
        bluetoothHeadset = null;
        bluetoothA2dp = null;
    }

    public AudioManagerDelegate getAudioManager() {
        return audioManager;
    }

    public void requestAudioFocus() {
        WmeSession session = mediaEngine.getSession();
        boolean audioOnly = session != null && !session.getCapability().hasVideo() && !session.getCapability().hasSharing();
        try {
            if (session != null && session.getCapability().hasAudio()) {
                if (!audioManager.isWiredHeadsetOrBluetoothConnected()) {
                    if (audioOnly) {
                        Ln.d("requestAudioFocus: play through earpiece");
                        devicePreference = DevicePref.EARPIECE;
                        playThroughEarpiece();
                    } else {
                        Ln.d("requestAudioFocus: play through speaker");
                        devicePreference = DevicePref.SPEAKER;
                        playThroughSpeakerPhone();
                    }
                }
            }
            if (isAudioEnhancement()) {
                MediaSessionAPI.enableAudioEnhancement(true);
            }
            int playbackStreamType = session == null ? AudioManager.STREAM_VOICE_CALL : session.getAudioPlaybackStreamType().getValue();
            Ln.i("requestAudioFocus playbackStreamType(%s)", playbackStreamType);
            audioManager.requestAudioFocus(null, playbackStreamType, android.media.AudioManager.AUDIOFOCUS_GAIN);
        } catch (Throwable t) {
            Ln.e(t);
        }
    }

    public void abandonAudioFocus() {
        if (audioManager.isBluetoothScoOn()) {
            audioManager.stopBluetoothSco();
        }
        audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
        audioManager.abandonAudioFocus(null);
    }

    void setBluetoothHeadset(BluetoothHeadset bluetoothHeadset) {
        this.bluetoothHeadset = bluetoothHeadset;
        doUpdate();
    }

    void setBluetoothA2dp(BluetoothA2dp bluetoothA2dp) {
        this.bluetoothA2dp = bluetoothA2dp;
        doUpdate();
    }

    void onWiredHeadsetStateUpdated(boolean connected) {
        WmeSession session = mediaEngine == null ? null : mediaEngine.getSession();
        if (session != null) {
            if (connected) {
                session.headsetPluggedIn();
            }
            else {
                session.headsetPluggedOut();
            }
        }
        doUpdate();
    }

    void onBluetoothStateUpdated() {
        doUpdate();
    }

    private void doUpdate() {
        if (audioManager.isWiredHeadsetOn()) {
            status = ConnectionStatus.WIRED_HEADSET;
        }
        else if (audioManager.isBluetoothScoAvailableOffCall() && (isBTHeadsetConnected() || isBTA2dpConnected())) {
            status = ConnectionStatus.BLUETOOTH;
        }
        else {
            status = ConnectionStatus.NONE;
        }
        Ln.d("status: " + status + "; previousStatus: " + previousStatus + "; devicePreference: " + devicePreference);
        if (status != previousStatus) {
            previousStatus = status;
            if (status == ConnectionStatus.WIRED_HEADSET) {
                Ln.d("playThroughWiredHeadset");
                audioManager.stopBluetoothSco();
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(isAudioEnhancement() ? android.media.AudioManager.MODE_IN_COMMUNICATION : android.media.AudioManager.MODE_NORMAL);
            }
            else if (status == ConnectionStatus.BLUETOOTH) {
                Ln.d("playThroughBluetooth");
                audioManager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(false);
                audioManager.startBluetoothSco();
            }
            else {
                if (devicePreference == DevicePref.EARPIECE) {
                    playThroughEarpiece();
                }
                else {
                    playThroughSpeakerPhone();
                }
            }
            updateAudioVolume();
        }
    }

    public void updateAudioVolume() {
        int streamType;
        if (audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            if (audioManager.isBluetoothScoOn()) {
                streamType = STREAM_BLUETOOTH_SCO;
            } else {
                streamType = AudioManager.STREAM_VOICE_CALL;
            }
        } else {
            streamType = AudioManager.STREAM_MUSIC;
        }

        int currentVol = audioManager.getStreamVolume(streamType);
        int maxVol = audioManager.getStreamMaxVolume(streamType);
        int volSetting = (65535 * currentVol) / maxVol;
        Ln.d("updateAudioVolume: streamType=%s; set volume to=%s(%s/%s)", streamType, volSetting, currentVol, maxVol);
        WmeSession session = mediaEngine == null ? null : mediaEngine.getSession();
        if (session != null) {
            session.setAudioVolume(volSetting);
        }
    }

    private boolean isBTHeadsetConnected() {
        if (bluetoothHeadset == null) {
            return false;
        }
        List<BluetoothDevice> connectedDevices = bluetoothHeadset.getConnectedDevices();
        return connectedDevices != null && !connectedDevices.isEmpty();
    }

    public boolean isBTA2dpConnected() {
        if (bluetoothA2dp == null) {
            return false;
        }
        List<BluetoothDevice> connectedDevices = bluetoothA2dp.getConnectedDevices();
        return connectedDevices != null && !connectedDevices.isEmpty();
    }

    private boolean isAudioEnhancement() {
        WmeSession session = mediaEngine == null ? null : mediaEngine.getSession();
        if (session != null) {
            MediaCapability capability = session.getCapability();
            return capability != null && capability.isAudioEnhancement();
        }
        return false;
    }

    private void playThroughSpeakerPhone() {
        Ln.d("playThroughSpeakerPhone");
        audioManager.stopBluetoothSco();
        audioManager.setSpeakerphoneOn(true);
        int mode = isAudioEnhancement() ? android.media.AudioManager.MODE_IN_COMMUNICATION : android.media.AudioManager.MODE_NORMAL;
        audioManager.setMode(mode);
    }

    private void playThroughEarpiece() {
        Ln.d("playThroughEarpiece");
        audioManager.stopBluetoothSco();
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
    }


}
