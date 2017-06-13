package com.cisco.spark.android.media;

import android.database.ContentObserver;
import android.os.Handler;

import com.cisco.spark.android.app.AudioManager;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.github.benoitdion.ln.Ln;


public class VolumeSettingChangedListener extends ContentObserver {

    private int currVoiceVolume = 0;
    private int currSysVolume = 0;
    private AudioManager audioManager;
    private CallControlService callControlService;

    public VolumeSettingChangedListener(Handler handler, CallControlService callControlService, AudioManager audioManager) {
        super(handler);
        this.audioManager = audioManager;
        this.callControlService = callControlService;
        currVoiceVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL);
        currSysVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_SYSTEM);
    }
    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        //will change later, decide by the device type, Google serials is STREAM_SYSTEM, Samsung S3,S4 is STREAM_VOICE_CALL
        int voiceVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL);
        int systemVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_SYSTEM);
        int maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_VOICE_CALL);
        int vol = voiceVol;
        if (voiceVol != currVoiceVolume) {
            currVoiceVolume = voiceVol;
        } else if (systemVol != currSysVolume) {
            maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_SYSTEM);
            vol = systemVol;
            currSysVolume = vol;
        } else {
            return;
        }
        int volSetting = 65535 >> (maxVol - vol);
        if (0 == vol) {
            volSetting = 0;
        }
        Ln.d("VolumeSettingChangedListener,onChange volume setting:" + volSetting + ",max volume:" + maxVol + ",current volume:" + vol);
        callControlService.setAudioVolume(volSetting);
    }
}
