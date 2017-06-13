package com.cisco.spark.android.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.text.MessageFormat;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

/**
 * Helper to show {@link Toast} notifications Inspired by https://github.com/kevinsawicki/wishlist/blob/master/lib/src/main/java/com/github/kevinsawicki/wishlist/Toaster.java
 */
public class Toaster {

    private static void show(final Context context, final int resId, final int duration) {
        if (context == null) {
            return;
        }

        if (Looper.getMainLooper() == Looper.myLooper()) {
            Toast.makeText(context, resId, duration).show();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, resId, duration).show();
                }
            });
        }
    }

    private static void show(final Context context, final String message, final int duration) {
        if (context == null) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }

        if (Looper.getMainLooper() == Looper.myLooper()) {
            Toast.makeText(context, message, duration).show();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, duration).show();
                }
            });
        }
    }


    private static void show(final Activity activity, final int resId, final int duration) {
        if (activity == null) {
            return;
        }

        final Context context = activity.getApplication();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, resId, duration).show();
            }
        });
    }

    private static void show(final Activity activity, final String message, final int duration) {
        if (activity == null) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }

        final Context context = activity.getApplication();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, message, duration).show();
            }
        });
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param resId
     */
    public static void showLong(final Activity activity, int resId) {
        show(activity, resId, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param activity
     * @param resId
     */
    public static void showShort(final Activity activity, final int resId) {
        show(activity, resId, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param message
     */
    public static void showLong(final Activity activity, final String message) {
        show(activity, message, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param activity
     * @param message
     */
    public static void showShort(final Activity activity, final String message) {
        show(activity, message, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param activity
     * @param message
     * @param args
     */
    public static void showLong(final Activity activity, final String message, final Object... args) {
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
    public static void showShort(final Activity activity, final String message, final Object... args) {
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
    public static void showLong(final Activity activity, final int resId, final Object... args) {
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
    public static void showShort(final Activity activity, final int resId, final Object... args) {
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
    public static void showLong(final Context context, final int resId) {
        show(context, resId, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration (must be called from
     * UI thread)
     *
     * @param context
     * @param resId
     */
    public static void showShort(final Context context, final int resId) {
        show(context, resId, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param context
     * @param message
     */
    public static void showLong(final Context context, final String message) {
        show(context, message, LENGTH_LONG);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_SHORT} duration
     *
     * @param context
     * @param message
     */
    public static void showShort(final Context context, final String message) {
        show(context, message, LENGTH_SHORT);
    }

    /**
     * Show message in {@link Toast} with {@link Toast#LENGTH_LONG} duration
     *
     * @param context
     * @param message
     * @param args
     */
    public static void showLong(final Context context, final String message, final Object... args) {
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
    public static void showShort(final Context context, final String message, final Object... args) {
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
    public static void showLong(final Context context, final int resId, final Object... args) {
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
    public static void showShort(final Context context, final int resId, final Object... args) {
        if (context == null) {
            return;
        }

        String message = context.getString(resId);
        showShort(context, message, args);
    }
}
