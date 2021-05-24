/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.utils;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.text.TextUtils;

import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LoggingLock extends ReentrantLock {

    private static final long serialVersionUID = -2955432306737992011L;

    private long lockAcquired;

    // Warn if it takes longer than this to successfully acquire a lock on the main thread.
    private static final long MAINTHREAD_LOCKWAIT_WARN_MS = 30;

    // Warn if it takes longer than this to successfully acquire a lock.
    private static final long LOCKWAIT_WARN_MS = TimeUnit.SECONDS.toMillis(5);

    // Warn if we are still waiting to acquire a lock after this duration.
    private static long deadlockWarnWaitTime = TimeUnit.SECONDS.toMillis(10);

    private long warnWaitTime;
    private String lockname;
    private LongHoldException lastLongHoldException;
    private boolean isMainThreadWaiting;
    private boolean isDebug;

    public ArrayList<Throwable> exceptions = new ArrayList<>();

    // Logging to ensure that locks are always acquired in the same order to avoid deadlocks.
    protected static final boolean AUDIT_LOCK_ORDER = true;
    @SuppressLint("UseSparseArrays")
    private static HashMap<Long, LinkedList<LoggingLock>> locksByThread = new HashMap<>();
    private static HashMap<LoggingLock, HashSet<LoggingLock>> locksLockedAfterLock = new HashMap<>();

    private static long mainThreadId = 1;

    public LoggingLock(boolean isDebug, String name, long warnTimeout) {
        super(true);
        lockname = name;
        warnWaitTime = warnTimeout;
        this.isDebug = isDebug;
        Ln.d("$PERF LoggingLock initialized in " + (isDebug ? "debug" : "release") + " mode");
        locksLockedAfterLock.put(this, new HashSet<>());
        try {
            mainThreadId = Looper.getMainLooper().getThread().getId();
        } catch (Throwable ignored) {
        }
    }

    public LoggingLock(boolean isDebug, String name) {
        this(isDebug, name, LOCKWAIT_WARN_MS);
    }

    public static void setDeadlockWarnWaitTime(long deadlockWarnWaitTime) {
        LoggingLock.deadlockWarnWaitTime = deadlockWarnWaitTime;
    }

    public void lock() {

        if (!isDebug) {
            super.lock();
            return;
        }

        long lockRequested = System.currentTimeMillis();
        long tid = Thread.currentThread().getId();

        if (tid == mainThreadId) {
            isMainThreadWaiting = true;
        }

        // If we can't get the lock within 10 seconds, it might be a deadlock. Log an exception and continue waiting.
        try {
            boolean gotLock = super.tryLock(deadlockWarnWaitTime, TimeUnit.MILLISECONDS);
            if (!gotLock) {
                try {
                    throw new LongWaitException("$PERF Really Long Wait for " + lockname);
                } catch (Throwable e) {
                    if (exceptions.size() < 100)
                        exceptions.add(e);
                    Ln.w(e, String.format("$PERF Warning: Really Long Wait for lock %s held by %s",
                            lockname, getOwner() == null ? "none" : getOwner().getName()));
                }
                lockRequested = System.currentTimeMillis();
                super.lock();
            }
        } catch (InterruptedException e1) {
            Ln.e(e1, "InterruptedException: ");
            throw new RuntimeException(e1);
        } finally {
            lockAcquired = System.currentTimeMillis();
            if (tid == mainThreadId) {
                isMainThreadWaiting = false;
            }
        }

        // Ensure that locks are always held in the same order to avoid deadlocks
        if (AUDIT_LOCK_ORDER) {
            LinkedList<LoggingLock> alreadyHeldLocks = locksByThread.get(tid);
            if (alreadyHeldLocks == null) {
                alreadyHeldLocks = new LinkedList<>();
                locksByThread.put(tid, alreadyHeldLocks);
            }

            if (!alreadyHeldLocks.contains(this)) {
                for (LoggingLock alreadyHeldLock : alreadyHeldLocks) {
                    if (alreadyHeldLock != this)
                        locksLockedAfterLock.get(alreadyHeldLock).add(this);

                    if (locksLockedAfterLock.get(this).contains(alreadyHeldLock)) {
                        try {
                            throw new LockOrderException("Inconsistent Lock Order between " + lockname + " and " + alreadyHeldLock.lockname);
                        } catch (Exception e) {
                            Ln.w(e, "Lock ordering of " + this.lockname + " and " + alreadyHeldLock.lockname + " is not consistent");
                            if (exceptions.size() < 100)
                                exceptions.add(e);
                        }
                    }
                }
            }

            alreadyHeldLocks.add(this);
        }

        long waitTime = lockAcquired - lockRequested;

        try {
            if (tid == mainThreadId && waitTime > MAINTHREAD_LOCKWAIT_WARN_MS) {
                throw new BlockingMainThreadException("$PERF Waited " + waitTime + " ms for lock " + lockname + " on main thread");
            } else if (waitTime > warnWaitTime) {
                throw new LongWaitException("$PERF Long Wait for lock " + lockname);
            }
        } catch (Throwable e) {
            if (exceptions.size() < 100)
                exceptions.add(e);
            Ln.d(e, "$PERF Warning: Waited " + waitTime + " for lock " + lockname);
            Ln.d(lastLongHoldException, "$PERF Warning: Long Hold for lock " + lockname);
        }
    }

    public void unlock() {

        if (!isDebug || lockAcquired == 0) {
            super.unlock();
            return;
        }

        try {
            long holdTime = System.currentTimeMillis() - lockAcquired;

            long maxWaitTime = isMainThreadWaiting ? MAINTHREAD_LOCKWAIT_WARN_MS : warnWaitTime;

            if (holdTime > maxWaitTime) {
                try {
                    throw new LongHoldException("$PERF Long Hold: " + holdTime + "ms for lock " + lockname);
                } catch (LongHoldException e) {
                    if (exceptions.size() < 100)
                        exceptions.add(e);
                    lastLongHoldException = e;
                }
            }

            if (AUDIT_LOCK_ORDER) {
                long tid = Thread.currentThread().getId();
                LinkedList<LoggingLock> alreadyHeldLocks = locksByThread.get(tid);

                if (alreadyHeldLocks.getLast() != this) {
                    try {
                        throw new LockOrderException("Releasing lock " + lockname + " from the middle of the list " + TextUtils.join(" => ", alreadyHeldLocks));
                    } catch (Exception e) {
                        if (exceptions.size() < 100)
                            exceptions.add(e);
                        Ln.w(e, "Releasing lock " + lockname + " from the middle of the list");
                    }
                }
                alreadyHeldLocks.removeLastOccurrence(this);
            }
        } catch (Throwable e) {
            Ln.e(e);
        } finally {
            super.unlock();
        }
    }

    public String toString() {
        return lockname;
    }

    public class LongHoldException extends RuntimeException {
        public LongHoldException(String s) {
            super(s);
        }
    }

    public class LongWaitException extends RuntimeException {
        public LongWaitException(String s) {
            super(s);
        }
    }

    public class LockOrderException extends RuntimeException {
        public LockOrderException(String s) {
            super(s);
        }
    }

    public class BlockingMainThreadException extends RuntimeException {
        public BlockingMainThreadException(String s) {
            super(s);
        }
    }
}
