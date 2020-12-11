/*
 * Copyright 2016-2021 Cisco Systems Inc
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
