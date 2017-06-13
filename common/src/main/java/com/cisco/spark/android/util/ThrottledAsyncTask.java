package com.cisco.spark.android.util;

import android.os.Handler;
import android.os.Looper;

import com.github.benoitdion.ln.Ln;

import java.util.concurrent.RejectedExecutionException;

public abstract class ThrottledAsyncTask {

    /**
     * A simple class to solve the problem of too many asynctasks spinning up at once. The max queue
     * size on the shared executor is 128 and we can hit that quickly during busy times as for
     * example the FTS indexing task is launched each time something changes. We don't need to
     * update the search index more than once per second.
     * <p/>
     * To use, instantiate and implement doInBackground(). Then call scheduleExecute().
     */

    private static final long THROTTLE_MS = 200;
    private long nextExecutionTime;
    private long throttle;
    private ThrottledTaskRunnable throttledTaskRunnable;
    private Handler handler;
    private boolean isRunning;
    private RuntimeException createdHereStacktrace;

    public ThrottledAsyncTask(long throttle) {
        this.throttle = throttle;
        createdHereStacktrace = new RuntimeException("ThrottledAsyncTask Created Here");
    }

    public ThrottledAsyncTask() {
        this(THROTTLE_MS);
    }

    abstract protected void doInBackground();

    // Override as needed
    protected void onPreExecute() {
    }

    protected void onSuccess() {
    }

    protected void onInterrupted(Exception e) {
        Ln.i(e);
    }

    protected void onException(Exception e) {
        Ln.i(e);
    }

    protected void onThrowable(Throwable t) {
        Ln.i(t);
    }

    protected void onFinally() {
    }

    /**
     * Schedule a new async task. If it has been at least <code>throttle</code> ms since
     * the last execution, it will be launched immediately. Otherwise it will be scheduled
     * to launch at the earliest execution time that respects the throttle.
     */
    public void scheduleExecute() {
        long toWait = nextExecutionTime - System.currentTimeMillis();

        if (toWait <= 0) {
            nextExecutionTime = System.currentTimeMillis() + throttle;
            // call run directly
            new ThrottledTaskRunnable().run();
            return;
        }

        if (throttledTaskRunnable != null) {
            getMainLooperHandler().removeCallbacks(throttledTaskRunnable);
        }

        throttledTaskRunnable = new ThrottledTaskRunnable();
        getMainLooperHandler().postDelayed(throttledTaskRunnable, toWait);
    }

    public void cancel() {
        if (throttledTaskRunnable != null)
            getMainLooperHandler().removeCallbacks(throttledTaskRunnable);
    }

    public void cancelAndWait() {
        cancel();
        while (isRunning) {
            try {
                Thread.sleep(THROTTLE_MS);
            } catch (InterruptedException ex) {
            }
        }
    }

    private class ThrottledTaskRunnable implements Runnable {
        @Override
        public void run() {
            nextExecutionTime = System.currentTimeMillis() + throttle;

            // Check to make sure throttled tasks do not overlap
            if (isRunning) {
                scheduleExecute();
                return;
            }

            try {
                new SafeAsyncTask<Void>() {

                    @Override
                    public Void call() throws Exception {
                        isRunning = true;
                        try {
                            ThrottledAsyncTask.this.doInBackground();
                        } catch (Exception e) {
                            throw e;
                        } finally {
                            isRunning = false;
                        }

                        return null;
                    }

                    @Override
                    protected void onPreExecute() throws Exception {
                        ThrottledAsyncTask.this.onPreExecute();
                    }

                    @Override
                    protected void onSuccess(Void aVoid) throws Exception {
                        ThrottledAsyncTask.this.onSuccess();
                    }

                    @Override
                    protected void onInterrupted(Exception e) {
                        ThrottledAsyncTask.this.onInterrupted(e);
                    }

                    @Override
                    protected void onException(Exception e) throws RuntimeException {
                        ThrottledAsyncTask.this.onException(e);
                    }

                    @Override
                    protected void onThrowable(Throwable t) throws RuntimeException {
                        ThrottledAsyncTask.this.onThrowable(t);
                    }

                    @Override
                    protected void onFinally() throws RuntimeException {
                        isRunning = false;
                        ThrottledAsyncTask.this.onFinally();
                    }
                }.execute();
            } catch (RejectedExecutionException e) {
                Ln.w(e);
                Ln.w(createdHereStacktrace, "Failed executing ThrottledAsyncTask that was created here:");
                scheduleExecute();
            }
        }
    }

    private Handler getMainLooperHandler() {
        if (handler == null)
            handler = new Handler(Looper.getMainLooper());
        return handler;
    }
}
