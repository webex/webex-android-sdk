package com.cisco.spark.android.log;

import com.github.benoitdion.ln.Ln;

import java.lang.Thread.UncaughtExceptionHandler;

import de.greenrobot.event.EventBus;

/**
 * Catch-all for unhandled exceptions in the application.  By registering an instance of this class
 * with:
 * Thread.currentThread().setUncaughtExceptionHandler(logUncaughtExceptionHandler);
 * we'll capture the exception in our log.
 * <p/>
 * Once done with our work, pass the exception to the previous, default exception handler so
 * the "Unfortunately, <your app> has stopped." dialog is displayed.  That handler is found by:
 * Thread.getDefaultUncaughtExceptionHandler();
 */
public class LogUncaughtExceptionHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler previousUEH = null;
    private LogFilePrint logFilePrint;
    private EventBus bus;

    public LogUncaughtExceptionHandler() {
    }

    public void setPreviousUncaughtExceptionHandler(UncaughtExceptionHandler previous) {
        previousUEH = previous;
    }

    public void uncaughtException(Thread t, Throwable e) {
        Ln.e(e, "FATAL EXCEPTION from thread: %s, caused application termination!", t.toString());
        if (bus != null) {
            bus.post(new UncaughExceptionEvent());
        }
        if (logFilePrint != null) {
            logFilePrint.stopCapture();
        }
        if (previousUEH != null) {
            previousUEH.uncaughtException(t, e);
        }
    }

    public void setLogFilePrint(LogFilePrint logFilePrint) {
        this.logFilePrint = logFilePrint;
    }

    public void setBus(EventBus bus) {
        this.bus = bus;
    }
}
