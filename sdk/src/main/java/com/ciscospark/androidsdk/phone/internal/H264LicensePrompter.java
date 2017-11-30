/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.phone.internal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.github.benoitdion.ln.Ln;

/**
 * Created with IntelliJ IDEA.
 * User: zhiyuliu
 * Date: 15/09/2017
 * Time: 6:42 PM
 */

public class H264LicensePrompter {
	
	public interface CompletionHandler<T> {
		void onComplete(T result);
	}
	
	private SharedPreferences _preferences;
	
	H264LicensePrompter(SharedPreferences preferences) {
		_preferences = preferences; 
	}
	
	String getLicense() {
		return "To enable video calls, activate a free video license (H.264 AVC) from Cisco. By selecting 'Activate', you accept the Cisco End User License Agreement and Notices.";
	}
	
	String getLicenseURL() {
		return "http://www.openh264.org/BINARY_LICENSE.txt";
	}
	
	void check(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Boolean> handler) {
		if (isVideoLicenseActivated() || isVideoLicenseActivationDisabled()) {
			handler.onComplete(true);
		}
		else {
			Context context = builder.getContext();
			builder.setTitle("Activate License");
			builder.setMessage(getLicense());
			builder.setPositiveButton("Activate", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Ln.i("Video license has been activated");
					setVideoLicenseActivated(true);
					handler.onComplete(true);
				}
			});
			
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Ln.i("Video license has not been activated");
					dialog.cancel();
					handler.onComplete(false);
				}
			});
			
			builder.setNeutralButton("View License", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Ln.i("Video license opened for viewing");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getLicenseURL()));
					context.startActivity(browserIntent);
					dialog.cancel();
					handler.onComplete(false);
				}
			});
			AlertDialog diag = builder.create();
			diag.show();
		}
	}

	boolean isVideoLicenseActivationDisabled() {
		return _preferences.getBoolean("isVideoLicenseActivationDisabledKey", false);
	}

	void setVideoLicenseActivationDisabled(boolean disabled) {
		_preferences.edit().putBoolean("isVideoLicenseActivationDisabledKey", disabled).apply();
	}
	
	void reset() {
		setVideoLicenseActivationDisabled(false);
		setVideoLicenseActivated(false);
	}
	
	private boolean isVideoLicenseActivated() {
		return _preferences.getBoolean("isVideoLicenseActivatedKey", false);
	}
	
	private void setVideoLicenseActivated(boolean activated) {
		_preferences.edit().putBoolean("isVideoLicenseActivatedKey", activated).apply();
	}
}
