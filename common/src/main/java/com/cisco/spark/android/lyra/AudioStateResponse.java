package com.cisco.spark.android.lyra;

public class AudioStateResponse {

    private String url;

    private AudioMicrophones microphones;

    private AudioVolume volume;

    public String getUrl() {
        return url;
    }

    public AudioMicrophones getMicrophones() {
        return microphones;
    }

    public AudioVolume getVolume() {
        return volume;
    }
}
