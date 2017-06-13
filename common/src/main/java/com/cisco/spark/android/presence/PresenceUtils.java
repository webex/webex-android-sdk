package com.cisco.spark.android.presence;

import android.content.Context;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import com.cisco.spark.android.R;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.Features;
import com.github.benoitdion.ln.Ln;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class PresenceUtils {
    public static void displayPresence(Context context, TextView textView, PresenceStatus status, Date lastSeen, Date expiration) {
        if (status == null) {
            textView.setVisibility(View.GONE);
            return;
        }

        CharSequence presenceText = formatUserPresence(context, status, lastSeen, expiration);

        textView.setText(presenceText);
        textView.setVisibility((status == null || status == PresenceStatus.PRESENCE_STATUS_UNKNOWN) ? View.GONE : View.VISIBLE);
    }

    public static CharSequence formatUserPresence(Context context, PresenceStatus status, Date lastSeen, Date expiration) {
        Ln.v("displayPresence - " + status + ", last seen - " + lastSeen);
        CharSequence text = null;

        switch(status) {
            case PRESENCE_STATUS_INACTIVE:
                text = DateUtils.formatInactiveTime(context, lastSeen);
                break;
            case PRESENCE_STATUS_ACTIVE:
                text = context.getString(R.string.presence_status_active);
                break;
            case PRESENCE_STATUS_DO_NOT_DISTURB:
                text = formatUntilString(context, R.string.do_not_disturb, R.string.dnd_time_set_until, expiration);
                break;
            case PRESENCE_STATUS_PENDING:
                text = context.getString(R.string.presence_status_pending);
                break;
            case PRESENCE_STATUS_OOO:
                text = formatUntilString(context, R.string.presence_status_ooo, R.string.presence_status_ooo_until, expiration);
                break;
            case PRESENCE_STATUS_UNKNOWN:
            default:
                break;
        }

        return text;
    }

    public static Date getExpireTime(int ttlInSeconds) {
        if (ttlInSeconds == -1) {
            return null;
        }

        DateTime dateTime = new DateTime().plusSeconds(ttlInSeconds);
        return dateTime.toDate();
    }

    public static boolean isPresenceStatusExpired(Date expireDate) {
        if (expireDate == null) {
            return true;
        }

        Date currentDate = new Date();

        if (expireDate == null) {
            return true;
        }

        Ln.d("isPresenceStatusExpired: %s", (currentDate.getTime() >= expireDate.getTime()));

        return currentDate.getTime() >= expireDate.getTime();
    }

    public static boolean shouldDisplayPresence(DeviceRegistration deviceRegistration) {
        Features features = deviceRegistration.getFeatures();

        return (features.isUserPresenceEnabled() && features.isAndroidUserPresenceEnabled() && features.isPersonalUserPresenceEnabled());
    }

    public static String formatUntilString(Context context, @StringRes int defaultString,  @StringRes int formatString, Date expiration) {
        Calendar calendar = new GregorianCalendar();
        Calendar today = new GregorianCalendar();

        if (expiration == null) {
            return context.getString(defaultString);
        }

        calendar.setTime(expiration);
        String formatedString = "";
        String timeString = android.text.format.DateUtils.formatDateTime(context, expiration.getTime(), android.text.format.DateUtils.FORMAT_SHOW_TIME);

        if (DateUtils.isSameDay(new Date(), expiration)) {
            formatedString = timeString;
        } else if (DateUtils.isTomorrow(new Date(), expiration)) {
            formatedString = String.format(Locale.getDefault(), "%s, %s", context.getString(R.string.tomorrow), timeString);
        } else if (DateUtils.isSameWeek(today, calendar)) {
            String dayofWeek =  new DateTime(expiration).dayOfWeek().getAsText();
            formatedString = String.format(Locale.getDefault(), "%s, %s", dayofWeek, timeString);
        } else {
            Date futureDate = new DateTime(expiration).plusDays(1).toDate();
            formatedString = android.text.format.DateUtils.formatDateTime(context, DateUtils.isBeforeNoon(expiration) ? expiration.getTime() : futureDate.getTime(), android.text.format.DateUtils.FORMAT_NUMERIC_DATE | android.text.format.DateUtils.FORMAT_SHOW_YEAR);
        }

        return context.getString(formatString, formatedString);
    }
}
