package com.cisco.spark.android.notification;

import android.app.*;

import com.github.benoitdion.ln.Ln;

public class AndroidNotificationManager implements com.cisco.spark.android.app.NotificationManager {
    private final android.app.NotificationManager delegate;

    public AndroidNotificationManager(android.app.NotificationManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void notify(int id, Notification notification) {
        delegate.notify(id, notification);
    }

    @Override
    public void notify(String tag, int id, Notification notification) {
        delegate.notify(tag, id, notification);
    }

    @Override
    public void cancel(int id) {
        delegate.cancel(id);
    }

    @Override
    public void cancel(String tag, int id) {
        Ln.v("cancel notification tag:%s, id:%d", tag, id);
        delegate.cancel(tag, id);
    }

    @Override
    public void cancelAll() {
        delegate.cancelAll();
    }
}
