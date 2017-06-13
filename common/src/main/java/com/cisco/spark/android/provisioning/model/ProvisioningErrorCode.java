package com.cisco.spark.android.provisioning.model;

import android.util.SparseArray;

import java.util.EnumSet;

public enum ProvisioningErrorCode {
    USERNAME_EXISTS(0),
    INVALID_MISSING_TOKEN(200001),
    INVALID_OTP(200030),
    THIRD_STRIKE_INVALIDATED(200031),
    EXPIRED_OTP(200032),
    INSUFFICIENT_ACCESS_TOKEN(200041);

    private int errorCode;

    private static final SparseArray<ProvisioningErrorCode> LOOKUP_TABLE = new SparseArray<ProvisioningErrorCode>();

    static {
        for (ProvisioningErrorCode code : EnumSet.allOf(ProvisioningErrorCode.class)) {
            LOOKUP_TABLE.put(code.getErrorCode(), code);
        }
    }

    ProvisioningErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public static ProvisioningErrorCode fromErrorCode(int errorCode) {
        return LOOKUP_TABLE.get(errorCode);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
