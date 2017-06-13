package com.cisco.spark.android.app;

import android.app.*;

public interface NotificationManager {
    void notify(int id, Notification notification);
    void notify(String tag, int id, Notification notification);
    void cancel(int id);
    void cancel(String tag, int id);
    void cancelAll();
}
