package com.cisco.spark.android.room;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.cisco.spark.android.app.ActivityManager;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.cisco.spark.android.room.audiopairing.AudioPairingNative;
import com.cisco.spark.android.room.audiopairing.AudioSamplerAndroid;
import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;
import com.cisco.spark.android.sproximity.SProximityPairingCallback;
import com.cisco.spark.android.sproximity.SProximityPairingCallbackAdapter;
import com.github.benoitdion.ln.Ln;

import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.room.AverageUltrasonicMetrics.*;

public class UltrasoundProximityDetector implements ProximityDetector, AudioDataListener {

    // Increased from 0 again, we need to
    private static final long START_LISTENER_DELAY_MILLIS = 150;
    private static final long RETRY_START_LISTENER_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private MediaEngine mediaEngine;
    private final ActivityManager activityManager;
    private final EventBus eventBus;

    private TokenListener tokenListener = NULL_LISTENER;

    // Maintain two different histories to avoid
    private final HistoryToken historySparkToken = new HistoryToken();
    private final HistoryToken historyAltoToken = new HistoryToken();

    private static final Object audioSamplerLock = new Object();
    private AudioSamplerAndroid audioSamplerAndroid;

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

    private long startListeningForNextPairedTokenTiming;
    private boolean isListeningForNextPairedToken = false;

    private AverageUltrasonicMetrics averageMetrics = new AverageUltrasonicMetrics();
    private volatile boolean isInUltrasonicRange;

    public UltrasoundProximityDetector(MediaEngine mediaEngine, ActivityManager activityManager, EventBus eventBus) {
        this.mediaEngine = mediaEngine;
        this.activityManager = activityManager;
        this.eventBus = eventBus;
    }

    @Override
    public void startListening(TokenListener listener, boolean switchFromWme) {
        if (switchFromWme) {
            Ln.i("RoomService, startListening, switching from media engine to audio sampler");
            Ln.d("startListening, clearAudioDataListener: %s", (AudioDataListener) this);
            mediaEngine.clearAudioDataListener(this);
            usingWme = false;
        }
        if (usingWme) { // if wme is active don't grab microphone
            Ln.i("startListening, (currently using media engine, ignore)");
            return;
        }
        if (isListening) {
            Ln.i("startListening, (already listening, ignore)");
            return;
        }

        boolean inForeground = activityManager.isMostRecentTask();

        if (inForeground) {
            Ln.i("RoomService, app in foreground (most recent task), use audioSampler for sampling (isListening=%s)", isListening());
            stopListening(tokenListener, false);
            startListenerDelayed(listener, START_LISTENER_DELAY_MILLIS, false);
        } else {
            Ln.i("RoomService, app not in foreground, ignore sampling request");
        }
    }

