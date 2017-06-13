package com.cisco.spark.android.sync;

import android.content.ContentValues;
import android.database.Cursor;

public interface SQLiteDatabaseInterface {
    long insertWithOnConflict(String tablename, Object o, ContentValues values, int rule);

    void beginTransactionNonExclusive();

    void setTransactionSuccessful();

    void endTransaction();

    void execSQL(String s);

    int delete(String table, String selection, String[] selectionArgs);

    int updateWithOnConflict(String tablename, ContentValues values, String selection, String[] argsArray, int conflictRule);

    Cursor query(String tableName, String[] strings, String selection, String[] selectionArgs, String groupBy, String having, String orderBy);

    int getVersion();

    void setVersion(int mTargetVersion);

    Cursor rawQuery(String sql, String[] selectionArgs);
}
