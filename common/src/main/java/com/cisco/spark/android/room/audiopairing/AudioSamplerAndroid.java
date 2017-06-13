package com.cisco.spark.android.room.audiopairing;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.ConditionVariable;

import com.github.benoitdion.ln.Ln;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AudioSamplerAndroid {

    public static final int DEFAULT_SAMPLE_RATE = 48000;

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final long STOP_BLOCKING_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);

    // Used to exit the recording loop
    private volatile boolean stopped;

    // Used to know if we have a running sampler
    private volatile boolean started;

    private AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private int currentSessionId = -1; // Not set yet
    private ConditionVariable condition;
    private AudioRecord recorder;


    public void start(AudioDataListener listener) {
        stopped = false;
        started = true;
        recorder = null;
        shuttingDown.set(false);

        try {
            final int bufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            recorder = new AudioRecord(AUDIO_SOURCE, DEFAULT_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            Ln.i(formatLogMessage("start recording", recorder));
            recorder.startRecording();

            setSessionId(recorder);

            final short[] shorts = new short[bufferSize / 2];

            ByteBuffer bb = ByteBuffer.allocateDirect(bufferSize * 2);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer samples = bb.asFloatBuffer();

            while (!stopped) {
                int numRead = recorder.read(shorts, 0, shorts.length);

                if (numRead != shorts.length) {
                    Ln.w("Did not read the full buffer ( %s / %s )", numRead, shorts.length);
                }
                if (readFailed(numRead)) {
                    throw new IllegalArgumentException(formatErrorMessage(recorder, numRead));
                }
                PcmUtils.toFloatData(shorts, numRead, 16, samples);
                listener.audioDataAvailable(samples);
            }
            Ln.i(formatLogMessage("stopping recording", recorder));
        } catch (Exception ex) {
            Ln.w("audioSampler, shuttingDown ? %s stoppedFlag ? %s, failed to capture data from microphone sessionId: %s exception: %s",
                    shuttingDown.get(), stopped,
                    recorder == null ? "null" : currentSessionId,
                    ex.toString()

            );
            if (!shuttingDown.get()) {
                Ln.i("audioSampler, failed, trigger onFailure");
                listener.onFailure();
            } else {
                Ln.i("audioSampler, shuttingDown, do not register a new listener.");
            }
        } finally {
            if (recorder != null) {
                Ln.i(formatLogMessage("stop and releasing recorder", recorder));
                recorder.release(); // Calls stop and handles the exception if its not initialized.
                Ln.i(formatLogMessage("recorder released", recorder));
            } else {
                Ln.i("audioSampler, no recorder to release");
            }

            synchronized (syncLock) {
                if (condition != null) {
                    Ln.i("audioSampler, is blocking stop, open condition " + condition);
                    condition.open();
                    Ln.i("audioSampler, blocking stop completed");
                } else {
                    Ln.i("audioSampler, non-blocking stop completed");
                }
            }
            started = false;
        }
    }

    private void setSessionId(AudioRecord recorder) {
        currentSessionId = recorder.getAudioSessionId() + 1;
    }

    private boolean readFailed(int numRead) {
        // Consider 0 read bytes a failure, if we aren't able to get the microphone read will return
        // 0 bytes read everytime
        return numRead <= 0;
    }

    private String formatLogMessage(String logMessage, AudioRecord recorder) {
        StringBuilder message = new StringBuilder("audioSampler, ");
        message.append(logMessage);
        message.append(String.format(Locale.US, " [ %08x ]", this.hashCode())); // for debugging issues in alpha
        appendSessionId(recorder, message);
        return message.toString();

    }

    private String formatErrorMessage(AudioRecord recorder, int numRead) {
        StringBuilder message = new StringBuilder("audioSampler, recording");
        message.append(String.format(Locale.US, " [ %08x ]", this.hashCode())); // for debugging issues in alpha
        appendSessionId(recorder, message);
        message.append(" failed with message: ");
        if (numRead == AudioRecord.ERROR_INVALID_OPERATION) {
            message.append("Invalid operation, not properly initialized");
        } else if (numRead == AudioRecord.ERROR_BAD_VALUE) {
            message.append("Bad value, no valid data");
        } else if (numRead == 0) {
            message.append("Unable to read data");
        } else {
            message.append("Unknown");
        }
        return message.toString();
    }

    private void appendSessionId(AudioRecord recorder, StringBuilder message) {
        message.append(String.format(Locale.US, " (sessionId %d)", recorder == null ? -2 : recorder.getAudioSessionId() + 1));
    }

    public void stop() {
        Ln.i("audioSampler, flagging thread to stop [ %08x ] (sessionId %d)", this.hashCode(), currentSessionId);
        stopped = true;
    }

    final private Object syncLock = new Object();

    public void stopImmediateBlocking() {
        Ln.i("audioSampler, stopImmediateBlocking, flagging thread to stop and block 30 sec timeout [ %08x ] (sessionId %d) is stopped = %s started = %s", this.hashCode(), currentSessionId, stopped, started);

        if (!started) {
            // might need to mark as stopped if there is a scheduled one going to start?
            Ln.i("audioSampler, ignore - no recorder started");
            return;
        }
        Ln.i("grab syncLock to create condition, old condition: " + condition);
        synchronized (syncLock) {
            condition = new ConditionVariable();
            Ln.i("got synclock, new condition: " + condition);
            shuttingDown.set(true);
        }
        if (recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            Ln.i("audioSampler, stopImmediateBlocking, stop recorder [start]:    " + currentSessionId);
            recorder.stop();
            Ln.i("audioSampler, stopImmediateBlocking, stop recorder [done]:     " + currentSessionId);
            Ln.i("audioSampler, stopImmediateBlocking, release recorder [start]: " + currentSessionId);
            recorder.release();
            Ln.i("audioSampler, stopImmediateBlocking, release recorder [done]:  " + currentSessionId);
        }
        stopped = true;
        Ln.i("audioSampler, stopImmediateBlocking, block on condition " + condition);
        final boolean block = condition.block(STOP_BLOCKING_TIMEOUT_MILLIS);
        condition = null;
        Ln.i("audioSampler, stopImmediateBlocking, flagging thread to stop and block Done. released ? %s [ %08x ] (sessionId %d)", block, this.hashCode(), currentSessionId);

    }

}
