package com.cisco.spark.android.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.Toast;

import com.cisco.spark.android.sdk.SdkClient;

import java.text.MessageFormat;

import static android.widget.Toast.*;

/**
 * Helper to show {@link Toast} notifications Inspired by https://github.com/kevinsawicki/wishlist/blob/master/lib/src/main/java/com/github/kevinsawicki/wishlist/Toaster.java
 */
public class Toaster {

    private volatile boolean enabled;

    public Toaster(SdkClient sdkClient) {
        this.enabled = sdkClient.toastsEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void show(final Context context, final int resId, final int duration) {

        if (context == null || !enabled) {
            return;
        }

        if (Looper.getMainLooper() == Looper.myLooper()) {
            Toast.makeText(context, resId, duration).show();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, resId, duration).show());
        }
    }

    private void show(final Context context, final String message, final int duration) {

        if (context == null || !enabled) {
            return;
        }

        if (TextUtils.isEmpty(message)) {
            return;
        }

        if (Looper.getMainLooper() == Looper.myLooper()) {
            Toast.makeText(context, message, duration).show();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, duration).show());
        }
    }


    private void show(final Activity activity, final @StringRes int resId, final int duration) {

        if (activity == null || !enabled) {
            return;
        }

        final Context context = activity.getApplication();
        activity.runOnUiThread(() -> Toast.makeText(context, resId, duration).show());
    }

    private void show(final Activity activity, final String message, final int duration) {

        if (activity == null || !enabled) {
            return;
        }

        if (TextUtils.isEmpty(message)) {
            return;
        }

        final Context context = activity.getApplication();
        activity.runOnUiThread(() -> Toast.makeText(context, message, duration).show());
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param resId
     */
    public void showLong(final Activity activity, @StringRes int resId) {
        show(activity, resId, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param activity
     * @param resId
     */
    public void showShort(final Activity activity, final @StringRes int resId) {
        show(activity, resId, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param message
     */
    public void showLong(final Activity activity, final String message) {
        show(activity, message, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param activity
     * @param message
     */
    public void showShort(final Activity activity, final String message) {
        show(activity, message, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param message
     * @param args
     */
    public void showLong(final Activity activity, final String message, final Object... args) {
        String formatted = MessageFormat.format(message, args);
        show(activity, formatted, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param activity
     * @param message
     * @param args
     */
    public void showShort(final Activity activity, final String message, final Object... args) {
        String formatted = MessageFormat.format(message, args);
        show(activity, formatted, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param resId
     * @param args
     */
    public void showLong(final Activity activity, final @StringRes int resId, final Object... args) {
        if (activity == null) {
            return;
        }

        String message = activity.getString(resId);
        showLong(activity, message, args);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param activity
     * @param resId
     * @param args
     */
    public void showShort(final Activity activity, final @StringRes int resId, final Object... args) {
        if (activity == null) {
            return;
        }

        String message = activity.getString(resId);
        showShort(activity, message, args);
    }


    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration (must be called from UI
     * thread)
     *
     * @param context
     * @param resId
     */
    public void showLong(final Context context, final @StringRes int resId) {
        show(context, resId, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration (must be called from
     * UI thread)
     *
     * @param context
     * @param resId
     */
    public void showShort(final Context context, final @StringRes int resId) {
        show(context, resId, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param context
     * @param message
     */
    public void showLong(final Context context, final String message) {
        show(context, message, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param context
     * @param message
     */
    public void showShort(final Context context, final String message) {
        show(context, message, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param context
     * @param message
     * @param args
     */
    public void showLong(final Context context, final String message, final Object... args) {
        String formatted = MessageFormat.format(message, args);
        show(context, formatted, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param context
     * @param message
     * @param args
     */
    public void showShort(final Context context, final String message, final Object... args) {
        String formatted = MessageFormat.format(message, args);
        show(context, formatted, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param context
     * @param resId
     * @param args
     */
    public void showLong(final Context context, final @StringRes int resId, final Object... args) {
        if (context == null) {
            return;
        }

        String message = context.getString(resId);
        showLong(context, message, args);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param context
     * @param resId
     * @param args
     */
    public void showShort(final Context context, final @StringRes int resId, final Object... args) {
        if (context == null) {
            return;
        }

        String message = context.getString(resId);
        showShort(context, message, args);
    }
}
