package com.ciscowebex.androidsdk.phone.internal;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.cisco.wme.appshare.ScreenShareContext;
import com.github.benoitdion.ln.Ln;

public class ScreenShareForegroundService extends Service {
    public static final String SCREEN_SHARING_DATA = "screen_sharing_data";
    public static final String SCREEN_SHARING_NOTIFICATION = "screen_sharing_notification";
    public static final String SCREEN_SHARING_NOTIFICATION_ID = "screen_sharing_notification_id";
    private final ScreenShareContext.OnShareStoppedListener onShareStoppedListener = this::stopSelf;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Ln.d("Screen share service start");
        Notification notification = intent.getParcelableExtra(SCREEN_SHARING_NOTIFICATION);
        int notificationId = intent.getIntExtra(SCREEN_SHARING_NOTIFICATION_ID, 0);
        startForeground(notificationId, notification);
        Intent screenSharingIntent = intent.getParcelableExtra(SCREEN_SHARING_DATA);
        ScreenShareContext.getInstance().init(this, Activity.RESULT_OK, screenSharingIntent);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Ln.d("Screen share service create");
        ScreenShareContext.getInstance().registerCallback(onShareStoppedListener);
    }

    @Override
    public void onDestroy() {
        Ln.d("Screen share service destroy");
        ScreenShareContext.getInstance().unregisterCallback(onShareStoppedListener);
        ScreenShareContext.getInstance().finit();
        stopForeground(true);
        super.onDestroy();
    }
}
