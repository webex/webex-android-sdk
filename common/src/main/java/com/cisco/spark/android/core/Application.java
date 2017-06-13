package com.cisco.spark.android.core;

import android.content.Context;
import android.support.multidex.MultiDex;

public abstract class Application extends android.app.Application {
    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Application getInstance() {
        return instance;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(this);
    }
}
