package com.cisco.spark.android.sync;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

public class UnencryptedSQLiteDatabase implements SQLiteDatabaseInterface {
    @NonNull
    private SQLiteDatabase database;

    public UnencryptedSQLiteDatabase(){}
    public UnencryptedSQLiteDatabase(SQLiteDatabase db) {
        database = db;
    }

    public SQLiteDatabase getDatabase() {
        return this.database;
    }

    @Override
    public long insertWithOnConflict(String tablename, Object o, ContentValues values, int rule) {
        if (database != null) {
            return database.insertWithOnConflict(tablename, null, values, rule);
        }
        return 0;
    }

    @Override
    public void beginTransactionNonExclusive() {
        if (database != null) {
            database.beginTransactionNonExclusive();
        }
    }

    @Override
    public void setTransactionSuccessful() {
        if (database != null) {
            database.setTransactionSuccessful();
        }
    }

    @Override
    public void endTransaction() {
        if (database != null) {
            database.endTransaction();
        }
    }

    @Override
    public void execSQL(String s) {
        if (database != null) {
            database.execSQL(s);
        }
    }

    @Override
    public int delete(String table, String selection, String[] selectionArgs) {
        if (database != null) {
            return database.delete(table, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public int updateWithOnConflict(String tablename, ContentValues values, String selection, String[] argsArray, int conflictRule) {
        if (database != null) {
            return database.updateWithOnConflict(tablename, values, selection, argsArray, conflictRule);
        }
        return 0;
    }

    @Override
    public Cursor query(String tableName, String[] strings, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        if (database != null) {
            return database.query(tableName, strings, selection, selectionArgs, groupBy, having, orderBy);
        }
        return null;
    }


    @Override
    public int getVersion() {
        if (database != null) {
            return database.getVersion();
        }
        return 0;
    }

    @Override
    public void setVersion(int mTargetVersion) {
        if (database != null) {
            database.setVersion(mTargetVersion);
        }
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (database != null) {
            return database.rawQuery(sql, selectionArgs);
        }
        return null;
    }
}
