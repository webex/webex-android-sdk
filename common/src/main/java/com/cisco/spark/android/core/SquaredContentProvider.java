package com.cisco.spark.android.core;

import android.content.ContentProvider;

public abstract class SquaredContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        ApplicationDelegate.registerContentProvider(this);
        return true;
    }
}
