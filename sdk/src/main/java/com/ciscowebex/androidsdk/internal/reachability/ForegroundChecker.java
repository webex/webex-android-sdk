package com.ciscowebex.androidsdk.internal.reachability;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import com.github.benoitdion.ln.Ln;

public class ForegroundChecker implements Application.ActivityLifecycleCallbacks {

    private static ForegroundChecker instance;
    private boolean foreground = false, paused = true;
    private Handler handler = new Handler();
    private Runnable check;

    public static void init(android.app.Application app) {
        if (instance == null) {
            instance = new ForegroundChecker();
            instance.onActivityResumed(null);
            app.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static ForegroundChecker getInstance() {
        return instance;
    }

    private ForegroundChecker() { }

    public boolean isForeground() {
        return foreground;
    }

    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    public void onActivityStarted(Activity activity) {
    }

    public void onActivityResumed(Activity activity) {
        Ln.d("onActivityResumed");
        foreground = true;
        paused = false;
        if (check != null)
            handler.removeCallbacks(check);
    }

    public void onActivityPaused(Activity activity) {
        Ln.d("onActivityPaused");
        paused = true;
        if (check != null) {
            handler.removeCallbacks(check);
        }
        handler.postDelayed(check = () -> {
            if (foreground && paused) {
                foreground = false;
            }
        }, 500);
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

}
