/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.media.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.PowerManager;
import android.os.SystemClock;
import com.github.benoitdion.ln.Ln;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProximitySensor implements SensorEventListener {

    public interface Listener {

        enum ProximityEvent {
            NEAR, FAR
        }

        void onEvent(ProximityEvent event);

    }

    private static final String WAKELOCK_TAG = "androidsdk:ProximitySensorLock";
    private static final String EVENT_TIMER_TAG = "Proximity event timer";
    private static final long DEFAULT_SENSITIVITY_MSEC = 1500;
    private static final long SENSITIVITY_TIMER_MINIMUM = 10;
    private static int wakeLockType = PowerManager.PARTIAL_WAKE_LOCK;

    private Sensor proximitySensor;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private boolean hasProximitySensor;
    private final Context context;
    private SensorManagerDelegate sensorManager;
    private long sensitivity;
    private Timer sensitivityTimer;
    private TimerTask sensitivityTimerTask;
    private AtomicBoolean nearEventScheduled = new AtomicBoolean(false);
    private long firstProximityChangedEvent;
    private Listener listener;

    public ProximitySensor(Context context, Listener listener) {
        this(context, DEFAULT_SENSITIVITY_MSEC, listener);
    }

    public ProximitySensor(Context context, long sensitivity, Listener listener) {
        this.context = context;
        this.sensorManager = new SensorManagerDelegate(context);
        this.listener = listener;
        setSensitivity(sensitivity);
        firstProximityChangedEvent = 0;
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        hasProximitySensor = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);
        Ln.d("System reports %s a proximity sensor", hasProximitySensor ? "having" : "not having");
        if (hasProximitySensor) {
            if (sensorManager != null) {
                proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                if (proximitySensor == null) {
                    Ln.w("Failed to get ProximitySensor");
                }
            }
            else {
                Ln.w("Failed to get SensorManager");
            }

            try {
                // Documentation says PARTIAL_WAKE_LOCK level 'should' give us a WakeLock that controls the screen, but doesn't.
                // So using the previously exposed, but now hidden level of PROXIMITY_SCREEN_OFF_WAKE_LOCK, which works well.
                wakeLockType = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            } catch (Throwable ignored) {
                Ln.w("Unable to get PROXIMITY_SCREEN_OFF_WAKE_LOCK level for newWakeLock.  Using default: PARTIAL_WAKE_LOCK");
            }
        }
    }

    public boolean isProximityAvailable() {
        return hasProximitySensor;
    }

    public void setSensitivity(long value) {
        sensitivity = value > 0 ? value : 0;
        // if sensitivity is below minimum, then timers won't be used and 'scheduled' should always be true
        if (sensitivity < SENSITIVITY_TIMER_MINIMUM)
            nearEventScheduled.set(true);
    }

    public void onResume() {
        Ln.d("ProximitySensor.onResume, sensorManager = " + sensorManager + ", proximitySensor = " + proximitySensor);
        if (hasProximitySensor && sensorManager != null && proximitySensor != null) {
            Ln.d("Registering listener for proximity sensor with sensitivity=%d", sensitivity);
            sensorManager.registerListener(this, proximitySensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
            acquireScreenLock();
        }
    }

    public void onPause() {
        if (hasProximitySensor && sensorManager != null && proximitySensor != null) {
            Ln.d("Un-registering listener for proximity sensor");
            sensorManager.unregisterListener(this);
            releaseScreenLock();
        }
    }

    public void disableScreen() {
        // There's a small window where a NEAR event can get posted on the bus, before a canceling
        // FAR event can catch and remove it.  The synchronized check of nearEventScheduled closes that.
        if (wakeLock != null && !wakeLock.isHeld() && nearEventScheduled.get()) {
            Ln.i("Disabling screen!");
            wakeLock.acquire();
        }
    }

    public void enableScreen() {
        if (wakeLock != null && wakeLock.isHeld()) {
            Ln.i("Enabling screen");
            wakeLock.release();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Ln.d("onAccuracyChanged: sensor=%s, accuracy=%d", sensor.getName(), accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float eventValue = event.values[0];
        Ln.i("onSensorChanged: type=%d, value[0]=%f", event.sensor.getType(), eventValue);

        /*
         * Work around defective proximity sensors by ignoring a proximity sensor that
         * immediately reports that an object is close and never reports that it goes
         * away.
         */
        long time = SystemClock.currentThreadTimeMillis();
        if (firstProximityChangedEvent == 0) {
            firstProximityChangedEvent = time;
        }
        else if (eventValue == 0 && time - firstProximityChangedEvent < 500) {
            Ln.w("Ignoring an immediate NEAR proximity event, which often indicates a faulty sensor!");
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (eventValue < proximitySensor.getMaximumRange()) {
                handleNearEvent();
            }
            else {
                handleFarEvent();
            }
        }
    }

    private void acquireScreenLock() {
        wakeLock = powerManager.newWakeLock(wakeLockType, WAKELOCK_TAG);
        if (wakeLock == null) {
            Ln.w("Failed to get WakeLock for screen control");
        }
    }

    private void releaseScreenLock() {
        enableScreen();
        wakeLock = null;
    }

    public boolean isWakeLockHeld() {
        Ln.d("isWakeLockHeld: %s", wakeLock.isHeld() ? "true" : "false");
        return wakeLock.isHeld();
    }

    private void handleNearEvent() {
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Ln.i("ignore proximity sensor (NEAR event) while in landscape mode");
            return;
        }
        else if (isTablet(context)) {
            Ln.i("ignore proximity sensor (NEAR event) on non-phone (tablet) device");
            return;
        }

        if (sensitivity >= SENSITIVITY_TIMER_MINIMUM) {
            // if a sensitivity is configure and we don't have a pending event, post a delayed event
            if (!nearEventScheduled.get()) {
                sensitivityTimer = new Timer(EVENT_TIMER_TAG);
                sensitivityTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        Ln.d("posting proximity NEAR event");
                        listener.onEvent(Listener.ProximityEvent.NEAR);
                        nearEventScheduled.set(false);
                    }
                };
                sensitivityTimer.schedule(sensitivityTimerTask, sensitivity);
                nearEventScheduled.set(true);
            }
            return;
        }
        Ln.d("posting proximity NEAR event");
        listener.onEvent(Listener.ProximityEvent.NEAR);
    }

    private void handleFarEvent() {
        if (isTablet(context)) {
            Ln.i("ignore proximity sensor (FAR event) on non-phone (tablet) device");
            return;
        }

        if (sensitivity >= SENSITIVITY_TIMER_MINIMUM) {
            if (nearEventScheduled.getAndSet(false)) {
                Ln.d("FAR event occurred before NEAR timer task executed--cancelling NEAR event");
                sensitivityTimerTask.cancel();
                sensitivityTimer.cancel();
                return;
            }
        }
        Ln.d("posting proximity FAR event");
        listener.onEvent(Listener.ProximityEvent.FAR);
    }

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