    /**
     * Starts the audiosampler in delayMillis millis
     * @param isPaired set when we are tracking the metrics for successive tokens when paired
     */
    private void startListenerDelayed(TokenListener listener, long delayMillis, final boolean isPaired) {
        this.tokenListener = listener;
        isListening = true;
        // kick off thread to capture audio and check for ultrasound pairing token from TP unit

        final int audioSamplerReference;
        synchronized (audioSamplerLock) {
            audioSamplerAndroid = new AudioSamplerAndroid();
            audioSamplerReference = audioSamplerAndroid.hashCode();
        }
        Ln.i("RoomService, creating new AudioSampler [ %08x ]", audioSamplerReference);
        final Thread audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (audioSamplerLock) {
                    // Avoid null pointer by locking the access to the audioSampler, this might not be
                    // sufficient as with the current layout of things we cannot call start on the
                    // sampler in the synchronized region.
                    // TODO: Refactor this flow such that we can guarantee not starting or stopping
                    // the sampler when it is null.
                    if (audioSamplerAndroid != null) {
                        Ln.v("AltoMetric, post start listening");
                        eventBus.post(new AltoMetricsPairingEventStartedListening());
                    }
                }
                if (audioSamplerAndroid != null) {
                    audioSamplerAndroid.start(UltrasoundProximityDetector.this);
                } else {
                    Ln.w("RoomService, audioSampler was null when attempting to start recording");
                }
            }
        }, String.format("audioSampler-%08x", audioSamplerReference));
        Ln.i("RoomService, request audioSampler to start in %d ms", delayMillis);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (audioSamplerLock) {
                    Ln.i("RoomService, audioSampler, now starting [ %08x ]", audioSamplerAndroid != null ? audioSamplerAndroid.hashCode() : 0);
                }
                if (isPaired) { // Successive tokens when already paired
                    Ln.d("RoomService, metrics - register start listening when paired");
                    startListeningForNextPairedTokenTiming = SystemClock.uptimeMillis();
                    isListeningForNextPairedToken = true;
                }
                audioThread.start();
                isInUltrasonicRange = false;
            }
        }, delayMillis);
    }

    @Override
    public boolean isListening() {
        return isListening;
    }

    @Override
    public void resetState() {
        Ln.v("RoomService, ProximityDetector: clear history token");
        historySparkToken.clear();
    }

    @Override
    public void pauseListeningFor(TokenListener listener, int secondsToNextTokenEmit) {
        if (usingWme) {
            Ln.i("RoomService, ignore pause as WME is providing audio");
            return;
        }

        // Pause for minimum amount of time, we do not have to pick up the new token right away, if
        // we picked up the last token at the end of its "life" it is still valid for about 70 seconds
        long pauseTime = Math.max(RETRY_START_LISTENER_DELAY_MILLIS, (secondsToNextTokenEmit * 1000));
        Ln.i("RoomService, pause listening for %d secs", pauseTime);

        stopListening(listener, false);
        startListenerDelayed(listener, pauseTime, true);

    }

    @Override
    public void mediaEngineAudioTrackReady() {
        Ln.i("mediaEngineAudioTrackReady, using wme = %s", usingWme);
        if (usingWme) {
            Ln.i("mediaEngineAudioTrackReady, set audio data listener");
            mediaEngine.setAudioDataListener(this);
        }
    }

    @Override
    public void stopListening(TokenListener listener, boolean switchToWme) {
        Ln.i("RoomService, stopListening.  (isListening = %s usingWme = %s switchToWme = %s)", isListening, usingWme, switchToWme);
        if (usingWme) {
            return;
        }
        isListening = false;
        this.tokenListener = NULL_LISTENER;
        handler.removeCallbacksAndMessages(null);
        synchronized (audioSamplerLock) {
            if (audioSamplerAndroid != null) {
                Ln.i("RoomService, stopping audio sampler [ %08x ]", audioSamplerAndroid.hashCode());
                audioSamplerAndroid.stop();
                audioSamplerAndroid = null;
            } else {
                if (!switchToWme) {
                    Ln.i("RoomService, stopping audio sampler, currently no running audio sampler");
                }
            }
        }
        if (switchToWme) {
            Ln.i("RoomService, switching to use media engine for sampling");
            usingWme = true;
            // We cannot set the audioDataListener on media engine yet, as the audioTrack is not ready
            // yet.
            this.tokenListener = listener;
        }
    }

    //TODO: Avoid duplication between stopListening and stopListeningBlocking
    @Override
    public void stopListeningBlocking(TokenListener tokenListener, boolean switchToWme) {
        final String tag = "UltrasoundProximityDetector, stopListeningBlocking";

        Ln.i("%s, (isListening = %s usingWme = %s switchToWme = %s)", tag, isListening, usingWme, switchToWme);

        if (usingWme) {
            return;
        }
        isListening = false;
        this.tokenListener = NULL_LISTENER;
        handler.removeCallbacksAndMessages(null);
        synchronized (audioSamplerLock) {
            if (audioSamplerAndroid != null) {
                Ln.i("%s, stopImmediateBlocking() [Start]", tag);
                audioSamplerAndroid.stopImmediateBlocking();
                // This seems critical to avoid trying to stop an already stopped sampler later (when handing over back from wme to us)
                audioSamplerAndroid = null;
                Ln.i("%s, stopImmediateBlocking() [DONE]", tag);
            }
        }
        if (switchToWme) {
            Ln.i("%s, switching to use media engine for sampling", tag);
            usingWme = true;
            mediaEngine.setAudioDataListener(this);
            this.tokenListener = tokenListener;
        }
    }

    /**
     * The samples may come from WME or our own AudioSamplerAndroid.
     */
    @Override
    public void audioDataAvailable(FloatBuffer samples) {
        UltrasoundMetrics ultrasoundMetrics = decodeToken(samples);
        String newToken = ultrasoundMetrics.token;

        detectUltrasonicRange(ultrasoundMetrics);

        if (ultrasoundMetrics.isAltoToken()) {
            if (!TextUtils.isEmpty(newToken) && historyAltoToken.isNewTokenThenSet(newToken)) {
                    if (newToken.length() == 12) {
                        String host = ProximityUtils.extractSystem(newToken.substring(0, 8));
                        Ln.i("RoomService, detected new alto token %s (%s) [ %s ]", newToken, host, getSource());
                    } else {
                        Ln.i("RoomService, detected new alto token %s [ %s ]", newToken, getSource());
                    }
            }
        } else { // We know this is a spark token
            // Only triggered when we are listening for a second token after being paired
            if (!TextUtils.isEmpty(newToken) && isListeningForNextPairedToken) {
                long millisFromListenToFound = SystemClock.uptimeMillis() - startListeningForNextPairedTokenTiming;
                isListeningForNextPairedToken = false;
                Ln.d("RoomService, AltoMetric - found next token in %d millis", millisFromListenToFound);
                eventBus.post(new AltoMetricsTokenRefreshedEvent(millisFromListenToFound));
            }
            boolean firstToken = !historySparkToken.hasToken();
            if (historySparkToken.isNewTokenThenSet(newToken)) {
                Ln.i("RoomService, detected a new spark token%s [ %s ]", (firstToken ? " initial" : ""), getSource());
                Ln.v("AltoMetric, post found token");
                eventBus.post(new AltoMetricsPairingEventFoundNewToken());
                tokenListener.newToken(newToken, ultrasoundMetrics, firstToken);
            } else {
                if (!TextUtils.isEmpty(newToken)) {
                    // Shows us that we are getting tokens
                    Ln.i("RoomService, detected spark token [ %s ]", getSource());
                }
            }
        }
    }

    private String getSource() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        StackTraceElement stackTraceElement = stackTrace[2];
        return stackTraceElement.getClassName();
    }

    // We are trying to detect whether we are in range of an ultrasonic sound source
    private void detectUltrasonicRange(UltrasoundMetrics ultrasoundMetrics) {
        averageMetrics.add(ultrasoundMetrics);
        AverageResult averageResult = averageMetrics.process();
        if (averageResult != AverageResult.NOT_PROCESSED) {
            boolean inRange = (averageResult == AverageResult.IN_RANGE);
            if (isInUltrasonicRange != inRange) {
                // State changed
                if (inRange) {
                    Ln.v("AltoMetric, post entered ultrasonic range");
                    eventBus.post(new AltoMetricsEnteredUltrasonicRange());
                } else {
                    Ln.v("AltoMetric, post left ultrasonic range");
                    eventBus.post(new AltoMetricsLeftUltrasonicRange());
                }
            }
            isInUltrasonicRange = inRange;
        }
    }

    @Override
    public void onFailure() {
        // If we fail to capture the microphone. stopListening for a while, and try again.
        Ln.i("RoomService, failed grabbing microphone. try again later");
        stopListening(tokenListener, false);
        startListenerDelayed(tokenListener, RETRY_START_LISTENER_DELAY_MILLIS, false);
    }

    private UltrasoundMetrics decodeToken(FloatBuffer samples) {
        UltrasoundMetrics ultrasoundMetrics = null;
        try {
            ultrasoundMetrics = AudioPairingNative.checkForToken(samples);
        } catch (Exception ex) {
            Ln.e(ex, "Unable to decode token from sample");
        }
        return ultrasoundMetrics;
    }

    private class HistoryToken {

        private String previousToken = null;

        synchronized boolean isNewTokenThenSet(String newToken) {
            boolean isToken = !TextUtils.isEmpty(newToken);
            boolean isNew = (isToken && !newToken.equals(previousToken));
            if (isNew) {
                previousToken = newToken;
            }
            return isNew;
        }

        synchronized boolean hasToken() {
            return previousToken != null;
        }

        public synchronized void clear() {
            previousToken = null;
        }
    }
}
