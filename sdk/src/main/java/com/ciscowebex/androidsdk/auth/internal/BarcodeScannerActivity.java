package com.ciscowebex.androidsdk.auth.internal;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.auth.OAuthQRCodeAuthenticator;
import com.github.benoitdion.ln.Ln;

public class BarcodeScannerActivity extends Activity {
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
        Ln.d("action: " + action + " requestCode: " + _requestCode);
        Intent intent = new Intent(action);
        try {
            startActivityForResult(intent, _requestCode);
        } catch (ActivityNotFoundException e) {
            Ln.e("Activity Not Found");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Ln.d("requestCode: " + requestCode + " resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (_requestCode == requestCode) {
            String result = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    result = data.getStringExtra(getIntent().getStringExtra(BARCODE_SCAN_RESULT));
                }
                OAuthQRCodeAuthenticator.barcodeAuthorize(result);
            } else {
                Ln.i("User cancelled barcode scan");
            }
        }
        finish();
    }
}
