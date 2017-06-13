package com.cisco.spark.android.model;

public class AudioState {
    private boolean isMuted;
    private int volumeStep;
    private int maxVolumeLevel;
    private int minVolumeLevel;
    private int volumeLevel;
    private String roomId;

    public AudioState(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setIsMuted(boolean muted) {
        isMuted = muted;
    }

    public boolean getIsMuted() {
        return isMuted;
    }

    public int getVolumeLevel() {
        return volumeLevel;
    }

    public void setVolumeLevel(int volumeLevel) {
        this.volumeLevel = volumeLevel;
    }

    public int getMaxVolumeLevel() {
        return maxVolumeLevel;
    }

    public void setMaxVolumeLevel(int maxVolumeLevel) {
        this.maxVolumeLevel = maxVolumeLevel;
    }

    public int getVolumeStep() {

        return volumeStep;
    }

    public void setVolumeStep(int volumeStep) {
        this.volumeStep = volumeStep;
    }

    public int getMinVolumeLevel() {
        return minVolumeLevel;
    }

    public void setMinVolumeLevel(int minVolumeLevel) {
        this.minVolumeLevel = minVolumeLevel;
    }
}
