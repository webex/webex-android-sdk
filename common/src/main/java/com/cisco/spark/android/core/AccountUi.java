package com.cisco.spark.android.core;

import android.app.Activity;
import android.content.Context;

public interface AccountUi {
    void showHome(Activity activity);
    void logout(Context context, Activity parent, boolean isTeardown, boolean showAlert);
}
