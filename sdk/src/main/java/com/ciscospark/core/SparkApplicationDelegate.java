package com.ciscospark.core;


import com.cisco.spark.android.app.AndroidSystemServicesModule;
import com.cisco.spark.android.core.AndroidModule;
import com.cisco.spark.android.core.Application;
import com.cisco.spark.android.core.ApplicationDelegate;
import com.cisco.spark.android.core.BaseSquaredModule;
import com.cisco.spark.android.core.LiveProximityModule;
import com.cisco.spark.android.core.RootModule;
import com.cisco.spark.android.core.SquaredModule;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

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

    protected void onCreate() {
        this.initializeObjectGraph();
        this.objectGraphCreated();
        this.inject();
        this.afterInject();
    }

    protected void objectGraphCreated() {
        RootModule.setInjector(this);
    }

    protected void afterInject() {
        Ln.i(bus.toString());

    }

    @Override
    protected NaturalLog buildLn() {
        return new DebugLn();
    }

    protected Object getApplicationModule() {
        return applicationModule;
    }

    private void inject() {
        this.objectGraph.inject(this);

        this.bus.register(this);
    }

    private void initializeObjectGraph() {
        if (this.bus != null) {
            this.bus.unregister(this);
        }

        if (this.objectGraph == null) {
            this.objectGraph = ObjectGraph.create(this.getModules());
        }

    }

    protected Object[] getModules() {
        return new Object[]{new RootModule(this), new AndroidModule(), new SquaredModule(), new BaseSquaredModule(), new AndroidSystemServicesModule(), new LiveProximityModule(), this.getApplicationModule()};
    }

    @Override
    public void inject(Object o) {

    }

    @Override
    public ObjectGraph getObjectGraph() {
        return null;
    }
}
