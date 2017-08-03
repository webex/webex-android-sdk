package com.cisco.spark.android.util;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.github.benoitdion.ln.Ln;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Provider;

public class UserAgentProvider implements Provider<String> {
    public static final String APP_NAME = "wx2-android";

    @Inject
    protected ApplicationInfo appInfo;
    @Inject
    protected AppVersionProvider appVersionProvider;
    @Inject
    protected TelephonyManager telephonyManager;
    @Inject
    protected ClassLoader classLoader;

    protected String userAgent;

    @Override
    public String get() {
        if (userAgent == null) {
            synchronized (UserAgentProvider.class) {
                if (userAgent == null) {
                    String tempUserAgent = String.format("%s/%s (Android %s; %s %s / %s %s; %s)",
                            APP_NAME,
                            appVersionProvider.getVersionName(),
                            Build.VERSION.RELEASE,
                            Strings.capitalize(Build.MANUFACTURER),
                            Strings.capitalize(Build.DEVICE),
                            Strings.capitalize(Build.BRAND),
                            Strings.capitalize(Build.MODEL),
                            Strings.capitalize(telephonyManager == null ? "not-found" : telephonyManager.getSimOperatorName())
                    );

                    final ArrayList<String> params = new ArrayList<String>();
                    params.add("preload=" + ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)); // Determine if this app was a preloaded app
                    params.add("locale=" + Locale.getDefault());

                    // http://stackoverflow.com/questions/2641111/where-is-android-os-systemproperties
                    try {
                        final Class systemProperties = classLoader.loadClass("android.os.SystemProperties");
                        @SuppressWarnings("unchecked")
                        final Method get = systemProperties.getMethod("get", String.class);
                        params.add("clientidbase=" + get.invoke(systemProperties, "ro.com.google.clientidbase"));
                    } catch (Exception ignored) {
                        Ln.d(ignored);
                    }

                    if (params.size() > 0) {
                        tempUserAgent += "[" + Strings.join(";", params) + "]";
                    }

                    userAgent = Strings.stripInvalidHeaderChars(tempUserAgent);
                }
            }
        }

        return userAgent;
    }

    public void invalidate() {
        userAgent = null;
    }
}
