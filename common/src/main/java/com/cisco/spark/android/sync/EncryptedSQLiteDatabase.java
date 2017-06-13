package com.cisco.spark.android.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.github.benoitdion.ln.Ln;

import net.sqlcipher.database.SQLiteDatabase;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

@Singleton
public class EncryptedSQLiteDatabase implements SQLiteDatabaseInterface {
    private ReentrantLock lock = null;
    @NonNull
    private SQLiteDatabase sqLiteDatabase;

    public EncryptedSQLiteDatabase() {
    }

    public EncryptedSQLiteDatabase(String path, char[] password, int flags) {
        sqLiteDatabase = new SQLiteDatabase(path, password, null, flags);
    }


    public EncryptedSQLiteDatabase(SQLiteDatabase writableDatabase) {
        this.sqLiteDatabase = writableDatabase;
    }

    public void getLockFromSQLiteDatabaseAndSetup() {
        try {
            Field mLockField = SQLiteDatabase.class.getDeclaredField("mLock");
            mLockField.setAccessible(true);
            lock = (ReentrantLock) mLockField.get(sqLiteDatabase);
            mLockField.setAccessible(false);
        } catch (NoSuchFieldException e) {
            Ln.e("getLockFromSQLiteDatabaseAndSetup NoSuchFieldException " + e.getStackTrace());
        } catch (IllegalAccessException e) {
            Ln.e("getLockFromSQLiteDatabaseAndSetup IllegalAccessException " + e.getStackTrace());

        }
    }

    /*
     *  Based on net.sqlcipher.database.SQLiteDatabase.beginTransactionWithListener()
     *  Executing Sql command BEGIN IMMEDIATE to enable non exclusive transactions
     *
     */

    @Override
    public void beginTransactionNonExclusive() {
        try {
            Method lockForced = SQLiteDatabase.class.getDeclaredMethod("lockForced");
            lockForced.setAccessible(true);
            lockForced.invoke(sqLiteDatabase);
            lockForced.setAccessible(false);
        } catch (NoSuchMethodException e) {
            Ln.e("beginTransaction NoSuchMethodException" + e.getStackTrace());
        } catch (InvocationTargetException e) {
            Ln.e("beginTransaction InvocationTargetException" + e.getStackTrace());
        } catch (IllegalAccessException e) {
            Ln.e("beginTransaction IllegalAccessException" + e.getStackTrace());
        }

        if (!sqLiteDatabase.isOpen()) {
            throw new IllegalStateException("database not open");
        } else {
            boolean ok = false;

            try {
                boolean innerTransactionIsSuccessful = false;
                Field mInnerTransactionIsSuccessfulField = null, transactionIsSuccessfulField = null, transactionListenerField = null;
                try {
                    mInnerTransactionIsSuccessfulField = SQLiteDatabase.class.getDeclaredField("mInnerTransactionIsSuccessful");
                    mInnerTransactionIsSuccessfulField.setAccessible(true);
                    innerTransactionIsSuccessful = mInnerTransactionIsSuccessfulField.getBoolean(sqLiteDatabase);

                    transactionIsSuccessfulField = SQLiteDatabase.class.getDeclaredField("mTransactionIsSuccessful");
                    transactionIsSuccessfulField.setAccessible(true);

                    transactionListenerField = SQLiteDatabase.class.getDeclaredField("mTransactionListener");
                    transactionListenerField.setAccessible(true);

                } catch (NoSuchFieldException e) {
                    Ln.e("beginTransaction NoSuchFieldException" + e.getStackTrace());
                } catch (IllegalAccessException e) {
                    Ln.e("beginTransaction IllegalAccessException" + e.getStackTrace());

                }
                getLockFromSQLiteDatabaseAndSetup();

                if (this.lock != null && this.lock.getHoldCount() > 1) {
                    if (innerTransactionIsSuccessful) {
                        String e = "Cannot call beginTransaction between calling setTransactionSuccessful and endTransaction";
                        IllegalStateException e1 = new IllegalStateException(e);
                        Ln.e("Database", "beginTransaction() failed", e1);
                        throw e1;
                    }
                    ok = true;
                    return;
                }

                sqLiteDatabase.execSQL("BEGIN IMMEDIATE;");
                try {
                    transactionListenerField.set(sqLiteDatabase, null);
                    transactionIsSuccessfulField.set(sqLiteDatabase, true);
                    mInnerTransactionIsSuccessfulField.set(sqLiteDatabase, false);
                    mInnerTransactionIsSuccessfulField.setAccessible(false);
                    transactionIsSuccessfulField.setAccessible(false);
                    transactionListenerField.setAccessible(false);
                } catch (IllegalAccessException e) {
                    Ln.e("beginTransaction IllegalAccessException" + e.getStackTrace());
                }
                ok = true;
            } finally {
                if (!ok) {
                    Method unlockForced = null;
                    try {
                        unlockForced = SQLiteDatabase.class.getDeclaredMethod("unlockForced");
                        unlockForced.setAccessible(true);
                        unlockForced.invoke(sqLiteDatabase);
                        unlockForced.setAccessible(false);
                    } catch (NoSuchMethodException e) {
                        Ln.e("beginTransaction NoSuchMethodException" + e.getStackTrace());
                    } catch (InvocationTargetException e) {
                        Ln.e("beginTransaction InvocationTargetException" + e.getStackTrace());
                    } catch (IllegalAccessException e) {
                        Ln.e("beginTransaction IllegalAccessException" + e.getStackTrace());
                    }
                }

            }

        }
    }

    public SQLiteDatabase getDatabase() {
        return sqLiteDatabase;
    }

    @Override
    public long insertWithOnConflict(String tablename, Object o, ContentValues values, int rule) {
        if (sqLiteDatabase != null) {
            return sqLiteDatabase.insertWithOnConflict(tablename, null, values, rule);
        }
        return 0;
    }

    @Override
    public void setTransactionSuccessful() {
        if (sqLiteDatabase != null) {
            sqLiteDatabase.setTransactionSuccessful();
        }
    }

    @Override
    public void endTransaction() {
        if (sqLiteDatabase != null) {
            sqLiteDatabase.endTransaction();
        }
    }

    @Override
    public void execSQL(String sql) {
        if (sqLiteDatabase != null) {
            sqLiteDatabase.execSQL(sql);
        }
    }

    @Override
    public int delete(String table, String selection, String[] selectionArgs) {
        if (sqLiteDatabase != null) {
            return sqLiteDatabase.delete(table, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public int updateWithOnConflict(String tablename, ContentValues values, String selection, String[] argsArray, int conflictRule) {
        if (sqLiteDatabase != null) {
            argsArray = DBHelperUtils.replaceNull(argsArray);
            return sqLiteDatabase.updateWithOnConflict(tablename, values, selection, argsArray, conflictRule);
        }
        return 0;
    }

    @Override
    public Cursor query(String tableName, String[] strings, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        if (sqLiteDatabase != null) {
            return sqLiteDatabase.query(tableName, strings, selection, selectionArgs, groupBy, having, orderBy);
        }
        return null;
    }

    @Override
    public int getVersion() {
        if (sqLiteDatabase != null) {
            return sqLiteDatabase.getVersion();
        }
        return 0;
    }

    @Override
    public void setVersion(int mTargetVersion) {
        if (sqLiteDatabase != null) {
            sqLiteDatabase.setVersion(mTargetVersion);
        }
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (sqLiteDatabase != null) {
            return sqLiteDatabase.rawQuery(sql, selectionArgs);
        }
        return null;
    }
}
