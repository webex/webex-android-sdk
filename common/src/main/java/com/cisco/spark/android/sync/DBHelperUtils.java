package com.cisco.spark.android.sync;

import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class DBHelperUtils {
    private static final String TMP_PREFIX = "TMP_";

    public static List<Uri> getContentUris() {
        List<Uri> uris = new ArrayList<Uri>();
        for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
            uris.add(table[0].contentUri());
        }
        for (ConversationContract.DbColumn[] table : ConversationContract.allVirtualTables) {
            uris.add(table[0].contentUri());
        }
        return uris;
    }

    protected void dropAllTables(SQLiteDatabaseInterface db) {
        for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
            String cmd = "DROP TABLE IF EXISTS " + table[0].tablename();
            Ln.v(cmd);
            execSQL(db, cmd);

            cmd = "DROP TABLE IF EXISTS " + TMP_PREFIX + table[0].tablename();
            Ln.v(cmd);
            execSQL(db, cmd);
        }

        dropAllVirtualTables(db);
        dropAllViews(db);
    }

    protected void dropAllVirtualTables(SQLiteDatabaseInterface db) {
        for (ConversationContract.VirtualTableColumn[] table : ConversationContract.allVirtualTables) {
            String cmd = "DROP TABLE IF EXISTS " + table[0].tablename();
            Ln.v(cmd);
            execSQL(db, cmd);
        }
    }

    protected void dropAllViews(SQLiteDatabaseInterface db) {
        for (ConversationContract.ViewColumn[] view : ConversationContract.allViews) {
            String cmd = "DROP VIEW IF EXISTS " + view[0].tablename();
            Ln.v(cmd);
            execSQL(db, cmd);
        }
    }

    protected void dropAllTriggers(SQLiteDatabaseInterface db) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'trigger'", null);
            while (c.moveToNext()) {
                String triggerName = c.getString(0);
                execSQL(db, "DROP TRIGGER IF EXISTS " + triggerName);
            }
        } finally {
            if (c != null)
                c.close();
            c = null;
        }
    }

    // WARNING this is very slow when migrating large datasets, only use during dev test
    private void logTableContents(SQLiteDatabaseInterface db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + table, null);
            Ln.d("DBLOG " + c.getCount() + " rows from table " + table);
            while (c.moveToNext()) {
                for (int i = 0; i < c.getColumnCount(); i++) {
                    Ln.d("DBLOG " + table + "." + c.getColumnName(i) + " = " + c.getString(i));
                }
            }
        } finally {
            if (c != null)
                c.close();
            c = null;
        }
    }

    protected void createTable(SQLiteDatabaseInterface db, ConversationContract.DbColumn[] cols) {
        String table = cols[0].tablename();

        if (tableExists(db, table)) {
            try {
                execSQL(db, "DROP TABLE IF EXISTS " + TMP_PREFIX + table);
                execSQL(db, "CREATE TABLE " + TMP_PREFIX + table + " AS SELECT * FROM " + table);
            } catch (Exception e) {
                Ln.w("Failed creating table " + TMP_PREFIX + table + " from existing table "
                        + table);
            }

            execSQL(db, "DROP TABLE IF EXISTS " + cols[0].tablename());
        }

        String complexKey = "";

        String cmd = "CREATE TABLE " + cols[0].tablename() + " (";
        for (ConversationContract.DbColumn col : cols) {
            String datatypeWithModifiers = col.datatype();
            if ((col.flags() & ConversationContract.PRIMARY_KEY) > 0)
                datatypeWithModifiers += " PRIMARY KEY";
            if ((col.flags() & ConversationContract.AUTOINCREMENT) > 0)
                datatypeWithModifiers += " AUTOINCREMENT";
            if ((col.flags() & ConversationContract.UNIQUE) > 0)
                datatypeWithModifiers += " UNIQUE";
            if ((col.flags() & ConversationContract.NOT_NULL) > 0)
                datatypeWithModifiers += " NOT NULL";
            if ((col.flags() & ConversationContract.DEFAULT_1) > 0)
                datatypeWithModifiers += " DEFAULT 1";
            if ((col.flags() & ConversationContract.DEFAULT_0) > 0)
                datatypeWithModifiers += " DEFAULT 0";
            if ((col.flags() & ConversationContract.DEFAULT_EMPTY_STRING) > 0)
                datatypeWithModifiers += " DEFAULT ''";
            if ((col.flags() & ConversationContract.COMPLEX_KEY) > 0)
                complexKey += col.name() + ", ";
            if ((col.flags() & ConversationContract.TIMESTAMP) > 0)
                datatypeWithModifiers += " DEFAULT 0";


            cmd += col.name() + " " + datatypeWithModifiers + ", ";
        }
        cmd = cmd.replaceAll(", $", ""); // remove trailing comma
        complexKey = complexKey.replaceAll(", $", "");

        if (complexKey.length() > 0) {
            cmd += ", CONSTRAINT complexkey_" + cols[0].tablename() + " UNIQUE (" + complexKey
                    + ") ";
        }

        cmd += ");";
        execSQL(db, cmd);

        // The table is created; if there's an TMP_ table with migrated data from an upgrade, copy the data in and drop
        // the temp table.
        if (tableExists(db, TMP_PREFIX + table)) {
            try {
                String columnList = TextUtils.join(",", cols);
                execSQL(db, "INSERT INTO " + table + "(" + columnList + ") SELECT " + columnList + " FROM "
                        + TMP_PREFIX + table);
            } catch (SQLException sqlex) {
                Ln.e(sqlex, "Failed migrating data from table " + TMP_PREFIX + table + " to table "
                        + table);
                throw sqlex;
            } catch (Exception e) {
                Ln.e(e, "Failed migrating data from table " + TMP_PREFIX + table + " to table "
                        + table);
            } finally {
                execSQL(db, "DROP TABLE IF EXISTS " + TMP_PREFIX + table);
            }
        }
    }

    // just to catch any typos or cut-paste bugs
    static HashSet<Integer> urlIDs = new HashSet<Integer>();

    private static void ensureUnique(int n) {

        if (urlIDs.contains(n)) {
            throw new IllegalArgumentException("ASSERT - Cannot add duplicate URL ID " + n
                    + ". Check Content.java");
        }
        urlIDs.add(n);
    }

    protected void execSQL(SQLiteDatabaseInterface db, String sql) {
        Ln.i("Execute SQL: " + sql);
        db.execSQL(sql);
    }

    public boolean tableExists(SQLiteDatabaseInterface db, String table) {
        Cursor c = null;
        try {
            c = db.query("sqlite_master", new String[]{
                    "count(*)"
            }, "name like ?", new String[]{
                    table
            }, null, null, null);
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
        } catch (Exception e) {
            Ln.e("Error checking table exists " + table, e);
        } finally {
            if (c != null)
                c.close();
        }
        return false;
    }

    public boolean columnExists(SQLiteDatabaseInterface db, ConversationContract.DbColumn col) {
        return columnExists(db, col.tablename(), col.name());
    }

    public boolean columnExists(SQLiteDatabaseInterface db, String table, String column) {
        Cursor c = null;
        try {
            c = db.query(table, new String[]{column}, null, null, null, null, null);
            return true;
        } catch (SQLiteException e) {
            Ln.d("Column does not exist. " + table + " : " + column);
        } catch (Exception e) {
            Ln.e(e, "Failed checking for column " + table + " : " + column);
        } finally {
            if (c != null)
                c.close();
        }
        return false;
    }


    // @formatter:off

    /**
     * UPGRADE SEQUENCE goes like this:
     * 1. Call each upgrade module sequentially starting from the current schema version.
     * 2. For each table in the current schema, copy the columns into a TMP_table.
     * 3. Recreate each table from the current schema.
     * 4. Copy data from the TMP tables into the new tables and clean up.
     * <p/>
     * <p/>
     * <p/>
     * *******************************************************************************************
     * UPGRADE MODULES
     * <p/>
     * Each upgrade module migrates from one schema version to the next.
     * <p/>
     * An upgrade module is required when incrementing the schema version. Upgrade modules should
     * handle the following changes:
     * <p/>
     * - add columns (including if one was renamed)
     * - modify column constraints
     * - add table (including if one was renamed)
     * - modify existing data to play nice with current components
     * - populate new columns if needed
     * <p/>
     * Views are always built from scratch on schema upgrades, no migration necessary for view changes.
     * <p/>
     * If incrementing the schema version but no upgrade module is needed it is polite to
     * add a NOOP UpgradeModule with a comment to that effect.
     * <p/>
     * Do not use ConversationContract constants in your migrations. They may change later.
     * <p/>
     * Once an upgrade module is published in production code, changing it is a bad idea. Add a new
     * one instead.
     * <p/>
     * For a bunch of UpgradeModule examples you can borrow from, see older versions of this file.
     * <p/>
     * for example:
     * <p/>
     * <a href="https://sqbu-github.cisco.com/WebExSquared/wx2-android/blob/f6b949efbbbce7abd858833e6f0681990886a9ef/app/src/main/java/com/cisco/wx2/android/sync/DatabaseHelper.java#L728">here</a>
     */
    // @formatter:on

    protected abstract class UpgradeModule {
        protected abstract boolean migrateFromPrevious(SQLiteDatabaseInterface db);

        public final int mTargetVersion;

        public UpgradeModule(int targetVersion) {
            mTargetVersion = targetVersion;
        }

        public final boolean execute(SQLiteDatabaseInterface db) {
            try {
                Ln.i("Upgrading to version " + mTargetVersion);
                if (migrateFromPrevious(db))
                    db.setVersion(mTargetVersion);
            } catch (Exception e) {
                Ln.e(e, "ERROR : Failed upgrading to version " + mTargetVersion);
            }
            return mTargetVersion == db.getVersion();
        }
    }

    protected List<UpgradeModule> upgradeModules = Arrays.asList();

    public boolean runSanity() {

        /**
         * First make sure all the match id's are unique
         */
        urlIDs.clear();

        for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
            ensureUnique(table[0].uriMatchCode());
            ensureUnique(table[0].idUriMatchCode());
        }

        for (ConversationContract.VirtualTableColumn[] table : ConversationContract.allVirtualTables) {
            ensureUnique(table[0].uriMatchCode());
            ensureUnique(table[0].idUriMatchCode());
        }

        for (ConversationContract.ViewColumn[] view : ConversationContract.allViews) {
            ensureUnique(view[0].uriMatchCode());
            ensureUnique(view[0].idUriMatchCode());
        }

        /**
         * Make sure the current schema version has an upgrade module
         */
        List<UpgradeModule> modules = upgradeModules;
        if (modules.size() > 0 && modules.get(modules.size() - 1).mTargetVersion != ConversationContract.SCHEMA_VERSION) {
            throw new RuntimeException("All new schema versions must have migration modules");
        }

        /**
         * See if the schema has changed, and politely suggest double checking to see if an
         * ConversationContract.SCHEMA_VERSION increment is necessary.
         */
        StringBuilder schemastring = new StringBuilder();
        for (ConversationContract.DbColumn[] table : ConversationContract.allTables) {
            schemastring.append(table[0].tablename());
            appendColumnsToStringBuilder(table, schemastring);
        }

        for (ConversationContract.VirtualTableColumn[] table : ConversationContract.allVirtualTables) {
            schemastring.append(table[0].tablename());
            appendColumnsToStringBuilder(table, schemastring);
        }

        for (ConversationContract.ViewColumn[] view : ConversationContract.allViews) {
            schemastring.append(view[0].tablename());
            appendColumnsToStringBuilder(view, schemastring);
        }

        String schemahash = Strings.md5(schemastring.toString());
        if (!TextUtils.equals(schemahash, ConversationContract.DB_SCHEMA_HASH)) {
            // We calculate a hash for the schema to see if it changed. Note that this might catch
            // irrelevant changes if somebody say changes the order of the columns.
            //
            // If the schema is incremented to reflect new changes and you're sure that running
            // the new DB schema on existing builds is OK, update the DB_SCHEMA_HASH in
            // ConversationContract with the new hash value. Note that there must be an upgrade
            // module in DatabaseHelper for the current schema version, otherwise a test will fail.

            throw new RuntimeException("ERROR The DB schema has changed. New hash=" + schemahash + "  See ConversationContract.java");
        }

        return true;
    }

    private void renameColumn(SQLiteDatabaseInterface db, String table, String oldColumnName, String newColumnName) {
        if (tableExists(db, table) && !columnExists(db, table, newColumnName) && columnExists(db, table, oldColumnName)) {
            db.execSQL("ALTER TABLE " + table + "  ADD COLUMN " + newColumnName + " TEXT");
            execSQL(db, "UPDATE " + table + " SET " + newColumnName + " = " + oldColumnName);
        }
    }

    private void addColumn(SQLiteDatabaseInterface db, String table, String column, String type) {
        if (tableExists(db, table) && !columnExists(db, table, column)) {
            execSQL(db, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    //used during test to calculate the checksum
    private static void appendColumnsToStringBuilder(ConversationContract.DbColumn[] columns, StringBuilder builder) {
        for (ConversationContract.DbColumn column : columns) {
            builder.append(column.name());
            builder.append(column.flags());
            builder.append(column.datatype());
        }
    }

    // Handy for debugging
    @SuppressWarnings("unused")
    public static String cursorToString(Cursor c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.getColumnCount(); i++) {
            sb.append(c.getColumnName(i)).append(" = ").append(c.getString(i)).append("    ");
        }
        return sb.toString();
    }

    //sqlcipher does not handle null values in selectionArgs correctly
    public static String[] replaceNull(String[] selectionArgs) {
        if (selectionArgs != null && selectionArgs.length > 0) {
            for (int index = 0; index < selectionArgs.length; index++) {
                if (selectionArgs[index] == null) {
                    selectionArgs[index] = "NULL";
                }
            }
        }
        return selectionArgs;
    }
}
