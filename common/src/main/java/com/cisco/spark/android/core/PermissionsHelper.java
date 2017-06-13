package com.cisco.spark.android.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.cisco.spark.android.util.UIUtils;

import javax.inject.Inject;

public class PermissionsHelper {
    public static final int PERMISSIONS_REQUEST_CODE = 0;
    public static final int PERMISSIONS_CALLING_REQUEST = 1;
    public static final int REQUEST_CODE_NATIVE_CONTACTS_INTEGRATION = 2;
    public static final int PERMISSIONS_STORAGE_REQUEST = 3;
    private final Context context;

    @Inject
    public PermissionsHelper(Context context) {
        this.context = context;
    }

    public boolean requiresOnboardingPermissions() {
        return (UIUtils.hasMarshmallow() && (!hasMicrophonePermission()));
    }

    public boolean hasGetAccountsPermission() {
        return checkSelfPermission(Manifest.permission.GET_ACCOUNTS);
    }

    public boolean hasStoragePermission() {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA);
    }

    public boolean hasMicrophonePermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO);
    }

    public boolean hasCalendarPermission() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR);
    }

    public boolean hasContactPermission() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS);
    }

    public boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean hasPhoneStatePermission() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
    }

    private boolean checkSelfPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public String[] missingPermissionsForCalling() {
        String[] permissions;

        if (!hasMicrophonePermission() && !hasCameraPermission()) {
            permissions = PermissionsHelper.permissionsForCalling();
        } else if (!hasMicrophonePermission()) {
            permissions = PermissionsHelper.permissionsForMicrophone();
        } else {
            permissions = PermissionsHelper.permissionsForCamera();
        }

        return permissions;
    }

    public static String[] permissionsForStorage() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    public static String[] permissionsForCamera() {
        return new String[]{Manifest.permission.CAMERA};
    }

    public static String[] permissionsForCalling() {
        return new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    }

    public static String[] permissionsForMicrophone() {
        return new String[]{Manifest.permission.RECORD_AUDIO};
    }

    public static String[] permissionsForCalendar() {
        return new String[]{Manifest.permission.READ_CALENDAR};
    }

    public static String[] permissionsForContacts() {
        return new String[]{Manifest.permission.READ_CONTACTS};
    }

    public static String[] permissionsForLocation() {
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    public static String[] permissionsForPhoneState() {
        return new String[]{Manifest.permission.READ_PHONE_STATE};
    }

    public static boolean resultForCalendarPermission(String[] permissions, int[] grantResults) {
        return checkPermissionsResults(Manifest.permission.READ_CALENDAR, permissions, grantResults);
    }

    public static boolean resultForCallingPermissions(String[] permissions, int[] grantResults) {
        boolean result = true;

        for (String permission : permissions) {
            result &= checkPermissionsResults(permission, permissions, grantResults);
        }

        return result;
    }

    public static boolean resultForCameraPermissions(String[] permissions, int[] grantResults) {
        return checkPermissionsResults(Manifest.permission.CAMERA, permissions, grantResults);
    }

    public static boolean resultsForMicrophone(String[] permissions, int[] grantResults) {
        return checkPermissionsResults(Manifest.permission.RECORD_AUDIO, permissions, grantResults);
    }

    public static boolean resultsForPhoneStatePermission(String[] permissions, int[] grantResults) {
        return checkPermissionsResults(Manifest.permission.READ_PHONE_STATE, permissions, grantResults);
    }

    public static boolean resultsForContactsIntegration(String[] permissions, int[] grantResults) {
        for (String perm : permissionsForNativeContacts()) {
            if (!checkPermissionsResults(perm, permissions, grantResults))
                return false;
        }
        return true;
    }

    public static boolean resultsForStorage(String[] permissions, int[] grantResults) {
        boolean result = true;

        for (String permission : permissionsForStorage()) {
            result &= checkPermissionsResults(permission, permissions, grantResults);
        }

        return result;
    }

    private static boolean checkPermissionsResults(String permissionRequested, String[] permissions, int[] grantResults) {
        for (int index = 0; index < permissions.length; index++) {
            String permission = permissions[index];
            int grantResult = grantResults[index];

            if (permissionRequested.equals(permission)) {
                return grantResult == PackageManager.PERMISSION_GRANTED;
            }
        }

        return false;
    }

    public static String[] permissionsForNativeContacts() {
        return new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_SYNC_SETTINGS};
    }
    public static String[] permissionsForCallLog() {
        return new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG};
    }

    public static boolean hasUserPermanentlyDeniedStoragePermissions(Activity activity, String[]permissions, int[] grantResults) {
        boolean userDeniedPermission = !resultsForStorage(permissions, grantResults);
        boolean shouldShowRationaleForReadPermission = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean shouldShowRationaleForWritePermission = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return userDeniedPermission && !shouldShowRationaleForReadPermission && !shouldShowRationaleForWritePermission;
    }
}
