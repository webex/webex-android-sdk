package com.ciscospark.androidsdk.phone;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class CallOption {

    public static CallOption audioOnly() {
        return new CallOption(null, null);
    }

    public static CallOption audioVideo(@NonNull View localView, @NonNull View remoteView) {
        return new CallOption(localView, remoteView);
    }

    private View _remoteView;

    private View _localView;

    private CallOption(@Nullable View localView, @Nullable  View remoteView) {
        _localView = localView;
        _remoteView = remoteView;
    }

    public boolean hasVideo() {
        return _localView != null || _remoteView != null;
    }

    public View getRemoteView() {
        return _remoteView;
    }

    public View getLocalView() {
        return _localView;
    }
}
