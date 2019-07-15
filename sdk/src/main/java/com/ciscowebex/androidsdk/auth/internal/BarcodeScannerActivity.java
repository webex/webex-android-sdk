package com.ciscowebex.androidsdk.auth.internal;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ciscowebex.androidsdk.auth.OAuthQRCodeAuthenticator;

/**
 * An Activity agent to launch third party Barcode scanner Application
 */
public class BarcodeScannerActivity extends Activity {
    private static final String TAG = BarcodeScannerActivity.class.getSimpleName();

    public static final String BARCODE_REQUEST_CODE = "barcode_request_code";
    public static final String BARCODE_SCAN_ACTION = "barcode_scan_action";
    public static final String BARCODE_SCAN_RESULT = "barcode_scan_result";

    public static final int REQUEST_BARCODE_SCANNER = 1992;

    private int _requestCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getStringExtra(BARCODE_SCAN_ACTION);
        _requestCode = getIntent().getIntExtra(BARCODE_REQUEST_CODE, REQUEST_BARCODE_SCANNER);
        Log.d(TAG, "action: " + action + " requestCode: " + _requestCode);
        Intent intent = new Intent(action);
        try {
            startActivityForResult(intent, _requestCode);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Activity Not Found");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "requestCode: " + requestCode + " resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (_requestCode == requestCode) {
            String result = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    result = data.getStringExtra(getIntent().getStringExtra(BARCODE_SCAN_RESULT));
                }
                OAuthQRCodeAuthenticator.barcodeAuthorize(result);
            } else {
                Log.i(TAG, "User cancelled barcode scanning");
            }
        }
        finish();
    }
}
