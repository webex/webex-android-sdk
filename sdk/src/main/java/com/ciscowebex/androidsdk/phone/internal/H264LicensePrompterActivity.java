package com.ciscowebex.androidsdk.phone.internal;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.phone.Phone;
import com.github.benoitdion.ln.Ln;

public class H264LicensePrompterActivity extends Activity {

    public static H264LicensePrompter PROMPTER;
    public static CompletionHandler<Phone.H264LicenseAction> CALLBACK;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        if (PROMPTER == null) {
            Ln.d("No H264LicensePrompter in activity");
            finish();
            if (CALLBACK != null) {
                CALLBACK.onComplete(ResultImpl.success(Phone.H264LicenseAction.ACCEPT));
                CALLBACK = null;
            }
        }
        else {
            PROMPTER.showUI(new AlertDialog.Builder(this), result -> {
                finish();
                if (CALLBACK != null) {
                    CALLBACK.onComplete(result);
                    CALLBACK = null;
                }
            });
        }
    }

}
