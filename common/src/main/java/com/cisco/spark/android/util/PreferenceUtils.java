package com.cisco.spark.android.util;

import android.preference.Preference;
import android.support.annotation.ColorInt;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

public class PreferenceUtils {

    private PreferenceUtils() {
    }

    /**
     * Set the title color of a Preference
     */
    public static void setTitleColor(Preference preference, @ColorInt int color) {
        CharSequence title = preference.getTitle();
        SpannableString newTitle = new SpannableString(title);
        newTitle.setSpan(new ForegroundColorSpan(color), 0, title.length(), 0);
        preference.setTitle(newTitle);
    }

}
