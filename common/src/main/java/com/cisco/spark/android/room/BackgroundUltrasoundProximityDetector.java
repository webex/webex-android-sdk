package com.cisco.spark.android.room;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.cisco.spark.android.room.audiopairing.AudioPairingNative;
import com.cisco.spark.android.room.audiopairing.AudioSamplerAndroid;
import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;
import com.cisco.spark.android.sproximity.SProximityPairingCallback;
import com.cisco.spark.android.sproximity.SProximityPairingCallbackAdapter;
import com.github.benoitdion.ln.Ln;

import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

public class BackgroundUltrasoundProximityDetector implements ProximityDetector, AudioDataListener {

    private static final long RETRY_START_LISTENER_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private static final String TAG = "[SProximity]";

    private TokenListener tokenListener = NULL_LISTENER;
    private final HistoryToken historyToken = new HistoryToken();

    AudioSamplerAndroid audioSamplerAndroid;

    private static final TokenListener NULL_LISTENER = new TokenListener() {
        @Override
        public void newTokenWithCallback(String token, UltrasoundMetrics ultrasoundMetrics, boolean firstToken, SProximityPairingCallback callback) {
            Ln.w("found token but no one is listening, token: %s", token);
        }

        @Override
        public void newToken(String token, UltrasoundMetrics ultrasoundMetrics, boolean firstToken) {
            newTokenWithCallback(token, ultrasoundMetrics, firstToken, new SProximityPairingCallbackAdapter());
        }
    };
    private boolean isListening = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean usingWme = false;


    public BackgroundUltrasoundProximityDetector() {
    }

    @Override
    public void startListening(TokenListener listener, boolean switchFromWme) {

        Ln.i("%s start Listening.  isListening ? %s", TAG, isListening);
        if (isListening)
            return;

        this.tokenListener = listener;
        isListening = true;
        // kick off thread to capture audio and check for ultrasound pairing token from TP unit

        audioSamplerAndroid = new AudioSamplerAndroid();
        Ln.i("%s creating new AudioSampler [ %08x ]", TAG, audioSamplerAndroid.hashCode());

        Ln.i("%s audioSampler, starting [ %08x ]", TAG, audioSamplerAndroid.hashCode());

        audioSamplerAndroid.start(BackgroundUltrasoundProximityDetector.this);

    }

    /**
     * Starts the audiosampler in delayMillis millis
     * @param isPaired set when we are tracking the metrics for successive tokens when paired
     */
    public void startListenerDelayed(TokenListener listener, long delayMillis, final boolean isPaired) {
        this.tokenListener = listener;
        isListening = true;
        // kick off thread to capture audio and check for ultrasound pairing token from TP unit

        audioSamplerAndroid = new AudioSamplerAndroid();
        Ln.i("%s creating new AudioSampler [ %08x ]", TAG, audioSamplerAndroid.hashCode());

        handler.removeCallbacksAndMessages(null);
        Ln.i("%s start listening in %s secs", TAG, TimeUnit.MILLISECONDS.toSeconds(delayMillis));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Ln.i("%s audioSampler, now starting [ %08x ]", TAG, audioSamplerAndroid.hashCode());
                audioSamplerAndroid.start(BackgroundUltrasoundProximityDetector.this);
            }
        }, delayMillis);
    }

    @Override
    public boolean isListening() {
        return isListening;
    }

    @Override
    public void resetState() {
        Ln.v("%S ProximityDetector: clear history token", TAG);
        historyToken.clear();
    }

    @Override
    public void pauseListeningFor(TokenListener listener, int secondsToNextTokenEmit) {
        // ignored
    }

    @Override
    public void mediaEngineAudioTrackReady() {
    }

    @Override
    public void stopListening(TokenListener listener, boolean switchToWme) {
        Ln.i("%s background stopListeningAsync. (isListening = %s usingWme = %s switchToWme = %s)", TAG, isListening, usingWme, switchToWme);
        handler.removeCallbacksAndMessages(null);

        if (audioSamplerAndroid != null) {
            Ln.i("%s stopListening stop() [Start]", TAG);
            audioSamplerAndroid.stop();
            Ln.i("%s stopListening stop() [DONE]", TAG);
        }
        isListening = false;

    }

    /**
     * The samples may come from WME or our own AudioSamplerAndroid.
     * @param samples
     */
    @Override
    public void audioDataAvailable(FloatBuffer samples) {
        UltrasoundMetrics ultrasoundMetrics = decodeToken(samples);
        String newToken = ultrasoundMetrics.token;


        boolean firstToken = !historyToken.hasToken();
        if (historyToken.isNewTokenThenSet(newToken)) {
            Ln.i("%s audioDataAvailable, detected a new token%s [ %s ]", TAG, (firstToken ? " initial" : ""), getSource());
            tokenListener.newToken(newToken, ultrasoundMetrics, firstToken);
        } else {
            if (!TextUtils.isEmpty(newToken)) {
                // Shows us that we are getting tokens
                Ln.i("%s audioDataAvailable, detected token [ %s ]", TAG, getSource());
            }
        }
    }

    private String getSource() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        StackTraceElement stackTraceElement = stackTrace[2];
        return stackTraceElement.getClassName();
    }

    @Override
    public void onFailure() {
        Ln.i("%s, onFailure (currently ignored)", TAG);
    }

    private UltrasoundMetrics decodeToken(FloatBuffer samples) {
        UltrasoundMetrics ultrasoundMetrics = null;
        try {
            ultrasoundMetrics = AudioPairingNative.checkForToken(samples);
        } catch (Exception ex) {
            Ln.e(ex, "%s Unable to decode token from sample", TAG);
        }
        return ultrasoundMetrics;
    }

    @Override
    public void stopListeningBlocking(TokenListener tokenListener, boolean switchToWme) {
        Ln.i("%s BackgroundDetector, stopListeningBlocking (isListening = %s usingWme = %s switchToWme = %s)", TAG, isListening, usingWme, switchToWme);
        handler.removeCallbacksAndMessages(null);
        if (audioSamplerAndroid != null) {
            Ln.i("%s BackgroundDetector, stopListeningBlocking, stopImmediateBlocking() [Start]", TAG);
            audioSamplerAndroid.stopImmediateBlocking();
            Ln.i("%s BackgroundDetector, stopListeningBlocking, stopImmediateBlocking() [DONE]", TAG);
        }
        isListening = false;
    }

    class HistoryToken {

        private String previousToken = null;

        public synchronized boolean isNewTokenThenSet(String newToken) {
            boolean isToken = !TextUtils.isEmpty(newToken);
            boolean isNew = (isToken && !newToken.equals(previousToken));
            if (isNew) {
                previousToken = newToken;
            }
            return isNew;
        }

        public synchronized boolean hasToken() {
            return previousToken != null;
        }

        public synchronized void clear() {
            previousToken = null;
        }
    }
}
