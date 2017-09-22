package com.ciscospark.androidsdk.phone.internal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: zhiyuliu
 * Date: 15/09/2017
 * Time: 6:42 PM
 */

public class H264LicensePrompter {

	private static final String TAG = H264LicensePrompter.class.getSimpleName();
	
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
					Log.i(TAG, "Video license has been activated");
					setVideoLicenseActivated(true);
					handler.onComplete(true);
				}
			});
			
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.i(TAG, "Video license has not been activated");
					dialog.cancel();
					handler.onComplete(false);
				}
			});
			
			builder.setNeutralButton("View License", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.i(TAG, "Video license opened for viewing");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.openh264.org/BINARY_LICENSE.txt"));
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
