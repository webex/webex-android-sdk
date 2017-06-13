package com.cisco.spark.android.room.model;

import android.net.Uri;

import com.cisco.spark.android.lyra.AudioMicrophones;
import com.cisco.spark.android.lyra.AudioVolume;

public final class UpdateAudioStateRequest {

    private final Uri deviceUrl;
    private final AudioMicrophones microphones;
    private final AudioVolume volume;


    public UpdateAudioStateRequest(Uri deviceUrl,
                                   AudioMicrophones microphones,
                                   AudioVolume volume) {
        this.deviceUrl = deviceUrl;
        this.microphones = microphones;
        this.volume = volume;
    }

    public AudioMicrophones getMicrophones() {
        return microphones;
    }

    public AudioVolume getVolume() {
        return volume;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

}
