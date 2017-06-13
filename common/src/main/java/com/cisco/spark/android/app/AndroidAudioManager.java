package com.cisco.spark.android.app;

import com.github.benoitdion.ln.Ln;

public class AndroidAudioManager implements AudioManager {

    private android.media.AudioManager delegate;
    private boolean hasAudioFocus;


    public AndroidAudioManager(android.media.AudioManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setSpeakerphoneOn(boolean on) {
        Ln.i("AudioManager.setSpeakerphoneOn(%b)", on);
        delegate.setSpeakerphoneOn(on);
    }

    @Override
    public boolean isSpeakerphoneOn() {
        return delegate.isSpeakerphoneOn();
    }

    @Override
    public void setMode(int mode) {
        delegate.setMode(mode);
    }

    @Override
    public int getMode() {
        return delegate.getMode();
    }

    @Override
    public boolean isMusicActive() {
        return delegate.isMusicActive();
    }

    @Override
    public boolean isWiredHeadsetOn() {
        boolean isWiredHeadsetOn = delegate.isWiredHeadsetOn();
        Ln.i("AudioManager.isWiredHeadsetOn() = %b", isWiredHeadsetOn);
        return isWiredHeadsetOn;
    }

    @Override
    public void setBluetoothScoOn(boolean on) {
        delegate.setBluetoothScoOn(on);
    }

    @Override
    public boolean isBluetoothScoOn() {
        boolean isBluetoothScoOn = delegate.isBluetoothScoOn();
        Ln.i("AudioManager.isBluetoothScoOn() = %b", isBluetoothScoOn);
        return isBluetoothScoOn;
    }

    @Override
    public void setBluetoothA2dpOn(boolean on) {
        delegate.setBluetoothA2dpOn(on);
    }

    @Override
    public boolean isBluetoothA2dpOn() {
        boolean isBluetoothA2dpOn = delegate.isBluetoothA2dpOn();
        Ln.i("AudioManager.isBluetoothA2dpOn() = %b", isBluetoothA2dpOn);
        return isBluetoothA2dpOn;
    }


    @Override
    public boolean isWiredHeadsetOrBluetoothConnected() {
        return isBluetoothScoOn() || isBluetoothA2dpOn() || isWiredHeadsetOn();
    }

    @Override
    public int getStreamVolume(int streamType) {
        int volume = delegate.getStreamVolume(streamType);
        Ln.i("AudioManager.getStreamVolume() type = %d, volume = %d", streamType, volume);
        return volume;
    }

    @Override
    public int getStreamMaxVolume(int streamType) {
        int maxVolume = delegate.getStreamMaxVolume(streamType);
        Ln.i("AudioManager.getStreamMaxVolume() type = %d, volume = %d", streamType, maxVolume);
        return maxVolume;
    }


    @Override
    public int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l, int streamType, int durationHint) {
        Ln.i("AudioManager.requestAudioFocus()");
        hasAudioFocus = true;
        return delegate.requestAudioFocus(l, streamType, durationHint);
    }

    @Override
    public int abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l) {
        Ln.i("AudioManager.abandonAudioFocus()");
        hasAudioFocus = false;
        return delegate.abandonAudioFocus(l);
    }

    @Override
    public boolean hasAudioFocus() {
        return hasAudioFocus;
    }
}
