package com.cisco.spark.android.model;


import android.content.Context;

import com.cisco.spark.android.R;

public class RetentionPolicy {

    private static final int ONE_YEAR_IN_DAYS = 365;

    private int retentionDays = 0;

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getRetentionDurationInfo(Context context) {
        if (retentionDays == -1) {
            return context.getResources().getString(R.string.room_retention_policy_indefinite);
        } else if (retentionDays == ONE_YEAR_IN_DAYS) {
            return context.getResources().getString(R.string.room_retention_policy_one_year);
        } else if (retentionDays == 0) {
            return null;
        }
        return String.format(context.getResources().getString(R.string.room_retention_policy_for_days), retentionDays);
    }

}
