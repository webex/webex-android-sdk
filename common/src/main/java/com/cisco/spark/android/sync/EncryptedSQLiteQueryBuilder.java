package com.cisco.spark.android.sync;

import android.database.Cursor;

import net.sqlcipher.database.SQLiteQueryBuilder;

public class EncryptedSQLiteQueryBuilder extends SQLiteQueryBuilder implements DatabaseSQLiteQueryBuilder {
    @Override
    public Cursor query(SQLiteDatabaseInterface db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return super.query(((EncryptedSQLiteDatabase) db).getDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
    }
}
