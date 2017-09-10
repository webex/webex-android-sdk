package com.ciscospark.androidsdk.core;

import android.app.Application;

import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.RootModule;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.NaturalLog;
import com.squareup.leakcanary.RefWatcher;

public class SparkInjector extends ApplicationDelegate {

    private final SparkModule _module;

    public SparkInjector(Application application) {
        super(application);
        _module = new SparkModule();
    }

    @Override
    public void create(boolean startAuthenticatedUserTask) {
        /* Override this function to prevent common lib auto login */
        super.create(false);
    }

    @Override
    protected Object getApplicationModule() {
        return _module;
    }

    @Override
    protected void onCreate() {

    }

    @Override
    protected void objectGraphCreated() {
        RootModule.setInjector(this);
        _module.setRefWatcher(RefWatcher.DISABLED);
    }

    @Override
    protected void afterInject() {

    }

    @Override
    protected NaturalLog buildLn() {
        return new DebugLn();
    }

}
