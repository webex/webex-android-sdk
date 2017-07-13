package com.ciscospark.core;


import android.app.Application;

import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.RootModule;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.NaturalLog;

class SparkApplicationDelegate extends ApplicationDelegate {
    private final SparkApplicationModule applicationModule;

    public SparkApplicationDelegate(Application application) {
        super(application);
        applicationModule = new SparkApplicationModule();
    }


    @Override
    protected void onCreate() {

    }

    @Override
    protected void objectGraphCreated() {
        RootModule.setInjector(this);
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
