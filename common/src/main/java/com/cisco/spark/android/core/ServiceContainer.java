package com.cisco.spark.android.core;

import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.presence.PresenceStatusListener;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.room.RoomService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServiceContainer {
    @Inject
    RoomService roomService;

    @Inject
    ActivityListener activityListener;

    @Inject
    LocusService locusService;

    @Inject
    CallControlService callControlService;

    @Inject
    PresenceStatusListener presenceStatusListener;

    @Inject
    public ServiceContainer() {
    }
}
