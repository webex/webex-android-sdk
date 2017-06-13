package com.cisco.spark.android.sync;

import android.content.Context;

import javax.inject.Inject;

public class DelegateSQLiteOpenHelper {
    private SQLiteOpenHelperInterface sqliteOpenHelper = null;
    private Context context;

    @Inject
    public DelegateSQLiteOpenHelper(Context context) {
        this.context = context;
    }

    public synchronized SQLiteOpenHelperInterface getSqliteOpenHelper() {
        if (sqliteOpenHelper == null) {
            sqliteOpenHelper = new DatabaseHelper(context);
        }
        return sqliteOpenHelper;
    }
}
