package com.cisco.spark.android.locus.service;


import com.cisco.spark.android.locus.events.LocusProcessorEvent;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.locus.events.LocusProcessorEvent.Type.NEW_EVENT;
import static com.cisco.spark.android.locus.events.LocusProcessorEvent.Type.PROCESSED_EVENT;


/**
 * A helper class stub for unit and integration testing.  For default debug and release builds, BaseSquaredModule.java
 * will instantiate an empty class that does nothing; but for tests--that desire it--the instance below will be
 * returned to post bus events about LocusProcessor actions
 */

public class LocusProcessorReporter {
    private EventBus bus;
    private int unprocessedEvents;

    public LocusProcessorReporter(EventBus bus) {
        this.bus = bus;
        unprocessedEvents = 0;
    }

    public void reportNewEvent(String info) {
        bus.post(new LocusProcessorEvent(NEW_EVENT, info));
        unprocessedEvents++;
    }

    public void reportProcessedEvent(String info) {
        bus.post(new LocusProcessorEvent(PROCESSED_EVENT, info));
        unprocessedEvents--;
    }

    public int getUnprocessedEvents() {
        return unprocessedEvents;
    }
}
