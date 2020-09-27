package com.ciscowebex.androidsdk.utils;

import android.os.Build;
import com.ciscowebex.androidsdk.Webex;
import me.helloworld.utils.Strings;

import java.util.regex.Pattern;

public class UserAgent {

    public static final String value = Pattern.compile("[^\\x20-\\x7E]").matcher(String.format("%s/%s (Android %s; %s %s / %s %s;)",
            Webex.APP_NAME, Webex.APP_VERSION,
            Build.VERSION.RELEASE,
            Strings.capitalize(Build.MANUFACTURER),
            Strings.capitalize(Build.DEVICE),
            Strings.capitalize(Build.BRAND),
            Strings.capitalize(Build.MODEL)
    )).replaceAll("");
}
