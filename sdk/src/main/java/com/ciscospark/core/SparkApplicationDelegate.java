package com.ciscospark.core;


import com.cisco.spark.android.core.Application;
import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.RootModule;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Inject;

import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;

class SparkApplicationDelegate extends ApplicationDelegate {
    private final SparkApplicationModule applicationModule;
    private ObjectGraph objectGraph;

    @Inject
    EventBus bus;

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

    @Override
    public void create(boolean startAuthenticatedUserTask) {
        /* Override this function to prevent common lib auto login */
        super.create(false);
    }
}
