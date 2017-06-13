package com.cisco.spark.android.media;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.cisco.spark.android.core.SquaredBroadcastReceiver;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;

public class HeadsetIntentReceiver extends SquaredBroadcastReceiver {

    @Inject
    MediaEngine mediaEngine;

    @Inject
    AudioManager audioManager;

    private boolean headsetConnected = false;

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!isInitialized()) {
            return;
        }

        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            if (intent.hasExtra("state")) {
                if (intent.getIntExtra("state", 0) == 0) {
                    Ln.i("HeadsetIntentReceiver.onReceive(), headset plugged out");
                    headsetConnected = false;
                    if (mediaEngine != null) {
                        mediaEngine.headsetPluggedOut();
                    }
                    // set audio path to speakerphone when headset is plugged out
                    if (audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn()) {
                        Ln.i("HeadsetIntentReceiver.onReceive(), BluetoothSco is on set audio path to speakerphone false");
                        audioManager.setSpeakerphoneOn(false);
                    } else {
                        Ln.i("HeadsetIntentReceiver.onReceive(), BluetoothSco is off set audio path to speakerphone true");
                        audioManager.setSpeakerphoneOn(true);
                    }

                } else if (!headsetConnected && intent.getIntExtra("state", 0) == 1) {
                    Ln.i("HeadsetIntentReceiver.onReceive(), headset plugged in");
                    headsetConnected = true;

                    // disable speakerphone when headset is plugged in
                    audioManager.setSpeakerphoneOn(false);
                    if (mediaEngine != null) {
                        mediaEngine.headsetPluggedIn();
                    }
                }
            }
        }
    }
}
