package com.cisco.spark.android.media;

import android.content.Context;
import android.content.Intent;

import com.cisco.spark.android.app.AudioDeviceConnectionManager;
import com.cisco.spark.android.core.SquaredBroadcastReceiver;

import javax.inject.Inject;

import static com.cisco.spark.android.app.AndroidAudioManager.lnAudio;

public class HeadsetIntentReceiver extends SquaredBroadcastReceiver {

    @Inject
    AudioDeviceConnectionManager audioDeviceConnectionManager;

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!isInitialized()) {
            return;
        }

        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            if (intent.hasExtra("state")) {
                int state = intent.getIntExtra("state", 0);
                if (state == 0) {
                    lnAudio.i("HeadsetIntentReceiver.onReceive(), headset plugged out");
                    audioDeviceConnectionManager.onWiredHeadsetStateUpdated(false);
                } else if (state == 1) {
                    lnAudio.i("HeadsetIntentReceiver.onReceive(), headset plugged in");
                    audioDeviceConnectionManager.onWiredHeadsetStateUpdated(true);
                }
            }
        }
    }
}
