package com.cisco.spark.android.sync;

import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;

public class UnencryptedSQLiteQueryBuilder extends SQLiteQueryBuilder implements DatabaseSQLiteQueryBuilder {
    @Override
    public Cursor query(SQLiteDatabaseInterface db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return super.query(((UnencryptedSQLiteDatabase) db).getDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
    }
}
