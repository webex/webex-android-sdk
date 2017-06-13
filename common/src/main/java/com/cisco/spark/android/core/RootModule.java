package com.cisco.spark.android.core;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Includes all the modules used in the application.
 */
@Module(
        library = true,
        complete = false
)
public class RootModule {
    private static Injector injector;

    public RootModule(Injector injector) {
        this.injector = injector;
    }

    @Provides
    @Singleton
    Injector provideInjector() {
        return injector;
    }

    public static Injector getInjector() {
        return injector;
    }

    public static void setInjector(Injector injector) {
        RootModule.injector = injector;
    }
}
