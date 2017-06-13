package com.cisco.spark.android.sync;

import android.content.Context;
import android.util.SparseArray;

import com.cisco.spark.android.core.Application;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.DBHelperUtils.UpgradeModule;
import com.github.benoitdion.ln.Ln;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.cisco.spark.android.sync.DBHelperUtils.urlIDs;

@Singleton
public class DatabaseHelper extends SQLiteOpenHelper implements SQLiteOpenHelperInterface {
    public static final String SERVICE_DB_NAME = "conversationCacheEncrypted.db";
    public static final String SERVICE_DB_LEGACY_NAME = "conversationCache.db";
    public static final String DB_PLAINTEXT_NAME = "conversationCachePlainText.db";
    // Upgrades from versions before this are not supported; the DB will be wiped clean and rebuilt.
    public static final int MINIMUM_SCHEMA_VERSION_FOR_MIGRATION = 142;

    private static boolean safetyNetExists = true;

    private DBHelperUtils dbHelperUtils = new DBHelperUtils();

    @Inject
    protected static Injector injector;


    @Inject
    public DatabaseHelper(Context context) {
        super(context, SERVICE_DB_NAME, null, ConversationContract.SCHEMA_VERSION);
        init(context);
    }

    public DatabaseHelper(Context context, String dbname) {
        super(context, dbname, null, ConversationContract.SCHEMA_VERSION);
        init(context);

    }

    public DBHelperUtils getDbHelperUtils() {
        return dbHelperUtils;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.rawExecSQL("PRAGMA journal_mode = WAL;");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        EncryptedSQLiteDatabase encryptedSQLiteDatabase = new EncryptedSQLiteDatabase(db);
        try {
            urlIDs.clear();
            try {
                for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
                    dbHelperUtils.createTable(encryptedSQLiteDatabase, table);
                }
            } catch (Exception e) {
                if (safetyNetExists) {
                    safetyNetExists = false;
                    Ln.e(e, "Failed migrating database. Dropping all tables and starting from scratch.");
                    dbHelperUtils.dropAllTables(encryptedSQLiteDatabase);
                    onCreate(db);
                    return;
                } else {
                    safetyNetExists = true;
                    throw new RuntimeException(e);
                }
            }

            for (ConversationContract.VirtualTableColumn[] table : ConversationContract.allVirtualTables) {
                dbHelperUtils.execSQL(encryptedSQLiteDatabase, table[0].getVirtualTableSql());
            }

            for (ConversationContract.ViewColumn[] view : ConversationContract.allViews) {
                dbHelperUtils.execSQL(encryptedSQLiteDatabase, view[0].getViewSql());
            }

            for (String triggersql : ConversationContract.triggers) {
                dbHelperUtils.execSQL(encryptedSQLiteDatabase, triggersql);
            }

            int i = 0;
            for (ConversationContract.DbColumn[] indexColumns : ConversationContract.TABLE_INDEXES) {
                dbHelperUtils.execSQL(encryptedSQLiteDatabase, "CREATE INDEX IF NOT EXISTS "
                        + " IX_" + indexColumns[0].tablename() + "_" + (i++)
                        + " ON " + indexColumns[0].tablename()
                        + " (" + ConversationContract.getTableColumns(indexColumns) + ")");
            }

            // update statistics
            dbHelperUtils.execSQL(encryptedSQLiteDatabase, "ANALYZE main");

        } catch (Exception e) {
            // We can get here if something goes wrong during data migration. Retry once with a clean slate.
            if (safetyNetExists) {
                safetyNetExists = false;
                Ln.e(e, "Failed migrating database. Dropping all tables and starting from scratch.");
                dbHelperUtils.dropAllTables(encryptedSQLiteDatabase);
                onCreate(db);
            } else {
                throw new RuntimeException(e);
            }
        }

