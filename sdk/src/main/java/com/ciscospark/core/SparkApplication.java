package com.ciscospark.core;


import android.support.annotation.CallSuper;
import android.util.Log;

import com.cisco.spark.android.core.Application;
import com.cisco.spark.android.core.ApplicationDelegate;


public class SparkApplication extends Application {
    private static SparkApplication instance;
    private ApplicationDelegate delegate;

    private static final String TAG = "SparkApplication";

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        instance = this;

        Log.d(TAG, "before daggerInit");
        delegate = new SparkApplicationDelegate(this);
        delegate.create();
        Log.i(TAG, "onCreate: ->after daggerInit");
    }

    public static SparkApplication getInstance() {
        return instance;
    }

    public void inject(Object object) {
        delegate.inject(object);
    }

}