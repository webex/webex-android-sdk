package com.ciscowebex.androidsdk.internal;

import com.cisco.spark.android.core.Injector;
import com.ciscowebex.androidsdk_commlib.SDKCommonInjector;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class SDKModule {

    private SDKCommonInjector<SDKComponent> injector;

    public SDKModule(SDKCommonInjector<SDKComponent> injector) {
        this.injector = injector;
    }

    @Provides
    @SDKScope
    @Named("SDK")
    Injector provideInjector() {
        return injector;
    }
}