        //Delete old conversationCache.db
        Context context = Application.getInstance().getApplicationContext();
        if (context != null) {
            File unencryptedDBPath = context.getDatabasePath(DatabaseHelper.SERVICE_DB_LEGACY_NAME);
            if (unencryptedDBPath != null && unencryptedDBPath.exists()) {
                boolean success = context.deleteDatabase(DatabaseHelper.SERVICE_DB_LEGACY_NAME);
                if (success) {
                    Ln.i(DatabaseHelper.SERVICE_DB_LEGACY_NAME + " has been deleted");
                } else {
                    Ln.e("Unable to delete " + DatabaseHelper.SERVICE_DB_LEGACY_NAME);
                }
            } else {
                Ln.w(DatabaseHelper.SERVICE_DB_LEGACY_NAME + " does not exist");
            }
        } else {
            Ln.w(DatabaseHelper.SERVICE_DB_LEGACY_NAME + " does not exist");
        }

    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbHelperUtils.dropAllTables(new EncryptedSQLiteDatabase(db));
        onCreate(db);
        db.setVersion(newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Ln.i("Upgrading Database from version " + oldVersion + " to " + newVersion);
        EncryptedSQLiteDatabase encryptedSQLiteDatabase = new EncryptedSQLiteDatabase(db);
        dbHelperUtils.dropAllVirtualTables(encryptedSQLiteDatabase);
        dbHelperUtils.dropAllViews(encryptedSQLiteDatabase);
        dbHelperUtils.dropAllTriggers(encryptedSQLiteDatabase);
        if (db.getVersion() < MINIMUM_SCHEMA_VERSION_FOR_MIGRATION) {
            dbHelperUtils.dropAllTables(encryptedSQLiteDatabase);
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
                if (module != null && !module.execute(encryptedSQLiteDatabase)) {
                    Ln.e("Failed migrating to version " + nextVersion
                            + "; wiping database and starting clean.");
                    dbHelperUtils.dropAllTables(encryptedSQLiteDatabase);
                    break;
                }
            }
        }
        onCreate(db);
    }

    @Override
    public boolean init(Context context) {
        SQLiteDatabase.loadLibs(context);
        LocalKeyStoreManager.init(context);
        return true;
    }

    @Override
    public synchronized SQLiteDatabaseInterface getWritableDb() {
        SQLiteDatabaseInterface encryptedSqliteDatabase = null;
        try {
            encryptedSqliteDatabase = new EncryptedSQLiteDatabase(super.getWritableDatabase(LocalKeyStoreManager.getMasterPassword()));

        } catch (SQLiteException ex) {
            Ln.e("exception while grabbing the database in write mode " + ex.getMessage());
        }
        return encryptedSqliteDatabase;
    }

    @Override
    public synchronized SQLiteDatabaseInterface getReadableDb() {
        SQLiteDatabaseInterface encryptedSqliteDatabase = null;
        try {
            encryptedSqliteDatabase = new EncryptedSQLiteDatabase(super.getReadableDatabase(LocalKeyStoreManager.getMasterPassword()));
        } catch (SQLiteException ex) {
            Ln.e("exception while grabbing the database for read only mode " + ex.getMessage());
        } finally {
            return encryptedSqliteDatabase;
        }
    }

    @Override
    public void beginTransactionNonExclusive(SQLiteDatabaseInterface db) {
        db.beginTransactionNonExclusive();
    }

    @Override
    public SQLiteDatabaseInterface openDatabase(String path) {
        SQLiteDatabaseInterface encryptedSqliteDatabase = null;
        try {
            encryptedSqliteDatabase = new EncryptedSQLiteDatabase(SQLiteDatabase.openOrCreateDatabase(path, LocalKeyStoreManager.getMasterPassword(), null));
        } catch (SQLiteException ex) {
            Ln.e("exception while opening the database" + ex.getMessage());
        } finally {
            return encryptedSqliteDatabase;
        }
    }

    public synchronized void toggleDatabaseEncryptionState(boolean switchToEncryptedDatabase) {
        //String password = LocalKeyStoreManager.getMasterPassword();
        SQLiteDatabase db = super.getWritableDatabase("");
        if (switchToEncryptedDatabase) {
            //LocalKeyStoreManager.createOrGetPasswordFromKeystore(this.context);
            String sql = String.format("PRAGMA rekey=\"%s\";", LocalKeyStoreManager.getMasterPassword());
            db.rawExecSQL(sql);
        } else {
            db.rawExecSQL("PRAGMA rekey='';");
            LocalKeyStoreManager.setMasterPassword("");
        }
        //db.close();
        //openDatabase(path);
    }

    public synchronized void rekeyDatabase(String dbPath, String newPassword) {
        String password = LocalKeyStoreManager.getMasterPassword();
        SQLiteDatabase db = super.getReadableDatabase(password);
        int version = db.getVersion();
        try {
            String sql = String.format("PRAGMA key = '%s'", password);
            db.rawExecSQL(sql);
            sql = String.format("ATTACH DATABASE '%s' AS encrypted KEY '%s'", dbPath, newPassword);
            db.rawExecSQL(sql);
            db.rawExecSQL("SELECT sqlcipher_export('encrypted')");
            db.rawExecSQL("DETACH DATABASE encrypted");
        } catch (Exception ex) {
            Ln.e("Exception during rekeying of encrypted database " + ex);
        } finally {
            db.close();
            db = SQLiteDatabase.openDatabase(dbPath, newPassword, null, SQLiteDatabase.OPEN_READWRITE);
            db.setVersion(version);
            db.close();
        }
    }

    public boolean extractUnencryptedDatabase(String plainTextDBPath) {
        String password = LocalKeyStoreManager.getMasterPassword();
        SQLiteDatabase db = super.getReadableDatabase(password);
        try {
            String sql = String.format("PRAGMA key = '%s'", password);
            db.rawExecSQL(sql);
            sql = String.format("ATTACH DATABASE '%s' AS plaintext KEY ''", plainTextDBPath);
            db.rawExecSQL(sql);
            db.rawExecSQL("SELECT sqlcipher_export('plaintext')");
            db.rawExecSQL("DETACH DATABASE plaintext");
        } catch (Exception ex) {
            Ln.e("Exception during extraction of encrypted database " + ex);
        } finally {
            db.close();
            return true;
        }
    }
}
