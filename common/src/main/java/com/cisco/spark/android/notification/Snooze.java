package com.cisco.spark.android.notification;

import com.cisco.spark.android.util.Action0;
import com.cisco.spark.android.util.Clock;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Snooze {
    public enum SnoozeDuration {
        ONE_HOUR, UNTIL_MORNING, UNTIL_MONDAY, DISABLE
    }

    private static final int MORNING_HOUR = 8;
    private static final int DAY_START = 4;

    private final SnoozeStore store;
    private final Clock clock;

    Action0 action;

    @Inject
    public Snooze(SnoozeStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public boolean snoozed() {
        return store.getSnoozeUntil() > clock.now();
    }

    public boolean disabled() {
        return store.getSnoozeUntil() == Long.MAX_VALUE;
    }

    public void snooze(SnoozeDuration duration) {
        long now = clock.now();
        long notificationTime = 0;

        if (duration == SnoozeDuration.ONE_HOUR) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.add(Calendar.HOUR, 1);
            cal.set(Calendar.SECOND, 0);
            notificationTime = cal.getTime().getTime();
        } else if (duration == SnoozeDuration.UNTIL_MORNING) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            int hour = cal.get(Calendar.HOUR);
            if (hour >= DAY_START) {
                cal.add(Calendar.DATE, 1);
            }
            cal.set(Calendar.HOUR, MORNING_HOUR);
            cal.set(Calendar.SECOND, 0);
            notificationTime = cal.getTime().getTime();
        } else if (duration == SnoozeDuration.UNTIL_MONDAY) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int hour = cal.get(Calendar.HOUR);

            // If we're partying late on Sunday night, no need to add any day to the calendar.
            if (hour >= DAY_START || dayOfWeek != Calendar.MONDAY) {
                int daysTillMonday = Calendar.MONDAY - cal.get(Calendar.DAY_OF_WEEK) + 7;
                if (daysTillMonday > 7) {
                    daysTillMonday = daysTillMonday - 7; // Sunday
                }
                cal.add(Calendar.DATE, daysTillMonday);
            }
            cal.set(Calendar.HOUR, MORNING_HOUR);
            cal.set(Calendar.SECOND, 0);
            notificationTime = cal.getTime().getTime();
        } else if (duration == SnoozeDuration.DISABLE) {
            notificationTime = Long.MAX_VALUE;
        }
        store.setSnoozeUntil(notificationTime);
        if (action != null) {
            action.call();
        }
    }

    public void reset() {
        store.setSnoozeUntil(0);
        if (action != null) {
            action.call();
        }
    }

    public void setOnChangeAction(Action0 action) {
        this.action = action;
    }
}
