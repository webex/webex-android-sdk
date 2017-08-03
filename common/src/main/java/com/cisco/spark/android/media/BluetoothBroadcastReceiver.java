package com.cisco.spark.android.media;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.cisco.spark.android.app.AudioDeviceConnectionManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.cisco.spark.android.app.AndroidAudioManager.lnAudio;

@Singleton
public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    @Inject
    AudioDeviceConnectionManager audioDeviceConnectionManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int extraState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            lnAudio.i("BluetoothBroadcastReceiver, action: " + action + "; extraState: " + extraState);
            if (BluetoothHeadset.STATE_CONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothDeviceConnected();
            } else if (BluetoothHeadset.STATE_DISCONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothDeviceDisconnected();
            }
        } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int extraState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
            lnAudio.i("BluetoothBroadcastReceiver, action: " + action + "; extraState: " + extraState);
            if (BluetoothA2dp.STATE_CONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothDeviceConnected();
            } else if (BluetoothA2dp.STATE_DISCONNECTED == extraState) {
                audioDeviceConnectionManager.onBluetoothDeviceDisconnected();
            }
        } else if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
            int scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
            lnAudio.i("BluetoothBroadcastReceiver, sco state: " + scoState);
        }
    }
}

