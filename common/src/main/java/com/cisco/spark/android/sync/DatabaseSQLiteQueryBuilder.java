package com.cisco.spark.android.sync;

import android.database.Cursor;

public interface DatabaseSQLiteQueryBuilder {
    void setTables(String tablename);
    Cursor query(SQLiteDatabaseInterface sqLiteDatabaseInterface, String[] projection, String selection, String[] selectionArgs, String sortOrder);
}
