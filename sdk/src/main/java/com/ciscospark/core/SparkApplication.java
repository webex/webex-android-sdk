package com.ciscospark.core;


import com.cisco.spark.android.core.Application;

public class SparkApplication extends Application {
    private static SparkApplication instance;
    private SparkApplicationDelegate delegate;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        delegate = new SparkApplicationDelegate(this);
        delegate.create();
    }

    public static SparkApplication getInstance() {
        return instance;
    }

    public void inject(Object object) {
        delegate.inject(object);
    }
}