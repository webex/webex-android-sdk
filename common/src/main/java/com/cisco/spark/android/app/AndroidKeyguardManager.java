package com.cisco.spark.android.app;

public class AndroidKeyguardManager implements KeyguardManager {
    private final android.app.KeyguardManager delegate;

    public AndroidKeyguardManager(android.app.KeyguardManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean inKeyguardRestrictedInputMode() {
        return delegate.inKeyguardRestrictedInputMode();
    }
}
