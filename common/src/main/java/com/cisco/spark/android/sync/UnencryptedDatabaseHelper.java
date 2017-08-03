package com.cisco.spark.android.sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;

import com.cisco.spark.android.core.Application;
import com.cisco.spark.android.sync.DBHelperUtils.UpgradeModule;
import com.github.benoitdion.ln.Ln;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UnencryptedDatabaseHelper extends SQLiteOpenHelper implements SQLiteOpenHelperInterface {
    private static boolean safetyNetExists = true;
    // Upgrades from versions before this are not supported; the DB will be wiped clean and rebuilt.
    private DBHelperUtils dbHelperUtils = new DBHelperUtils();

    @Inject
    public UnencryptedDatabaseHelper(Context context) {
        super(context, DatabaseHelper.SERVICE_DB_NAME, null, ConversationContract.SCHEMA_VERSION);
    }

    public UnencryptedDatabaseHelper(Context context, String dbname) {
        super(context, dbname, null, ConversationContract.SCHEMA_VERSION);
    }

    public DBHelperUtils getDbHelperUtils() {
        return dbHelperUtils;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        UnencryptedSQLiteDatabase unencryptedSQLiteDatabase = new UnencryptedSQLiteDatabase(db);
        try {
            DBHelperUtils.urlIDs.clear();

            try {
                for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
                    dbHelperUtils.createTable(unencryptedSQLiteDatabase, table);
                }
            } catch (Exception e) {
                if (safetyNetExists) {
                    safetyNetExists = false;
                    Ln.e(e, "Failed migrating database. Dropping all tables and starting from scratch.");
                    dbHelperUtils.dropAllTables(unencryptedSQLiteDatabase);
                    onCreate(db);
                    return;
                } else {
                    safetyNetExists = true;
                    throw new RuntimeException(e);
                }
            }

            for (ConversationContract.VirtualTableColumn[] table : ConversationContract.allVirtualTables) {
                dbHelperUtils.execSQL(unencryptedSQLiteDatabase, table[0].getVirtualTableSql());
            }

            for (ConversationContract.ViewColumn[] view : ConversationContract.allViews) {
                dbHelperUtils.execSQL(unencryptedSQLiteDatabase, view[0].getViewSql());
            }

            for (String triggersql : ConversationContract.triggers) {
                dbHelperUtils.execSQL(unencryptedSQLiteDatabase, triggersql);
            }

            int i = 0;
            for (ConversationContract.DbColumn[] indexColumns : ConversationContract.TABLE_INDEXES) {
                dbHelperUtils.execSQL(unencryptedSQLiteDatabase, "CREATE INDEX IF NOT EXISTS "
                        + " IX_" + indexColumns[0].tablename() + "_" + (i++)
                        + " ON " + indexColumns[0].tablename()
                        + " (" + ConversationContract.getTableColumns(indexColumns) + ")");
            }

            // update statistics
            dbHelperUtils.execSQL(unencryptedSQLiteDatabase, "ANALYZE main");

        } catch (Exception e) {
            // We can get here if something goes wrong during data migration. Retry once with a clean slate.
            if (safetyNetExists) {
                safetyNetExists = false;
                Ln.e(e, "Failed migrating database. Dropping all tables and starting from scratch.");
                dbHelperUtils.dropAllTables(unencryptedSQLiteDatabase);
                onCreate(db);
            } else {
                throw new RuntimeException(e);
            }
        }

        //Delete old conversationCacheEncrypted.db
        Context context = Application.getInstance().getApplicationContext();
        if (context != null) {
            File encryptedDBPath = context.getDatabasePath(DatabaseHelper.SERVICE_DB_ENCRYPTED_NAME);
            if (encryptedDBPath != null && encryptedDBPath.exists()) {
                boolean success = context.deleteDatabase(DatabaseHelper.SERVICE_DB_ENCRYPTED_NAME);
                if (success) {
                    Ln.i(DatabaseHelper.SERVICE_DB_ENCRYPTED_NAME + " has been deleted");
                } else {
                    Ln.e("Unable to delete " + DatabaseHelper.SERVICE_DB_ENCRYPTED_NAME);
                }
            } else {
                Ln.i(DatabaseHelper.SERVICE_DB_ENCRYPTED_NAME + " does not exist");
            }
        } else {
            Ln.i(DatabaseHelper.SERVICE_DB_ENCRYPTED_NAME + " does not exist");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbHelperUtils.dropAllTables(new UnencryptedSQLiteDatabase(db));
        onCreate(db);
        db.setVersion(newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Ln.i("Upgrading Database from version " + oldVersion + " to " + newVersion);
        UnencryptedSQLiteDatabase unencryptedSQLiteDatabase = new UnencryptedSQLiteDatabase(db);
        dbHelperUtils.dropAllVirtualTables(unencryptedSQLiteDatabase);
        dbHelperUtils.dropAllViews(unencryptedSQLiteDatabase);
        dbHelperUtils.dropAllTriggers(unencryptedSQLiteDatabase);

        if (db.getVersion() < DatabaseHelper.MINIMUM_SCHEMA_VERSION_FOR_MIGRATION) {
            dbHelperUtils.dropAllTables(unencryptedSQLiteDatabase);
            onCreate(db);
            db.setVersion(newVersion);
            return;
        } else {
            SparseArray<UpgradeModule> modules = new SparseArray<UpgradeModule>();

            for (Object module : dbHelperUtils.upgradeModules) {
                modules.append(((UpgradeModule) module).mTargetVersion, (UpgradeModule) module);
            }
            for (int nextVersion = oldVersion + 1; nextVersion <= newVersion; nextVersion++) {
                UpgradeModule module = modules.get(nextVersion);
                if (module != null && !module.execute(unencryptedSQLiteDatabase)) {
                    Ln.e("Failed migrating to version " + nextVersion
                            + "; wiping database and starting clean.");
                    dbHelperUtils.dropAllTables(unencryptedSQLiteDatabase);
                    break;
                }
            }
        }
        onCreate(db);
    }

    @Override
    public boolean init(Context context) {
        return true;
    }

    @Override
    public void beginTransactionNonExclusive(SQLiteDatabaseInterface db) {
        ((UnencryptedSQLiteDatabase) db).getDatabase().beginTransactionNonExclusive();
    }

    @Override
    public SQLiteDatabaseInterface openDatabase(String path) {
        return new UnencryptedSQLiteDatabase(SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE));
    }

    public synchronized void toggleDatabaseEncryptionState(boolean switchToEncryptedDatabase) {}

    public SQLiteDatabaseInterface getWritableDb() {
        return new UnencryptedSQLiteDatabase(super.getWritableDatabase());
    }

    public SQLiteDatabaseInterface getReadableDb() {
        return new UnencryptedSQLiteDatabase(super.getReadableDatabase());
    }
}
