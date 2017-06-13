package com.cisco.spark.android.contacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class DummyAuthenticatorService extends Service {

    private AccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new AccountAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
