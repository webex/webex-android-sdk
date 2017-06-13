package com.cisco.spark.android.app;

import android.accounts.*;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;

public class AndroidAccountManager implements com.cisco.spark.android.app.AccountManager {
    private final AccountManager delegate;

    public AndroidAccountManager(android.accounts.AccountManager delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressLint("MissingPermission")
    public Account[] getAccounts() {
        return delegate.getAccounts();
    }
}
