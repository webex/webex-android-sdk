package com.cisco.spark.android.sync;

import android.content.Context;

public interface SQLiteOpenHelperInterface {
    boolean init(Context context);

    SQLiteDatabaseInterface getWritableDb();

    SQLiteDatabaseInterface getReadableDb();

    void beginTransactionNonExclusive(SQLiteDatabaseInterface db);

    SQLiteDatabaseInterface openDatabase(String path);
}
