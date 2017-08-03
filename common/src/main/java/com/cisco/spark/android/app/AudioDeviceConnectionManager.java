package com.cisco.spark.android.app;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;

import com.cisco.spark.android.media.MediaEngine;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.cisco.spark.android.app.AndroidAudioManager.lnAudio;

@Singleton
public class AudioDeviceConnectionManager {

    protected static final int CONNECTION_STATUS_NONE = 0;
    protected static final int CONNECTION_STATUS_WIRED_HEADSET = 1;
    protected static final int CONNECTION_STATUS_BLUETOOTH = 2;
    private static final int CONNECTION_STATUS_INITIALIZE = -1;

    @Inject
    protected AudioManager audioManager;

    @Inject
    MediaEngine mediaEngine;

    protected int status = CONNECTION_STATUS_NONE;
    private int previousStatus = CONNECTION_STATUS_INITIALIZE;

    private BluetoothHeadset bluetoothHeadset = null;
    private BluetoothA2dp bluetoothA2dp = null;

    public void initialize() {
        status = CONNECTION_STATUS_NONE;
        previousStatus = CONNECTION_STATUS_INITIALIZE;
        bluetoothHeadset = null;
        bluetoothA2dp = null;
    }

    public void clear() {
        status = CONNECTION_STATUS_NONE;
        previousStatus = CONNECTION_STATUS_INITIALIZE;
        closeBluetoothProfile();
    }

    private void closeBluetoothProfile() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
        }
        bluetoothHeadset = null;
        bluetoothA2dp = null;
    }

    public boolean isBTHeadsetConnected() {
        if (bluetoothHeadset == null) {
            return false;
        }

        List<BluetoothDevice> connectedDevices = bluetoothHeadset.getConnectedDevices();
        return connectedDevices != null
                && !connectedDevices.isEmpty();
    }

    public boolean isBTA2dpConnected() {
        if (bluetoothA2dp == null) {
            return false;
        }

        List<BluetoothDevice> connectedDevices = bluetoothA2dp.getConnectedDevices();
        return connectedDevices != null
                && !connectedDevices.isEmpty();
    }

    public void setBluetoothHeadset(BluetoothHeadset bluetoothHeadset) {
        this.bluetoothHeadset = bluetoothHeadset;
        doUpdate();
    }

    public void setBluetoothA2dp(BluetoothA2dp bluetoothA2dp) {
        this.bluetoothA2dp = bluetoothA2dp;
        doUpdate();
    }

    public void onWiredHeadsetStateUpdated(boolean connected) {
        if (mediaEngine != null) {
            if (connected) {
                mediaEngine.headsetPluggedIn();
            } else {
                mediaEngine.headsetPluggedOut();
            }
        }
        doUpdate();
    }

    public void onBluetoothDeviceConnected() {
        onBluetoothStateUpdated();
    }

    public void onBluetoothDeviceDisconnected() {
        onBluetoothStateUpdated();
    }

    protected void doUpdate() {
        updateStatus();
        if (isStatusChange()) {
            previousStatus = status;

            // refer System phone call app to determine the priority of output device: wired_headset > bluetooth_headset > speakerphone
            if (CONNECTION_STATUS_WIRED_HEADSET == status) {
                playThroughWiredHeadset();
            } else if (CONNECTION_STATUS_BLUETOOTH == status) {
                playThroughBluetooth();
            } else {
                playThroughSpeakerPhone();
            }
        }
    }

    protected void updateStatus() {
        if (audioManager.isWiredHeadsetOn()) {
            status = CONNECTION_STATUS_WIRED_HEADSET;
        } else if (audioManager.isBluetoothScoAvailableOffCall() && (isBTHeadsetConnected() || isBTA2dpConnected())) {
            status = CONNECTION_STATUS_BLUETOOTH;
        } else {
            status = CONNECTION_STATUS_NONE;
        }
        lnAudio.d("status: " + status + "; previousStatus: " + previousStatus);
    }

    private void onBluetoothStateUpdated() {
        doUpdate();
    }

    private boolean isStatusChange() {
        return status != previousStatus;
    }

    private void playThroughSpeakerPhone() {
        lnAudio.i("playThroughSpeakerPhone");
        audioManager.stopBluetoothSco();
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
    }

    private void playThroughWiredHeadset() {
        lnAudio.i("playThroughWiredHeadset");
        audioManager.stopBluetoothSco();
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
    }

    private void playThroughBluetooth() {
        lnAudio.i("playThroughBluetooth");
        audioManager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
        audioManager.startBluetoothSco();
    }

}
