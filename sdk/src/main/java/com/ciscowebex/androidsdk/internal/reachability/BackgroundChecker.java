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

package com.ciscowebex.androidsdk.internal.reachability;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import com.ciscowebex.androidsdk.BuildConfig;
import com.ciscowebex.androidsdk.internal.queue.Scheduler;
import com.ciscowebex.androidsdk.utils.LoggingLock;
import com.github.benoitdion.ln.Ln;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class BackgroundChecker implements NetworkReachability.NetworkReachabilityObserver{

    public interface BackgroundListener {
        void onTransition(boolean foreground);
    }

    private final Context context;
    private final PowerManager powerManager;
    private final LoggingLock syncLock = new LoggingLock(BuildConfig.DEBUG, "BackgroundCheck");
    private final Condition activenessCondition = syncLock.newCondition();
    private boolean running = false;
    private boolean isInactive = true;

    private final NetworkReachability network;
    private final BackgroundListener listener;
    private TimerTask checker;

    public BackgroundChecker(Application application, BackgroundListener listener) {
        this.context = application;
        this.powerManager = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);;
        this.listener = listener;
        this.network = new NetworkReachability(context, this);
        application.registerReceiver(this.network, NetworkReachability.getIntentFilter());
    }

    public void start() {
        syncLock.lock();
        try {
            running = true;
            network.start();
            foregroundActivityDetected();
        } finally {
            syncLock.unlock();
        }
    }

    public void stop() {
        syncLock.lock();
        try {
            running = false;
            cancelChecker();
        } finally {
            syncLock.unlock();
        }
    }

    @Override
    public void onReachabilityChanged(boolean connected) {
        Ln.d("Network reachability changed:, in background = %s networkIsConnected = %s", isInBackground(), connected);
        if (waitForActiveState(0)) {
            if (connected) {
                Ln.d("Network connected. Resetting status");
                listener.onTransition(true);
            } else {
                Ln.d("Connectivity lost.");
                listener.onTransition(false);
            }
        }
    }

    @Override
    public void onConfigurationChanged(boolean isProxyChanged) {
        Ln.d("Network configuration changed: " + isProxyChanged);
    }

    public boolean tryForeground() {
        boolean ret = false;
        syncLock.lock();
        try {
            if (isInactive()) {
                listener.onTransition(true);
                Ln.i("TRY BG->FG: transition");
                isInactive = false;
                activenessCondition.signalAll();
                ret = true;
            }

        } finally {
            syncLock.unlock();
        }
        logBatteryStatus();
        return ret;
    }

    public boolean tryBackground() {
        boolean ret = false;
        syncLock.lock();
        try {
            if (isInBackground()) {
                isInactive = true;
                Ln.i("TRY FG->BG: transition");
                listener.onTransition(false);
                ret = true;
            }
        }
        finally {
            syncLock.unlock();
        }
        logBatteryStatus();
        return ret;
    }

    private void foregroundActivityDetected() {
        Ln.d("ForegroundActivityDetected");
        syncLock.lock();
        try {
            if (isInactive()) {
                listener.onTransition(true);
                Ln.i("BG->FG: transition");
                isInactive = false;
                activenessCondition.signalAll();
            }
            if (isInBackground()) {
                isInactive = true;
                Ln.i("FG->BG: transition");
                listener.onTransition(false);
            }
            cancelChecker();
            checker = Scheduler.schedule(() -> {
                Ln.d("ForegroundActivityDetected: " + isInBackground());
                if (isInBackground()) {
                    isInactive = true;
                    Ln.d("FG->BG: transition");
                    listener.onTransition(false);
                } else {
                    if (!running) {
                        cancelChecker();
                    } else {
                        foregroundActivityDetected();
                    }
                }
            }, 10000, true);
        } finally {
            syncLock.unlock();
        }
        logBatteryStatus();
    }

    private void cancelChecker() {
        if (checker != null) {
            checker.cancel();
            checker = null;
        }
    }

    public boolean isInBackground() {
        boolean isInForeground = ForegroundChecker.getInstance().isForeground();
        boolean isDeviceInteractive = (powerManager == null || powerManager.isInteractive());
        Ln.d("isInForeground = %b, isDeviceInteractive = %b", isInForeground, isDeviceInteractive);
        return (!isDeviceInteractive || !isInForeground);
    }

    private boolean isInactive() {
        return isInactive;
    }

    private boolean waitForActiveState(long msTimeToWait) {
        if (!isInactive()) {
            return true;
        }
        syncLock.lock();
        try {
            if (!isInactive()) {
                return true;
            }
            activenessCondition.await(msTimeToWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            syncLock.unlock();
        }
        return !isInactive();
    }

    private void logBatteryStatus() {
        try {
            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus != null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) (100 * level / (float) scale);
                Ln.i("Battery percentage = %d, charging = %b", batteryPct, isCharging);
            }
        } catch (Exception ex) {
            Ln.w(ex);
        }
    }
}
