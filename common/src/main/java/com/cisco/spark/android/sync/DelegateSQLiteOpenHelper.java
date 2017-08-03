package com.cisco.spark.android.sync;

import android.content.Context;

import javax.inject.Inject;

public class DelegateSQLiteOpenHelper {
    private SQLiteOpenHelperInterface sqliteOpenHelper = null;
    private Context context;
    private static boolean isDatabaseEncrypted = false;

    @Inject
    public DelegateSQLiteOpenHelper(Context context) {
        this.context = context;
    }

    public synchronized SQLiteOpenHelperInterface getSqliteOpenHelper() {
        if (sqliteOpenHelper == null) {
            sqliteOpenHelper = isDatabaseEncrypted ? new DatabaseHelper(context) : new UnencryptedDatabaseHelper(context);
        }
        return sqliteOpenHelper;
    }

    public static boolean isDatabaseEncrypted() {
        return isDatabaseEncrypted;
    }
}
