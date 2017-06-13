package com.cisco.spark.android.media;

import com.cisco.spark.android.app.AudioManager;
import android.bluetooth.BluetoothProfile;

import com.github.benoitdion.ln.Ln;


public class BluetoothServiceListener implements BluetoothProfile.ServiceListener {

    private AudioManager audioManager;

    public BluetoothServiceListener(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        Ln.i("BluetoothServiceListener.onServiceConnected");
        if (BluetoothProfile.HEADSET == profile || BluetoothProfile.A2DP == profile) {
            if (audioManager.isWiredHeadsetOrBluetoothConnected()) {
                audioManager.setSpeakerphoneOn(false);
            } else {
                audioManager.setSpeakerphoneOn(true);
            }
        }
    }

    public void onServiceDisconnected(int profile) {
        Ln.i("BluetoothServiceListener.onServiceDisconnected");
        if (BluetoothProfile.HEADSET == profile || BluetoothProfile.A2DP == profile) {
            if (audioManager.isWiredHeadsetOrBluetoothConnected()) {
                Ln.i("BluetoothAdapterListener, WiredHeadset is on set audio path to speakerphone false");
                audioManager.setSpeakerphoneOn(false);
            } else {
                Ln.i("BluetoothAdapterListener, WiredHeadset is off set audio path to speakerphone true");
                audioManager.setSpeakerphoneOn(true);
            }
        }
    }
}
