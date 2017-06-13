package com.cisco.spark.android.util;

import android.Manifest;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Debug;
import android.support.v4.content.ContextCompat;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.content.ContentLoader;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.DatabaseHelper;
import com.cisco.spark.android.sync.LocalKeyStoreManager;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.FeatureToggle;
import com.cisco.spark.android.wdm.Features;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class SystemUtils {

    private static final String APP_INFO_FILE_NAME = "sysinfo.txt";
    private static final float ONE_MEGABYTE = 1024 * 1024;

    public static File generateSysInfoFile(File logLocation, Context context, ApiTokenProvider tokenProvider, MediaEngine mediaEngine,
                                           Settings settings, ContentLoader contentLoader, DeviceRegistration deviceRegistration,
                                           TrackingIdGenerator trackingIdGenerator, DiagnosticManager diagnosticManager) {
        File versionFile = null;
        try {
            versionFile = new File(logLocation + File.separator + APP_INFO_FILE_NAME);

            PrintWriter writer = new PrintWriter(new FileOutputStream(versionFile, false));
            generateUserInfo(writer, tokenProvider, deviceRegistration, diagnosticManager);
            generateAppInfo(writer, logLocation, context, mediaEngine, contentLoader, trackingIdGenerator);
            generateSystemInfo(writer, context);
            generatePermissionsInfo(writer, context);
            generateUserSettingsInfo(writer, settings, deviceRegistration);
            generateDevConsoleInfo(writer, settings, deviceRegistration);
            generateStorageUseInfo(writer, context);

            try {
                generateDbInfo(writer, context);
            } catch (SQLiteException e) {
                Ln.e("Unable to write Database logs");
            }

            generateThreadDump(writer);
            writer.close();
            return versionFile;

        } catch (FileNotFoundException e) {
            Ln.e(e, "Error writing to " + APP_INFO_FILE_NAME);
        }
        return versionFile;
    }

    private static void generateUserInfo(PrintWriter writer, ApiTokenProvider tokenProvider, DeviceRegistration deviceRegistration, DiagnosticManager diagnosticManager) {
        String userName = "unknown";
        String deviceId = "unknown";
        String deviceUrl = "unknown";
        String mercuryUri = "unknown";
        String dynamicLoggingPIN = "unknown";

        try {
            if (tokenProvider.isAuthenticated()) {
                AuthenticatedUser user = tokenProvider.getAuthenticatedUser();
                userName = user.getDisplayName();
                deviceId = deviceRegistration.getId();
                deviceUrl = deviceRegistration.getUrl().toString();
                mercuryUri = deviceRegistration.getWebSocketUrl().toString();
                dynamicLoggingPIN = diagnosticManager.getPin();
            }
        } catch (Exception e) {
            Ln.e(e, "Failed to get user information");
        }

        writer.println("User Information:");
        writer.println("-----------------");
        writer.println("User name  : " + userName);
        writer.println("Device ID  : " + deviceId);
        writer.println("Device URL : " + deviceUrl);
        writer.println("Mercury URI: " + mercuryUri);
        writer.println("Server PIN : " + (dynamicLoggingPIN == null ? "none" : dynamicLoggingPIN));
        writer.println("");
    }

    private static void generateAppInfo(PrintWriter writer, File logLocation, Context context, MediaEngine mediaEngine, ContentLoader contentLoader, TrackingIdGenerator trackingIdGenerator) {
        try {
            String packageName = context.getPackageName();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            int processId = getAppProcessId(context, packageInfo.applicationInfo.processName);

            DateFormat dateFormat = DateUtils.buildIso8601Format();

            writer.println("Application Information:");
            writer.println("------------------------");
            writer.println("Tracking Id base: " + trackingIdGenerator.base());
            writer.println("Package name    : " + packageName);
            writer.println("Process name    : " + packageInfo.applicationInfo.processName);
            writer.println("Process ID      : " + Integer.toString(processId));
            writer.println("Version         : " + packageInfo.versionName);
            writer.println("WME version     : " + mediaEngine.getVersion());
            writer.println("Commit tag      : " + BuildConfig.GIT_COMMIT_SHA);
            writer.println("Installed       : " + dateFormat.format(packageInfo.firstInstallTime));
            writer.println("Last update     : " + dateFormat.format(packageInfo.lastUpdateTime));
            writer.println("Log location    : " + logLocation);
            writer.println("Content loc     : " + getContentLocation(contentLoader));
            writer.println("Memory usage    : " + getProcessMemoryInfo(context, processId));
            writer.println("Disk usage      : " + getAppStorageInfo(context));
            writer.println("");

        } catch (PackageManager.NameNotFoundException e) {
            Ln.e(e, "Error getting package info.");
        }
    }

    private static void generateSystemInfo(PrintWriter writer, Context context) {
        writer.println("System Information:");
        writer.println("-------------------");
        writer.println("Android version    : " + Build.VERSION.RELEASE);
        writer.println("Android build      : " + Build.DISPLAY);
        writer.println("Android SDK version: " + Build.VERSION.SDK_INT);
        writer.println("Manufacturer       : " + Build.MANUFACTURER);
        writer.println("Device model       : " + Build.MODEL);
        writer.println("Device time        : " + getDeviceTimeInfo());
        writer.println("Device timezone    : " + TimeZone.getDefault().getDisplayName(Locale.US));
        writer.println("CPU instruction set: " + Build.CPU_ABI);
        writer.println("Number of cores    : " + Runtime.getRuntime().availableProcessors());
        writer.println("Is rooted          : " + (isRooted() ? "Yes" : "No"));
        writer.println("Network status     : " + getNetworkStatus(context));
        writer.println("");
    }

    private static void generatePermissionsInfo(PrintWriter writer, Context context) {
        writer.println("Permissions:");
        writer.println("------------");
        printPermission(writer, context, Manifest.permission.RECORD_AUDIO);
        printPermission(writer, context, Manifest.permission.CAMERA);
        printPermission(writer, context, Manifest.permission.READ_PHONE_STATE);
        printPermission(writer, context, Manifest.permission.ACCESS_FINE_LOCATION);
        printPermission(writer, context, Manifest.permission.SYSTEM_ALERT_WINDOW);
        printPermission(writer, context, Manifest.permission.READ_CONTACTS);
        printPermission(writer, context, Manifest.permission.READ_CALENDAR);
        printPermission(writer, context, Manifest.permission.INTERNET);
        printPermission(writer, context, Manifest.permission.ACCESS_NETWORK_STATE);
        printPermission(writer, context, Manifest.permission.ACCESS_WIFI_STATE);
        printPermission(writer, context, Manifest.permission.BLUETOOTH);
        printPermission(writer, context, Manifest.permission.READ_EXTERNAL_STORAGE);
        printPermission(writer, context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        printPermission(writer, context, Manifest.permission.GET_TASKS);
        printPermission(writer, context, Manifest.permission.GET_ACCOUNTS);
        printPermission(writer, context, Manifest.permission.VIBRATE);
        printPermission(writer, context, Manifest.permission.WAKE_LOCK);
        printPermission(writer, context, Manifest.permission.MODIFY_AUDIO_SETTINGS);
        printPermission(writer, context, Manifest.permission.READ_SYNC_SETTINGS);
        printPermission(writer, context, Manifest.permission.WRITE_SYNC_SETTINGS);
        printPermission(writer, context, Manifest.permission.READ_SYNC_STATS);
        printPermission(writer, context, "android.permission.USE_CREDENTIALS");
        printPermission(writer, context, Manifest.permission.BATTERY_STATS);
        printPermission(writer, context, "com.google.android.c2dm.permission.RECEIVE");
        printPermission(writer, context, "com.cisco.wx2.android.permission.C2D_MESSAGE");
        writer.println("");
    }

    private static void printPermission(PrintWriter writer, Context context, String permission) {
        boolean hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        writer.println(String.format(Locale.US, "%-45s: %s", permission, hasPermission));
    }

    private static void generateUserSettingsInfo(PrintWriter writer, Settings settings, DeviceRegistration deviceRegistration) {
        writer.println("User settings:");
        writer.println("--------------");
        writer.println(String.format("Share Location Toggle: %s (feature enabled = %s)", settings.getLocationSharingEnabled(), deviceRegistration.getFeatures().isLocationSharingEnabled()));
        writer.println(String.format("Proximity Disabled: %s", deviceRegistration.getFeatures().hasUserDisabledProximityFeatures()));

        final boolean customNotificationsEnabled = deviceRegistration.getFeatures().isCustomNotificationsEnabled();
        writer.println(String.format("Custom Notifications Enabled: %s", customNotificationsEnabled));
        if (customNotificationsEnabled) {
            final FeatureToggle directMessages = deviceRegistration.getFeatures().getFeature(Features.FeatureType.USER, Features.USER_TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS);
            writer.println(String.format("Custom Notifications - 1:1: %s (%s)", directMessages.getBooleanVal(), directMessages.getLastModified()));
            final FeatureToggle messages = deviceRegistration.getFeatures().getFeature(Features.FeatureType.USER, Features.USER_TOGGLE_GROUP_MESSAGE_NOTIFICATIONS);
            writer.println(String.format("Custom Notifications - messages: %s (%s)", messages.getBooleanVal(), messages.getLastModified()));
            final FeatureToggle mentions = deviceRegistration.getFeatures().getFeature(Features.FeatureType.USER, Features.USER_TOGGLE_MENTION_NOTIFICATIONS);
            writer.println(String.format("Custom Notifications - @mentions: %s (%s)", mentions.getBooleanVal(), mentions.getLastModified()));
        }
        writer.println(String.format("Notification Sounds: %s", settings.isNotificationSoundEnabled()));
        writer.println(String.format("Notification Vibrations: %s", settings.isNotificationVibrateEnabled()));
        writer.println("");
    }

    private static void generateDevConsoleInfo(PrintWriter writer, Settings settings, DeviceRegistration deviceRegistration) {
        writer.println("Developer console:");
        writer.println("------------------");
        writer.println(String.format("Simulate Network Congestion: %s", settings.getNetworkCongestion()));
        writer.println(String.format("Linus Name: %s", settings.getLinusName()));
        writer.println(String.format("Custom Call Feature: %s", settings.getCustomCallFeature()));
        writer.println(String.format("Media Override IP Address: %s", settings.getMediaOverrideIpAddress()));
        writer.println(String.format("Audio Codec: %s", settings.getAudioCodec()));
        writer.println(String.format("Custom WDM URL: %s", settings.getCustomWdmUrl()));
        writer.println("");
        deviceRegistration.getFeatures().print(writer);
        writer.println("");
    }

    private static void generateStorageUseInfo(PrintWriter writer, Context context) {
        writer.println("Content Storage:");
        writer.println("------------------");

        try {
            DecimalFormat formatter = new DecimalFormat("#,###");
            long total = 0;

            for (ConversationContract.ContentDataCacheEntry.Cache cache : ConversationContract.ContentDataCacheEntry.Cache.values()) {
                long size = FileUtils.getFilesystemSize(ContentManager.getContentDirectory(context, cache));
                writer.println(cache.name() + " cache: " + formatter.format(size));
                total += size;
            }
            writer.println("Total: " + formatter.format(total));
        } catch (Exception e) {
            Ln.w(e);
            writer.println(e.getMessage());
        }
        writer.println("");
    }

    private static void generateDbInfo(PrintWriter writer, Context context) {
        writer.println("Database Info:");
        writer.println("------------------");

        try {
            DecimalFormat formatter = new DecimalFormat("#,###");

            File dbFile = context.getDatabasePath(DatabaseHelper.SERVICE_DB_NAME);
            if (dbFile.exists() && dbFile.canRead()) {
                writer.println(dbFile.getName() + ": " + formatter.format(dbFile.length()) + " bytes");
            }

            writer.println("");
            writer.println("Row Counts");
            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getPath(), LocalKeyStoreManager.getMasterPassword(), null, SQLiteDatabase.OPEN_READONLY);

            Cursor cursor = null;
            for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
                try {
                    String tablename = table[0].tablename();
                    cursor = db.query(tablename, new String[]{"count(*)"}, null, null, null, null, null);
                    if (cursor != null && cursor.moveToNext()) {
                        writer.println(tablename + ": " + formatter.format(cursor.getLong(0)));
                    }
                } finally {
                    if (cursor != null)
                        cursor.close();
                    cursor = null;
                }
            }
        } catch (Exception e) {
            writer.println(e.getMessage());
            Ln.w(e);
        }
        writer.println("");
    }

    private static void generateThreadDump(PrintWriter writer) {
        Set<Thread> runningThreads = Thread.getAllStackTraces().keySet();

        writer.println("Thread dump:");
        writer.println("------------");

        for (Thread thread : runningThreads) {
            try {
                writer.println(String.format(Locale.US, "Id: %d, %s %s", thread.getId(), thread.toString(), thread.getState().toString()));
                for (StackTraceElement element : thread.getStackTrace()) {
                    writer.println(element.toString());
                }
                writer.println("");
            } catch (Throwable e) {
                Ln.w("Failed including stacktrace in feedback because of " + e.getLocalizedMessage());
                writer.println("Call stack missing");
            }
        }
        writer.println("");
    }

    private static String getDeviceTimeInfo() {
        DateFormat dateFormat = DateUtils.buildIso8601Format();
        return dateFormat.format(new Date());
    }

    private static String getNetworkStatus(Context context) {
        String networkStatus;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if (activeNetwork.isConnectedOrConnecting()) {
                    networkStatus = "connected (" + activeNetwork.getTypeName() + ")";
                } else {
                    networkStatus = "no connection (state=" + activeNetwork.getDetailedState() + ")";
                }
            } else {
                networkStatus = "failed getting active network information";
            }
        } else {
            networkStatus = "failed getting connectivity service";
        }

        return networkStatus;
    }

    // Determining rooted status isn't (programmaticallly) straightforward,
    // so a few possible tests are tried
    public static boolean isRooted() {
        // check for 'test-keys' in the build info
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        // check if /system/app/Superuser.apk is present
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return true;
            }
        } catch (Exception e) {
            Ln.v(e);
        }

        // try executing commands
        return canExecuteCommand("/system/xbin/which su") ||
                canExecuteCommand("/system/bin/which su") ||
                canExecuteCommand("which su");
    }

    public static boolean hasLockScreen(Context context) {
        return ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardSecure();
    }

    private static boolean canExecuteCommand(String command) {
        boolean executed;
        try {
            Runtime.getRuntime().exec(command);
            executed = true;
        } catch (Exception e) {
            executed = false;
        }
        return executed;
    }

    private static int getAppProcessId(Context context, String processName) {
        int processId = 0;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo currentProcess : processList) {
            if (currentProcess.processName.equals(processName)) {
                processId = currentProcess.pid;
                break;
            }
        }

        return processId;
    }

    private static String getProcessMemoryInfo(Context context, int processId) {
        String result;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (processId > 0) {
            int[] pids = {processId};
            Debug.MemoryInfo[] mi = activityManager.getProcessMemoryInfo(pids);
            if (mi != null && mi.length == 1) {
                result = "total PSS = " + new DecimalFormat("#,###.##").format(mi[0].getTotalPss()) + " kB, ";
                result += "total shared dirty = " + new DecimalFormat("#,###.##").format(mi[0].getTotalPrivateDirty()) + " kB, ";
                result += "total private dirty = " + new DecimalFormat("#,###.##").format(mi[0].getTotalSharedDirty()) + " kB";
            } else {
                result = "error retrieving memory information";
            }
        } else {
            result = "invalid process ID specified (" + Integer.toString(processId) + ")";
        }

        return result;
    }

    private static String getAppStorageInfo(Context context) {
        // Get the parent folders for where internal and external "files" would be and
        // count size of all data under those folders
        float intFileSize = getFolderSize(context.getFilesDir().getParentFile());
        float extFileSize = getFolderSize(context.getExternalFilesDir(null).getParentFile());

        String intMB = new DecimalFormat("#,###.##").format(intFileSize / ONE_MEGABYTE);
        String extMB = new DecimalFormat("#,###.##").format(extFileSize / ONE_MEGABYTE);

        return String.format("internal = %s MB, external = %s MB", intMB, extMB);
    }

    private static long getFolderSize(File directory) {
        long length = 0;
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile())
                    length += file.length();
                else
                    length += getFolderSize(file);
            }
        }
        return length;
    }

    private static String getContentLocation(ContentLoader contentLoader) {
        if (contentLoader != null)
            return contentLoader.getContentDirectoryName();
        else
            return "unknown";
    }

    public static float getDistance(PointF pa, PointF pb) {
        if (pa == null || pb == null)
            return 0;

        float a = pa.x - pb.x;
        float b = pa.y - pb.y;

        return (float) Math.sqrt((a * a) + (b * b));
    }
}
