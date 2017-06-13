package com.cisco.spark.android.processing;

import android.content.*;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.util.AppVersionProvider;
import com.cisco.spark.android.callcontrol.*;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;

import dagger.*;
import de.greenrobot.event.*;

import javax.inject.*;
import java.lang.reflect.*;

@Module(
    complete = false,
    library = true
)
public class ActivityProcessorModule {
    private static final String DRONE_PROCESSOR_CLASS = "com.cisco.wx2.android.processing.drone.Drone";

    @Provides
    @Singleton
    public ActivityListener provideActivityListener(EventBus bus, @Named("drone") ActivityProcessor drone) {
        ActivityListener activityListener = new ActivityListener(bus);
        activityListener.register(drone);
        return activityListener;
    }

    @Provides
    @Singleton
    @Named("drone")
    public ActivityProcessor provideDrone(final Context context, final CallOptions options, final AppVersionProvider appVersionProvider, final AuthenticatedUserProvider userProvider,
                                          final CallControlService callControlService, final MediaEngine mediaEngine, final EventBus bus,
                                          final ApiClientProvider apiClientProvider, final DeviceRegistration deviceRegistration, final Settings settings, final OperationQueue operationQueue) {
        try {
            Class clazz = Class.forName(DRONE_PROCESSOR_CLASS);
            Constructor<?> ctor = clazz.getConstructor(Context.class, CallOptions.class, AppVersionProvider.class,
                    AuthenticatedUserProvider.class, CallControlService.class, MediaEngine.class, EventBus.class,
                    ApiClientProvider.class, DeviceRegistration.class, Settings.class, OperationQueue.class);

            return (ActivityProcessor) ctor.newInstance(context, options, appVersionProvider, userProvider, callControlService, mediaEngine,
                    bus, apiClientProvider, deviceRegistration, settings, operationQueue);
        } catch (Exception ex) {
            return null;
        }
    }
}
