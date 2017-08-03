package com.cisco.spark.android.util;

import android.content.Context;
import android.provider.Settings;
import android.support.v4.content.AsyncTaskLoader;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;

import java.util.Set;

public class TestUtils {
    private static Boolean isInstrumentation;
    private static final String INSTRUMENTATION_THREAD_NAME = "com.cisco.wx2.android.test.support.SquaredTestRunner";
    private static final String TEST_USER_EMAIL_DOMAIN = "example.com";
    private static boolean runningUnitTest;

    public static boolean isTestUser(ApiTokenProvider apiTokenProvider) {
        try {
            String currentUser = apiTokenProvider.getAuthenticatedUser().getEmail();
            return currentUser.endsWith(TEST_USER_EMAIL_DOMAIN);
        } catch (NotAuthenticatedException ignore) {
            return false;
        }
    }

    public static boolean isTestUser(String emailAddress) {
        return emailAddress.endsWith(TEST_USER_EMAIL_DOMAIN);
    }


    public static boolean isPreLaunchTest(Context context) {
        String testLabSetting = Settings.System.getString(context.getContentResolver(), "firebase.test.lab");
        if ("true".equals(testLabSetting)) {
            return true;
        }
        return false;
    }

    public static boolean isInstrumentation() {
        if (isInstrumentation == null) {
            isInstrumentation = false;

            Set<Thread> runningThreads = Thread.getAllStackTraces().keySet();

            for (Thread thread : runningThreads) {
                if (thread.getName().contains(INSTRUMENTATION_THREAD_NAME)) {
                    isInstrumentation = true;
                    break;
                }
            }
        }
        return isInstrumentation;
    }

    public static <T> void setUpdateThrottleIfNotTest(AsyncTaskLoader<T> loader, long delay) {
        if (!isInstrumentation()) {
            loader.setUpdateThrottle(delay);
        }
    }

    public static void setRunningUnitTest(boolean runningUnitTest) {
        TestUtils.runningUnitTest = runningUnitTest;
    }

    public static boolean isRunningUnitTest() {
        return runningUnitTest;
    }
}
