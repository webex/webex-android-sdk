package com.cisco.spark.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.v4.view.ViewCompat;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.benoitdion.ln.Ln;

import java.lang.reflect.Field;
import java.util.Locale;

public class UIUtils {
    // the minimum width for a tablet device
    private static final int MINIMUM_TABLET_WIDTH_DP = 600;

    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isTablet(Context context) {
        // StackOverflow's most common solution:
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static DeviceType getDeviceType(Context context) {
        if (Build.MODEL.toLowerCase(Locale.US).contains("kindle"))
            return DeviceType.kindleFire;

        return isTablet(context) ? DeviceType.tablet : DeviceType.phone;
    }

    public static void removeTransition(Activity activity) {
        if (activity != null) {
            activity.overridePendingTransition(0, 0);
        }
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId) + 1;
        }
        return result;
    }

    public static void alwaysUseActionBarOverflowMenu(Context context) {
        // Force showing of overflow menu (even if device has hardware menu button)
        try {
            ViewConfiguration config = ViewConfiguration.get(context);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
    }

    @SuppressLint("NewApi")
    public static boolean canDrawOnTopOfApps(Context context) {
        if (hasMarshmallow()) {
            return Settings.canDrawOverlays(context);
        } else {
            return true;
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static boolean isInLandscapeConfiguration(Context context) {
        final int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isLandscapeOrientation(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        boolean result = (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);
        return result;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasLollipopMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    public static boolean isRTL() {
        return isRTL(Locale.getDefault());
    }

    /**
     * Using ViewCompat we can figure out if a view has RTL layout or not rather than using locale
     * which does not work with the force RTL setting
     *
     * @param view a view to deduct RTL configuration from
     */
    public static boolean isRTL(View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * This won't return true unless your locale is set to a RTL language so for debugging, make it return true all the time
     * The force RTL setting will not report true
     * @param locale
     */
    public static boolean isRTL(Locale locale) {
        try {
            final int directionality = Character.getDirectionality(locale.getDisplayName().charAt(0));
            return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
        } catch (StringIndexOutOfBoundsException e) {
            Ln.e("Locale displayName not provided\n" + e.getMessage());
            return false;
        }
    }

    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView textView, int start, int top, int end, int bottom) {
        if (isRTL()) {
            textView.setCompoundDrawablesWithIntrinsicBounds(end, top, start, bottom);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView textView, @DrawableRes int[] d) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(textView, d[0], d[1], d[2], d[3]);
    }

    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView textView, Drawable start, Drawable top, Drawable end, Drawable bottom) {
        if (isRTL()) {
            textView.setCompoundDrawablesWithIntrinsicBounds(end, top, start, bottom);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    public static Drawable[] getCompoundDrawablesRelative(TextView view) {
        Drawable[] ret = new Drawable[4];
        ret = view.getCompoundDrawables();
        if (isRTL()) {
            Drawable temp = ret[0];
            ret[0] = ret[2];
            ret[2] = temp;
        }
        return ret;
    }

    public static int[] getPaddingRelative(TextView textView) {
        int[] ret = new int[4];
        ret[1] = textView.getPaddingTop();
        ret[3] = textView.getPaddingBottom();

        if (isRTL()) {
            ret[0] = textView.getPaddingRight();
            ret[2] = textView.getPaddingLeft();
        } else {
            ret[0] = textView.getPaddingLeft();
            ret[2] = textView.getPaddingRight();
        }

        return ret;
    }

    public static View setPaddingRelative(View view, int start, int top, int end, int bottom) {
        if (isRTL()) {
            view.setPadding(end, top, start, bottom);
        } else {
            view.setPadding(start, top, end, bottom);
        }
        return view;
    }

    public static RelativeLayout.LayoutParams setMarginsRelative(RelativeLayout.LayoutParams params, int start, int top, int end, int bottom) {
        if (isRTL()) {
            params.setMargins(end, top, start, bottom);
        } else {
            params.setMargins(start, top, end, bottom);
        }
        return params;
    }

    public static LinearLayout.LayoutParams setMarginsRelative(LinearLayout.LayoutParams params, int start, int top, int end, int bottom) {
        if (isRTL()) {
            params.setMargins(end, top, start, bottom);
        } else {
            params.setMargins(start, top, end, bottom);
        }
        return params;
    }

    public static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static boolean autoRotationEnabled(Context context) {
        return (Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
    }
}
