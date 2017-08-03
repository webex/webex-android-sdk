package com.cisco.spark.android.core;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentProvider;
import android.os.StrictMode;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.app.AndroidSystemServicesModule;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.log.LogUncaughtExceptionHandler;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.util.TestUtils;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.SubscriberExceptionEvent;

public abstract class ApplicationDelegate implements Injector {
    private static List<WeakReference<ContentProvider>> contentProviders = new ArrayList<WeakReference<ContentProvider>>();
    private Application application;
    private ObjectGraph objectGraph;
    private LogUncaughtExceptionHandler logUncaughtExceptionHandler;

    @Inject
    LogFilePrint log;

    @Inject
    ApiTokenProvider apiTokenProvider;

    @Inject
    EventBus bus;

    @Inject
    ApplicationController applicationController;

    @Inject
    ServiceContainer serviceContainer;

    @Inject
    MediaEngine mediaEngine;

    @Inject
    Settings settings;

    public ApplicationDelegate(Application application) {
        this.application = application;

        logUncaughtExceptionHandler = new LogUncaughtExceptionHandler();
        logUncaughtExceptionHandler.setPreviousUncaughtExceptionHandlers(getExceptionHandlers());
        Thread.setDefaultUncaughtExceptionHandler(logUncaughtExceptionHandler);
    }

    public void create() {
        initializeLn();
        Ln.d("SquaredApplication - onCreate()");

        onCreate();

        if (TestUtils.isInstrumentation()) {
            return;
        }
        create(true);
    }

    protected abstract void onCreate();

    protected abstract void objectGraphCreated();

    protected abstract void afterInject();

    protected abstract NaturalLog buildLn();

    protected List<Thread.UncaughtExceptionHandler> getExceptionHandlers() {
        return Collections.singletonList(Thread.getDefaultUncaughtExceptionHandler());
    }

    protected void initializeLn() {
        Ln.initialize(buildLn());
    }

    protected Application getApplication() {
        return application;
    }

    public void create(boolean startAuthenticatedUserTask) {
        Ln.i("SquaredApplication#onCreate. Setting object graph");
        initializeObjectGraph();
        objectGraphCreated();
        inject();
        afterInject();

        enableStrictMode();

        if (startAuthenticatedUserTask) {
            startAuthenticatedUserTask();
        }
    }

    /**
     * Content providers get created before the application class
     * This means the object graph is not setup when that happens.
     * Content providers that need injection should register and will get injected when the object graph is ready
     * @param contentProvider a content provider
     */
    public static void registerContentProvider(ContentProvider contentProvider) {
        contentProviders.add(new WeakReference<ContentProvider>(contentProvider));
    }

    private void startAuthenticatedUserTask() {
        if (apiTokenProvider.isAuthenticated()) {
            new AuthenticatedUserTask(applicationController).execute();
        }
    }

    private void inject() {
        this.objectGraph.inject(this);

        for (WeakReference<ContentProvider> contentProviderReference : contentProviders) {
            ContentProvider contentProvider = contentProviderReference.get();
            if (contentProvider != null) {
                this.objectGraph.inject(contentProvider);
            }
        }
        log.startCapture();
        logUncaughtExceptionHandler.setLogFilePrint(log);
        logUncaughtExceptionHandler.setBus(bus);
        bus.register(this);
    }

    @SuppressLint("NewApi")
    private void enableStrictMode() {
        if (!BuildConfig.DEBUG) {
            return;
        }

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder();
        vmPolicyBuilder = vmPolicyBuilder.detectLeakedSqlLiteObjects()
                .penaltyLog()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects();

        // Do not enable detectActivityLeaks.
        // It's too strict and will sentence the death penalty for activities that are being disposed
        // or only referenced by weak references.
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(SubscriberExceptionEvent event) {
        Ln.e(event.throwable, "Exception when trying to process event");
    }

    private void initializeObjectGraph() {
        if (bus != null) {
            bus.unregister(this);
        }
        if (objectGraph == null) {
            objectGraph = ObjectGraph.create(getModules());
        }
    }

    protected Object[] getModules() {
        return new Object[] {
            new RootModule(this),
            new AndroidModule(),
            new SquaredModule(),
            new BaseSquaredModule(),
            new AndroidSystemServicesModule(),
            new LiveProximityModule(),
            getApplicationModule()
        };
    }

    protected abstract Object getApplicationModule();

    public void setObjectGraph(ObjectGraph objectGraph) {
        this.objectGraph = objectGraph;
    }

    @Override
    public void inject(Object object) {
        objectGraph.inject(object);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    public EventBus getBus() {
        return bus;
    }

    public Settings getSettings() {
        return settings;
    }
}
