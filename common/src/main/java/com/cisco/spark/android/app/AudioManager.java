package com.cisco.spark.android.app;


public interface AudioManager {

    void setMode(int mode);
    int getMode();
    void setSpeakerphoneOn(boolean on);
    boolean isSpeakerphoneOn();

    // Is any apps playing music
    boolean isMusicActive();

    boolean isWiredHeadsetOn();
    boolean isBluetoothScoOn();
    boolean isWiredHeadsetOrBluetoothConnected();
    int getStreamVolume(int streamType);
    int getStreamMaxVolume(int streamType);
    int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l, int streamType, int durationHint);
    int abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l);
    boolean hasAudioFocus();

    void setBluetoothScoAvailableOffCall(boolean bluetoothScoAvailableOffCall);
    boolean isBluetoothScoAvailableOffCall();
    void startBluetoothSco();
    void stopBluetoothSco();
}
