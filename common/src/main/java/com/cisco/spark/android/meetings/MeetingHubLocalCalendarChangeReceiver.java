package com.cisco.spark.android.meetings;

import android.content.Context;
import android.content.Intent;

import com.cisco.spark.android.core.SquaredBroadcastReceiver;
import com.cisco.spark.android.events.MeetingHubLocalCalendarChangedEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MeetingHubLocalCalendarChangeReceiver extends SquaredBroadcastReceiver {

    @Inject
    EventBus bus;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!isInitialized()) {
            return;
        }
        if (intent != null) {
            bus.post(new MeetingHubLocalCalendarChangedEvent());
        }
    }
}
