package com.cisco.spark.android.sync;

public class DelegateSQLiteQueryBuilder {

    public static DatabaseSQLiteQueryBuilder getSQLiteQueryBuilder() {
        if (DelegateSQLiteOpenHelper.isDatabaseEncrypted()) {
            return new EncryptedSQLiteQueryBuilder();
        } else {
            return new UnencryptedSQLiteQueryBuilder();
        }
    }
}
