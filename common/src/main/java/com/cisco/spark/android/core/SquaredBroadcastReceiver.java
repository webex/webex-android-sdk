package com.cisco.spark.android.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cisco.spark.android.util.TestUtils;
import com.github.benoitdion.ln.Ln;

public abstract class SquaredBroadcastReceiver extends BroadcastReceiver {
    private boolean initialized;

    /*
        NOTE: After calling super, you must check isInitialized() is true before
        Continuing. If its false there is a chance that something else has happened and
        your injected variables are null.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TestUtils.isRunningUnitTest()) {
            try {
                RootModule.getInjector().inject(this);
                initialized = true;
            } catch (Exception ex) {
                Ln.e(ex);
            }
        } else {
            Ln.d("isRunningUnitTest() == true");
        }

        Ln.d("initialized => " + initialized);
    }

    protected boolean isInitialized() {
        return initialized;
    }
}
