package com.cisco.spark.android.util;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CpuLogger implements Component {
    private static final int CPU_LOGGING_INTERVAL = 2000;

    private final DeviceRegistration deviceRegistration;

    private final Object syncLock = new Object();
    private Timer timer;
    private boolean stopped;
    private CpuStat cpuStat = new CpuStat();

    @Inject
    public CpuLogger(DeviceRegistration deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        if (deviceRegistration.getFeatures().isTeamMember()) {
            Ln.i("CpuLogger - start()");
            stopped = false;
            scheduleNextTick();
        }
    }

    @Override
    public void stop() {
        synchronized (syncLock) {
            stopped = true;
            /**
             * Cancel the timer regardless of the isCpuLoggingEnabled setting value.
             * It might have changed after {@link #start()} was called.
             */
            if (timer != null) {
                Ln.i("CpuLogger - stop()");
                timer.cancel();
                timer = null;
            }
        }
    }

    private void scheduleNextTick() {
        synchronized (syncLock) {
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer("CPU logger timer.");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Ln.i("%s", cpuStat.toString());
                    if (stopped) {
                        if (timer != null) {
                            timer.cancel();
                        }
                    } else {
                        scheduleNextTick();
                    }
                }
            }, CPU_LOGGING_INTERVAL);
        }
    }

    public CpuStat getCpuStat() {
        return cpuStat;
    }
}
