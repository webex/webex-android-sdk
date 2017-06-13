package com.cisco.spark.android.core;

import dagger.ObjectGraph;

public interface Injector {
    void inject(Object object);

    ObjectGraph getObjectGraph();
}
