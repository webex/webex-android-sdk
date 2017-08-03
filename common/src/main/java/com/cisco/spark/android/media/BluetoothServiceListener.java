package com.cisco.spark.android.media;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;

import com.cisco.spark.android.app.AudioDeviceConnectionManager;

import static com.cisco.spark.android.app.AndroidAudioManager.lnAudio;

public class BluetoothServiceListener implements BluetoothProfile.ServiceListener {

    private AudioDeviceConnectionManager audioDeviceConnectionManager;

    public BluetoothServiceListener(AudioDeviceConnectionManager audioDeviceConnectionManager) {
        this.audioDeviceConnectionManager = audioDeviceConnectionManager;
    }

    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        lnAudio.i("BluetoothServiceListener.onServiceConnected, profile = " + (profile == BluetoothProfile.HEADSET ? "HEADSET" : "A2DP"));

        if (BluetoothProfile.HEADSET == profile) {
            audioDeviceConnectionManager.setBluetoothHeadset((BluetoothHeadset) proxy);
        } else if (BluetoothProfile.A2DP == profile) {
            audioDeviceConnectionManager.setBluetoothA2dp((BluetoothA2dp) proxy);
        }
    }

    public void onServiceDisconnected(int profile) {
        lnAudio.i("BluetoothServiceListener.onServiceDisconnected, profile = " + (profile == BluetoothProfile.HEADSET ? "HEADSET" : "A2DP"));

        if (BluetoothProfile.HEADSET == profile) {
            audioDeviceConnectionManager.setBluetoothHeadset(null);
        } else if (BluetoothProfile.A2DP == profile) {
            audioDeviceConnectionManager.setBluetoothA2dp(null);
        }
    }
}
