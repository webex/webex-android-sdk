package com.cisco.spark.android.app;

import android.os.Handler;

import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

public class AndroidAudioManager implements AudioManager {

    public static final NaturalLog lnAudio = Ln.get("audio_hw_primary_spark");

    private android.media.AudioManager delegate;
    private boolean hasAudioFocus;

    public AndroidAudioManager(android.media.AudioManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setSpeakerphoneOn(boolean on) {
        lnAudio.i("AudioManager.setSpeakerphoneOn(%b)", on);
        delegate.setSpeakerphoneOn(on);
    }

    @Override
    public boolean isSpeakerphoneOn() {
        return delegate.isSpeakerphoneOn();
    }

    @Override
    public void setMode(int mode) {
        lnAudio.i("AudioManager.setMode(%d)", mode);
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
        lnAudio.i("AudioManager.isWiredHeadsetOn() = %b", isWiredHeadsetOn);
        return isWiredHeadsetOn;
    }

    @Override
    public boolean isBluetoothScoOn() {
        boolean isBluetoothScoOn = delegate.isBluetoothScoOn();
        lnAudio.i("AudioManager.isBluetoothScoOn() = %b", isBluetoothScoOn);
        return isBluetoothScoOn;
    }

    @Override
    public boolean isWiredHeadsetOrBluetoothConnected() {
        boolean isWiredHeadsetOrBluetoothConnected = isBluetoothScoOn() || isWiredHeadsetOn();
        lnAudio.i("AudioManager.isWiredHeadsetOrBluetoothConnected() = %b", isWiredHeadsetOrBluetoothConnected);
        return isWiredHeadsetOrBluetoothConnected;
    }

    @Override
    public boolean isBluetoothScoAvailableOffCall() {
        boolean isBluetoothScoAvailableOffCall = delegate.isBluetoothScoAvailableOffCall();
        lnAudio.i("AudioManager.isBluetoothScoAvailableOffCall() = %b", isBluetoothScoAvailableOffCall);
        return isBluetoothScoAvailableOffCall;
    }

    @Override
    public int getStreamVolume(int streamType) {
        int volume = delegate.getStreamVolume(streamType);
        lnAudio.i("AudioManager.getStreamVolume() type = %d, volume = %d", streamType, volume);
        return volume;
    }

    @Override
    public int getStreamMaxVolume(int streamType) {
        int maxVolume = delegate.getStreamMaxVolume(streamType);
        lnAudio.i("AudioManager.getStreamMaxVolume() type = %d, volume = %d", streamType, maxVolume);
        return maxVolume;
    }


    @Override
    public int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l, int streamType, int durationHint) {
        lnAudio.i("AudioManager.requestAudioFocus() streamType = %d", streamType);
        hasAudioFocus = true;
        return delegate.requestAudioFocus(l, streamType, durationHint);
    }

    @Override
    public int abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l) {
        lnAudio.i("AudioManager.AudioManager.abandonAudioFocus()");
        hasAudioFocus = false;
        return delegate.abandonAudioFocus(l);
    }

    @Override
    public boolean hasAudioFocus() {
        return hasAudioFocus;
    }

    @Override
    public void startBluetoothSco() {
        lnAudio.i("AudioManager.startBluetoothSco()");

        // sleep 1 second to wait BT device is completely initialized.
        new Handler().postDelayed(() -> {
            lnAudio.i("AudioManager.startBluetoothSco() executes after 1 second");
            delegate.startBluetoothSco();
        }, 1000);
    }

    @Override
    public void stopBluetoothSco() {
        lnAudio.i("AudioManager.stopBluetoothSco()");
        if (isBluetoothScoOn()) {
            delegate.setBluetoothScoOn(false);
            delegate.stopBluetoothSco();
        }
    }

    @Override
    public void setBluetoothScoAvailableOffCall(boolean bluetoothScoAvailableOffCall) {
        lnAudio.d("AudioManager.setBluetoothScoAvailableOffCall() Wrong call! This is for test use.");
    }
}
