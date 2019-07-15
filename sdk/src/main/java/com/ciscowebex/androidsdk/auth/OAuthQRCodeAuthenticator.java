package com.ciscowebex.androidsdk.auth;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cisco.spark.android.core.Injector;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.internal.BarcodeScannerActivity;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk_commlib.AfterInjected;

import javax.inject.Inject;

/**
 * An <a href="https://oauth.net/2/">OAuth</a> based authentication with a barcode scanner application
 * Note: require a third party barcode scanner application
 */
public class OAuthQRCodeAuthenticator implements Authenticator {
    private static final String TAG = OAuthQRCodeAuthenticator.class.getSimpleName();

    private static OAuthAuthenticator _authenticator;
    private static CompletionHandler<Void> _authorizeCallback;
    private Intent _intent;

    private int requestCode;
    private String actionBarcode;
    private String scanResult;

    @Inject
    Injector _injector;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId      the OAuth client id
     * @param clientSecret  the OAuth client secret
     * @param scope         space-separated string representing which permissions the application needs
     * @param redirectUri   the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @param actionBarcode the intent action for barcode scanner application
     * @param scanResult    the extra intent to get the scan result
     * @see <a href="https://developer.webex.com/authentication.html">Cisco Webex Integration</a>
     * @since 2.2
     */
    public OAuthQRCodeAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri, @NonNull String actionBarcode, @NonNull String scanResult) {
        _authenticator = new OAuthAuthenticator(clientId, clientSecret, scope, redirectUri);
        _intent = new Intent();
        this.requestCode = BarcodeScannerActivity.REQUEST_BARCODE_SCANNER;
        this.actionBarcode = actionBarcode;
        this.scanResult = scanResult;
    }

    /**
     * Brings up a QR Scanner authorization application and directs the user through the OAuth process.
     *
     * @param context  the activity for the authorization by the end user.
     * @param callback the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
     * @since 2.2
     */
    public void authorize(@NonNull Context context, @NonNull CompletionHandler<Void> callback) {
        _intent.setAction(actionBarcode);
        if (_intent.resolveActivity(context.getPackageManager()) == null) {
            callback.onComplete(ResultImpl.error("Activity Not Found"));
            return;
        }
        Log.i(TAG, "Activity Found");

        _authorizeCallback = callback;
        _intent.setAction(null);
        _intent.setClass(context, BarcodeScannerActivity.class);
        _intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _intent.putExtra(BarcodeScannerActivity.BARCODE_REQUEST_CODE, requestCode);
        _intent.putExtra(BarcodeScannerActivity.BARCODE_SCAN_ACTION, actionBarcode);
        _intent.putExtra(BarcodeScannerActivity.BARCODE_SCAN_RESULT, scanResult);
        context.startActivity(_intent);
    }

    public static void barcodeAuthorize(String code) {
        Log.d(TAG, "authorize: " + code);
        if (code != null) {
            _authenticator.authorize(code, _authorizeCallback);
        } else {
            _authorizeCallback.onComplete(ResultImpl.error("Auth code isn't exist"));
        }
    }

    /**
     * Set customized requestCode
     *
     * @param requestCode Customized requestCode, default: 1992
     * @since 2.2
     */
    public OAuthQRCodeAuthenticator setRequestCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        return _authenticator.isAuthorized();
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        _authenticator.deauthorize();
    }

    /**
     * @see Authenticator
     */
    @Override
    public void getToken(CompletionHandler<String> handler) {
        _authenticator.getToken(handler);
    }

    /**
     * @see Authenticator
     */
    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        _authenticator.refreshToken(handler);
    }

    @AfterInjected
    private void afterInjected() {
        Log.d(TAG, "Inject authenticator after self injected");
        _injector.inject(_authenticator);
    }
}
