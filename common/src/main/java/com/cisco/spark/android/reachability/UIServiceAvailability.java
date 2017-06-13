package com.cisco.spark.android.reachability;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.cisco.spark.android.core.BackgroundCheck;
import com.cisco.spark.android.events.UIBackgroundTransition;
import com.cisco.spark.android.events.UIForegroundTransition;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.mercury.MercuryClient;
import com.github.benoitdion.ln.Ln;

import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

public class UIServiceAvailability {

    private static final long NO_SERVICE_NOTIFY_DELAY = 30000;

    private Timer timer = new Timer();
    private final EventBus bus;
    private final BackgroundCheck backgroundCheck;
    private final MercuryClient mercuryClient;
    private final ConnectivityManager connectivityManager;
    private Availability availability = Availability.CONNECTED;
    private NotifyTimerTask timerTask;

    public synchronized Availability getAvailability() {
        return availability;
    }

    public enum Availability {
        CONNECTED,
        NO_SERVICE,
        NO_NETWORK;
    }

    public UIServiceAvailability(EventBus bus, BackgroundCheck backgroundCheck, MercuryClient mercuryClient,
                                 ConnectivityManager connectivityManager) {
        this.bus = bus;
        this.backgroundCheck = backgroundCheck;
        this.mercuryClient = mercuryClient;
        this.connectivityManager = connectivityManager;
        bus.register(this);
    }

    public synchronized void update() {
        Availability oldAvailability = availability;
        if (!isNetworkConnected()) {
            availability = Availability.NO_NETWORK;
            postDelayedServiceAvailabilityEvent();
        } else if (!isServiceAvailable()) {
            availability = Availability.NO_SERVICE;
            postDelayedServiceAvailabilityEvent();
        } else {
            availability = Availability.CONNECTED;
            clearServiceAvailabilityEvent();
        }

        if (oldAvailability != availability) {
            Ln.i("UIServiceAvailability changed: " + oldAvailability + " -> " + availability);
        }
    }

    protected synchronized void clearServiceAvailabilityEvent() {
        if (timerTask == null)
            return;

        timerTask.cancel();
        timerTask = null;
        bus.post(new UIServiceAvailabilityEvent(availability));
    }

    private synchronized void postDelayedServiceAvailabilityEvent() {
        if (timerTask != null)
            return;

        timerTask = new NotifyTimerTask();
        timer.schedule(timerTask, NO_SERVICE_NOTIFY_DELAY);
    }

    private boolean isServiceAvailable() {
        // No need to complain if the app is in the background
        return mercuryClient.isRunning() || backgroundCheck.isInBackground();
    }

    public synchronized boolean isTimerExpired() {
        return timerTask != null && timerTask.isDone();
    }

    public static class UIServiceAvailabilityEvent {
        Availability availability;

        public UIServiceAvailabilityEvent(Availability availability) {
            this.availability = availability;
        }

        public Availability getAvailability() {
            return availability;
        }

        public boolean isServiceAvailable() {
            return availability == Availability.CONNECTED;
        }
    }

    private class NotifyTimerTask extends TimerTask {

        boolean done;

        @Override
        public void run() {
            update();
            if (availability != Availability.CONNECTED) {
                Ln.i("Notifying UIServiceAvailability state: " + availability);
                done = true;
                bus.post(new UIServiceAvailabilityEvent(availability));
            }
        }

        public boolean isDone() {
            return done;
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private synchronized void reset() {
        clearServiceAvailabilityEvent();
        availability = Availability.CONNECTED;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(NetworkReachabilityChangedEvent event) {
        update();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(ResetEvent event) {
        update();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(MercuryClient.MercuryConnectedEvent event) {
        update();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(UIForegroundTransition event) {
        update();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(UIBackgroundTransition event) {
        reset();
    }

    public void setAvailability(Availability state) {
        //NOT IMPL overridden for testing
        return;
    }
}
