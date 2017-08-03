package com.cisco.spark.android.util;

import android.content.Context;

import com.cisco.spark.android.R;
import com.github.benoitdion.ln.Ln;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private DateUtils() {}
    private static final int DAYS_IN_A_WEEK = 7;
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;

    private static final long SEVEN_DAYS_IN_MILLIS = TimeUnit.DAYS.toMillis(7);

    public static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        // Force UTC timezone so we can easily format the dates with the Z the same way the server does.
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    public static String formatUTCDateString(Date date) {
        if (date == null) {
            return "";
        }

        DateFormat iso8601Format = buildIso8601Format();
        return iso8601Format.format(date);
    }

    public static boolean isSameDay(final Date date1, final Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isSameWeek(final Calendar calendar1, final Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                Math.abs(calendar1.get(Calendar.DAY_OF_YEAR) - calendar2.get(Calendar.DAY_OF_YEAR)) < DAYS_IN_A_WEEK;
    }

    public static boolean isTomorrow(final Date date1, final Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        cal1.set(Calendar.HOUR, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);

        DateTime dateTime1 = new DateTime(date1).withTimeAtStartOfDay();
        DateTime dateTime2 = new DateTime(date2).withTimeAtStartOfDay();

        Days daysBetween = Days.daysBetween(dateTime1, dateTime2);

        return daysBetween == Days.ONE;
    }

    public static boolean isBeforeNoon(final Date date) {
        DateTime dateTime = new DateTime(date);
        return dateTime.getHourOfDay() < 12;
    }

    public static long getTimestampDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DATE, -days);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    public static CharSequence formatTime(Context context, long time) {
        long now = System.currentTimeMillis();
        long delta = now - time;
        if (delta < 2000) {
            return context.getResources().getString(R.string.now);
        } else if (android.text.format.DateUtils.isToday(time)) {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_SHOW_TIME);
        } else if (delta < SEVEN_DAYS_IN_MILLIS) {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_SHOW_TIME | android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY | android.text.format.DateUtils.FORMAT_ABBREV_WEEKDAY);
        } else {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_SHOW_TIME | android.text.format.DateUtils.FORMAT_SHOW_DATE);
        }
    }

    public static CharSequence formatTimeToNumericDate(Context context, long time) {
        long now = System.currentTimeMillis();
        long delta = now - time;
        if (delta < 2000) {
            return context.getResources().getString(R.string.now);
        } else if (android.text.format.DateUtils.isToday(time)) {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_SHOW_TIME);
        } else if (delta < SEVEN_DAYS_IN_MILLIS) {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY);
        } else {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_NUMERIC_DATE | android.text.format.DateUtils.FORMAT_SHOW_YEAR);
        }
    }

    public static CharSequence formatDuration(Context context, long millis) {
        if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);

            // TODO need to combine hours and minutes

            return context.getResources().getQuantityString(R.plurals.duration_hours, hours, hours);
        } else if (millis >= MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            return context.getResources().getQuantityString(R.plurals.duration_minutes, minutes, minutes);
        } else {
            final int seconds = (int) ((millis + 500) / SECOND_IN_MILLIS);
            return context.getResources().getQuantityString(R.plurals.duration_seconds, seconds, seconds);
        }
    }

    public static CharSequence formatInactiveTime(Context context, Date lastSeen) {
        Date currentDate = new Date();
        long millis = currentDate.getTime() - lastSeen.getTime();

        Ln.d("inactive duration - %d.", millis);

        if (millis >= DAY_IN_MILLIS) {
            final int days = (int) ((millis + 43200000) / DAY_IN_MILLIS);

            if (days == 1) {
                return context.getString(R.string.presence_status_yesterday);
            } else if (days < 7) {
                return context.getString(R.string.presence_status_active_day, android.text.format.DateUtils.formatDateTime(context, lastSeen.getTime(), android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY));
            } else {
                return context.getString(R.string.presence_status_active_on, android.text.format.DateUtils.formatDateTime(context, lastSeen.getTime(), android.text.format.DateUtils.FORMAT_NUMERIC_DATE | android.text.format.DateUtils.FORMAT_SHOW_YEAR));
            }
        } else if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);
            return context.getResources().getQuantityString(R.plurals.presence_status_inactive_hours, hours, hours);
        } else if (millis > MINUTE_IN_MILLIS) {
            int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            if (minutes > 10) {
                minutes = (int) ((minutes / 60.0) * 12);
                minutes = minutes * 5;
            }

            return context.getResources().getQuantityString(R.plurals.presence_status_inactive_minutes, minutes, minutes);
        } else {
            return context.getString(R.string.presence_status_active);
        }
    }

    public static CharSequence formatTimeForActivityDayMarker(Context context, long activityPublishTime) {
        Calendar publishedTime = Calendar.getInstance();
        publishedTime.setTimeInMillis(activityPublishTime);
        Calendar now = Calendar.getInstance();

        if (now.get(Calendar.YEAR) == publishedTime.get(Calendar.YEAR)) {
            switch (now.get(Calendar.DAY_OF_YEAR) - publishedTime.get(Calendar.DAY_OF_YEAR)) {
                case 0:  // Today
                    return context.getResources().getString(R.string.day_marker_today);
                case 1:  // Yesterday
                    return context.getResources().getString(R.string.day_marker_yesterday);
                default: // Before Yesterday
                    return android.text.format.DateUtils.formatDateTime(context, activityPublishTime, android.text.format.DateUtils.FORMAT_SHOW_DATE);
            }
        } else {
            return android.text.format.DateUtils.formatDateTime(context, activityPublishTime, android.text.format.DateUtils.FORMAT_SHOW_DATE | android.text.format.DateUtils.FORMAT_SHOW_YEAR).toUpperCase(Locale.getDefault());
        }
    }

    public static boolean isTimestampOverdue(long timestamp, long overdueTime) {
        return System.currentTimeMillis() - timestamp > overdueTime;
    }
}
