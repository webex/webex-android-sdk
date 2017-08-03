package com.cisco.spark.android.core;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.app.ActivityManager;
import com.cisco.spark.android.app.PowerManager;
import com.cisco.spark.android.events.UIBackgroundTransition;
import com.cisco.spark.android.events.UIForegroundTransition;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.util.LoggingLock;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import de.greenrobot.event.EventBus;

/**
 * Class to determine when the application has gone to the background or is brought to the foreground.
 * <p/>
 * A {@link com.cisco.spark.android.events.UIBackgroundTransition} event is sent on the bus when the app goes in the background.
 * A {@link com.cisco.spark.android.events.UIForegroundTransition} event is sent on the bus when the app is brought to the foreground.
 * <p/>
 * The application is considered in the background if any of the following condition is true:
 * - The screen is off.
 * - the app is not the most recent task reported by the {@link android.app.ActivityManager}.
 * - There is no active call
 * <p/>
 * One of the above condition must remain true for {@link #timeout} before a {@link com.cisco.spark.android.events.UIBackgroundTransition} is sent.
 */
public class SquaredBackgroundCheck implements BackgroundCheck {
    /**
     * How long when the screen is off, or the application is in the background
     * before posting a transition event.
     */
    private static final int DEFAULT_TIMEOUT = 30000;

    private final LocusDataCache locusCache;
    private final int timeout;
    private final EventBus bus;
    private final Context context;
    private final PowerManager powerManager;
    private final ActivityManager activityManager;
    private final LoggingLock syncLock = new LoggingLock(BuildConfig.DEBUG, "BackgroundCheck");
    private final Condition activenessCondition = syncLock.newCondition();
    private final NaturalLog ln;

    private Timer timer;
    private boolean running = false;

    private boolean isInactive = true;

    public SquaredBackgroundCheck(EventBus bus, Context context, PowerManager powerManager, ActivityManager activityManager, LocusDataCache locusCache) {
        this(bus, context, powerManager, activityManager, locusCache, DEFAULT_TIMEOUT);
    }

    public SquaredBackgroundCheck(EventBus bus, Context context, PowerManager powerManager, ActivityManager activityManager, LocusDataCache locusCache, int timeout) {
        this.bus = bus;
        this.context = context;
        this.powerManager = powerManager;
        this.activityManager = activityManager;
        this.locusCache = locusCache;
        this.timeout = timeout;
        this.ln = Ln.get("BackgroundCheck");
    }

    public void start() {
        running = true;
        if (!bus.isRegistered(this))
            bus.register(this);
        foregroundActivityDetected();
    }

    public void stop() {
        syncLock.lock();
        try {
            bus.unregister(this);
            running = false;
            cancelTimer();
        } finally {
            syncLock.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(ForegroundActivity event) {
        foregroundActivityDetected();
    }

    private void foregroundActivityDetected() {
        syncLock.lock();
        try {
            if (isInactive()) {
                if (isInactive()) {
                    bus.post(new UIForegroundTransition());
                    ln.i("BG->FG: transition");
                    isInactive = false;
                    activenessCondition.signalAll();
                }
            }

            cancelTimer();
            timer = new Timer("Background check timer");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isInBackground()) {
                        isInactive = true;
                        ln.i("FG->BG: transition");
                        bus.post(new UIBackgroundTransition());
                    } else {
                        if (!running) {
                            cancelTimer();
                        } else {
                            foregroundActivityDetected();
                        }
                    }
                }
            }, timeout);
        } finally {
            syncLock.unlock();
        }

        logBatteryStatus();
    }

    private void cancelTimer() {
        if (timer != null)
            timer.cancel();
        timer = null;
    }

    @Override
    public boolean isInBackground() {

        boolean isSparkMostRecentTask = activityManager.isMostRecentTask();
        boolean isDeviceInteractive = isInteractive();

        return (!isDeviceInteractive || !isSparkMostRecentTask) && !locusCache.isInCall();
    }

    @Override
    public boolean isInteractive() {
        return powerManager == null || powerManager.isInteractive();
    }

    private boolean isInactive() {
        return isInactive;
    }

    @Override
    public boolean waitForActiveState(long msTimeToWait) {
        if (!isInactive())
            return true;

        syncLock.lock();
        try {
            if (!isInactive())
                return true;

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
                // Are we charging / charged?
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

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
