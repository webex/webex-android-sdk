package com.ciscospark.core;


import android.app.Application;
import android.support.annotation.CallSuper;

import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.RootModule;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.NaturalLog;
import com.squareup.leakcanary.RefWatcher;

class VideoTestApplicationDelegate extends ApplicationDelegate {
    private final VideoTestApplicationModule applicationModule;

    public VideoTestApplicationDelegate(Application application) {
        super(application);
        applicationModule = new VideoTestApplicationModule();
    }


    @Override
    protected void onCreate() {

    }

    @Override
    protected void objectGraphCreated() {
        RootModule.setInjector(this);
        applicationModule.setRefWatcher(RefWatcher.DISABLED);
    }

    @Override
    protected void afterInject() {

    }

    @Override
    protected NaturalLog buildLn() {
        return new DebugLn();
    }

    @Override
    protected Object getApplicationModule() {
        return applicationModule;
    }

}
