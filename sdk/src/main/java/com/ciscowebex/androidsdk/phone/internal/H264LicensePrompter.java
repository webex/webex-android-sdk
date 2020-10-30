/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.phone.internal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.Settings;
import com.ciscowebex.androidsdk.phone.Phone;
import com.github.benoitdion.ln.Ln;

public class H264LicensePrompter {

    public static final String LICENSE_TEXT = "To enable video calls, activate a free video license (H.264 AVC) from Cisco. By selecting 'Activate', you accept the Cisco End User License Agreement and Notices.";

    public static final String LICENSE_URL = "http://www.openh264.org/BINARY_LICENSE.txt";

    H264LicensePrompter() {
    }

    void check(@NonNull Context context, @Nullable AlertDialog.Builder builder, @NonNull CompletionHandler<Phone.H264LicenseAction> handler) {
        if (isVideoLicenseActivated() || isVideoLicenseActivationDisabled()) {
            handler.onComplete(ResultImpl.success(Phone.H264LicenseAction.ACCEPT));
        }
        else if (builder == null) {
            H264LicensePrompterActivity.PROMPTER = this;
            H264LicensePrompterActivity.CALLBACK = handler;
            final Intent intent = new Intent(context, H264LicensePrompterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            context.startActivity(intent);
        }
        else {
            showUI(builder, handler);
        }
    }

    void showUI(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Phone.H264LicenseAction> handler) {
        Context context = builder.getContext();
        builder.setTitle("Activate License");
        builder.setMessage(LICENSE_TEXT);
        builder.setPositiveButton("Activate", (dialog, which) -> {
            Ln.i("Video license has been activated");
            setVideoLicenseActivated(true);
            handler.onComplete(ResultImpl.success(Phone.H264LicenseAction.ACCEPT));
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Ln.i("Video license has not been activated");
            dialog.cancel();
            handler.onComplete(ResultImpl.success(Phone.H264LicenseAction.DECLINE));
        });

        builder.setNeutralButton("View License", (dialog, which) -> {
            Ln.i("Video license opened for viewing");
            dialog.cancel();
            handler.onComplete(ResultImpl.success(Phone.H264LicenseAction.VIEW_LICENSE));
        });
        AlertDialog diag = builder.create();
        diag.show();
    }

    boolean isVideoLicenseActivationDisabled() {
        return Settings.shared.getKeep("isVideoLicenseActivationDisabledKey", false);
    }

    void setVideoLicenseActivationDisabled(boolean disabled) {
        Settings.shared.storeKeep("isVideoLicenseActivationDisabledKey", disabled);
    }

    void reset() {
        setVideoLicenseActivationDisabled(false);
        setVideoLicenseActivated(false);
    }

    private boolean isVideoLicenseActivated() {
        return Settings.shared.getKeep("isVideoLicenseActivatedKey", false);
    }

    private void setVideoLicenseActivated(boolean activated) {
        Settings.shared.storeKeep("isVideoLicenseActivatedKey", activated);
    }
}
