package com.cisco.spark.android.sync;

public class DelegateSQLiteQueryBuilder {

    public static DatabaseSQLiteQueryBuilder getSQLiteQueryBuilder() {
            return new EncryptedSQLiteQueryBuilder();
    }
}
