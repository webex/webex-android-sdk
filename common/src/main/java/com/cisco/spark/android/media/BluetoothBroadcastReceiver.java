package com.cisco.spark.android.media;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cisco.spark.android.app.AudioManager;
import com.github.benoitdion.ln.Ln;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private AudioManager audioManager;

    public BluetoothBroadcastReceiver(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Ln.d("BluetoothBroadcastReceiver.onReceive, action = " + action);
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            audioManager.setSpeakerphoneOn(false);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            audioManager.setSpeakerphoneOn(true);
        }
    }
}

