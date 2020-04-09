/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.utils;

import android.content.Context;
import com.github.benoitdion.ln.Ln;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.text.format.DateUtils.*;
import static android.text.format.DateUtils.FORMAT_12HOUR;

public class DateUtils {
    private DateUtils() {
    }

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

    public static String getTimeDifference(long endTimeMillis, long startTimeMillis) {
        long millis = endTimeMillis - startTimeMillis;
        double seconds = millis / 1000.0;
        int minutes = (int) (seconds / 60.0);
        double minPlusSecondsSeconds = seconds - (minutes * 60);
        return String.format(Locale.US, "%d min %1.2f secs (%1.2f secs) [$PERFTRACE.%s]", minutes, minPlusSecondsSeconds, seconds, millis > 500 ? "WARN" : "OK");
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
            return "Now";
        } else if (android.text.format.DateUtils.isToday(time)) {
            return android.text.format.DateUtils.formatDateTime(context, time, FORMAT_SHOW_TIME);
        } else if (delta < SEVEN_DAYS_IN_MILLIS) {
            return android.text.format.DateUtils.formatDateTime(context, time, FORMAT_SHOW_TIME | FORMAT_SHOW_WEEKDAY | android.text.format.DateUtils.FORMAT_ABBREV_WEEKDAY);
        } else {
            return android.text.format.DateUtils.formatDateTime(context, time, FORMAT_SHOW_TIME | FORMAT_SHOW_DATE);
        }
    }

    public static CharSequence formatTimeToNumericDate(Context context, long time) {
        long now = System.currentTimeMillis();
        long delta = now - time;
        if (delta < 2000) {
            return "Now";
        } else if (android.text.format.DateUtils.isToday(time)) {
            return android.text.format.DateUtils.formatDateTime(context, time, FORMAT_SHOW_TIME);
        } else if (delta < SEVEN_DAYS_IN_MILLIS) {
            return android.text.format.DateUtils.formatDateTime(context, time, FORMAT_SHOW_WEEKDAY);
        } else {
            return android.text.format.DateUtils.formatDateTime(context, time, android.text.format.DateUtils.FORMAT_NUMERIC_DATE | FORMAT_SHOW_YEAR);
        }
    }

    public static CharSequence formatDuration(Context context, long millis) {
        if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);

            // TODO need to combine hours and minutes

            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (millis >= MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            final int seconds = (int) ((millis + 500) / SECOND_IN_MILLIS);
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }

    public static CharSequence formatTimeForRecordingTime(Context context, Date recordingDate) {
        return android.text.format.DateUtils.formatDateTime(context, recordingDate.getTime(),
                FORMAT_SHOW_WEEKDAY
                        | FORMAT_SHOW_DATE
                        | FORMAT_SHOW_YEAR
                        | FORMAT_SHOW_TIME
                        | FORMAT_12HOUR);
    }

    public static boolean isTimestampOverdue(long timestamp, long overdueTime) {
        return System.currentTimeMillis() - timestamp > overdueTime;
    }

    public static Date maxDate(Date date1, Date date2) {
        if (date1 == null)
            return date2;
        if (date2 == null)
            return date1;
        if (date1.after(date2))
            return date1;
        return date2;
    }

    public static Date addMinutesToDate(int minutes, Date date) {
        long currentMillis = date.getTime();
        return new Date(currentMillis + TimeUnit.MINUTES.toMillis(minutes));
    }
}
