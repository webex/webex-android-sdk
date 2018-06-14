package com.ciscowebex.androidsdk.utils.log;

import android.content.Context;
import android.util.Log;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.core.PSUtils;
import com.cisco.spark.android.util.FileUtils;
import com.ciscowebex.androidsdk.Webex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * LogFilePrint reads from logcat and writes the output to files.
 * <p>
 * Internal FSLP creates and writes logs to a rolling set of files in the apps 'files/logs' storage.
 * The number and size of files is currently hard-coded below and start at zero (e.g. files/logs/wx2log0.txt)
 * If the logs/ zipDirectory does not exist, it is created.  If an available log index does not exist
 * (wx2log[0-N].txt) it will be created and used.  If the whole range of log files exist, then the
 * oldest file will be found and logging and rotation will commence from there.
 * <p>
 * TODO: The file rolling functionality is home-grown.  Java offers a more full-featured implementation
 * that could/should be investigated.
 * <p>
 * FSLP implements the println() method of Print to handle logging requests and write them to the
 * current log file.
 * <p>
 * Externally, FSLP acts as a FileProvider when logs need to be shared to other apps.  The AndroidManifest.xml
 * defines a (FileProvider) <provider>.  NOTE: The authorities parameter must match LOG_FILE_PROVIDER.
 * That block also points to the file_providerider.xml file, which provides URI indirection to the storage
 * location.  This XML indicates that our ZIP_FILE_NAME will can be referenced by:
 * X,
 * while it is stored in:
 * Y
 * <p>
 * Finally, an external ZipUtils class was created to compress all the files in the logs/ zipDirectory into
 * one single ZIP file.  When getZippedLogsUri() is called, a recursive zip of logs/ is done to produce
 * one file in the files/ zipDirectory.  This is referenced by getUriForFile() to generate a URI another
 * app can use to access the FileProvider and retrieve the file.
 * <p>
 * NOTE: Be aware that changing the LOG_FILE_PROVIDER will require corresponding changes to these
 * XML files.
 */

public class LogCaptureUtil implements Runnable {
    protected static final String TAG = "LogFilePrint";
    private static final String LOG_DIR = "logs";
    protected static final String LOG_FILE_NAME = "ssdk";
    protected static final String LOG_FILE_EXT = ".log";
    private static final int MAX_DEBUG_LOG_FILES = 5;
    private static final int MAX_RELEASE_LOG_FILES = 5;
    protected static final long MAX_LOG_FILE_SIZE = 20 * 1024 * 1024; // 20MB file size

    protected Context context;
    protected Process logcatProcess;
    protected File logFile;
    private String rootLogDir;
    protected int maxLogFiles;
    protected int currentLogFileSize;
    protected AtomicBoolean running;
    private Thread captureThread;

    public LogCaptureUtil(Context context) {
        this.context = context;
        this.running = new AtomicBoolean(false);
        rootLogDir = context.getExternalCacheDir().toString();

        // The upper range of log files MUST be set before getNewLogFile() is called.
        if (BuildConfig.DEBUG) {
            maxLogFiles = MAX_DEBUG_LOG_FILES;
        } else {
            maxLogFiles = MAX_RELEASE_LOG_FILES;
        }
    }

    private Webex.LogLevel logLevel = Webex.LogLevel.INFO;

    public void setLogLevel(Webex.LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public void startCapture() {
        if (!running.get()) {
            Log.d(TAG, "Starting log capture");
            captureThread = new Thread(this, "Log print");
            captureThread.start();
        }
    }

    public void stopCapture() {
        if (running.get()) {
            running.set(false);
            // Send a log message to break out of any blocked input reads.
            Log.i(TAG, "Logging stopped");
            stopCaptureThread();
            stopLogcat();
        }
    }

    public void restartCapture() {
        stopCapture();
        startCapture();
    }

    public File getCurrentLogFile() {
        return logFile;
    }

    public synchronized String getLogString() {
        String result = "";
        for (int i = maxLogFiles - 1; i >= 0; i--) {
            File logFile = buildLogFile(i);
            if (logFile.exists()) {
                result += FileUtils.readFile(logFile);
            }
        }
        return result;
    }

    public File[] getAllLogFiles() {
        return getLogDirectory().listFiles();
    }

    public File getLogDirectory() {
        return FileUtils.mkdir(getLogDirectoryName());
    }

    public void run() {
        capture();
    }

    public void clearLogs() {
        stopCapture();
        for (int i = 0; i < maxLogFiles; i++) {
            File logFile = buildLogFile(i);
            if (logFile.exists())
                if (!logFile.delete()) {
                    Log.e(TAG, "Unable to delete log file " + logFile.getName());
                }
        }
        startCapture();
    }

    private void stopCaptureThread() {
        if (captureThread != null) {
            if (captureThread.isAlive()) {
                // This probably doesn't do anything since our io mechanism for logging capture is not interruptible!
                captureThread.interrupt();
            }
            captureThread = null;
        }
    }

    protected void stopLogcat() {
        if (logcatProcess != null) {
            logcatProcess.destroy();
            logcatProcess = null;
        }
    }

    private void getNewLogFile() {
        File logDir = getLogDirectory();
        if (!logDir.exists()) {
            if (!logDir.mkdirs())
                Log.e(TAG, "Failed to make directory");
        }

        rotateLogFiles();
        logFile = buildLogFile(0);
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    Log.e(TAG, "Failed to create log file: " + logFile.getAbsolutePath());
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to create log file: " + logFile.getAbsolutePath(), e);
            }
        }
        currentLogFileSize = 0;
    }

    private void rotateLogFiles() {
        // File '0' is always most current, so we rotate (rename) files down in numbers
        // until the max is reached.  At which point, we delete the tail/oldest.
        int startIndex = maxLogFiles - 1;
        for (int i = startIndex; i > 0; i--) {
            File fromFile = buildLogFile(i - 1);
            File toFile = buildLogFile(i);
            if (toFile.exists() && (i == startIndex)) {
                if (!toFile.delete())
                    Log.e(TAG, "Failed to delete 'to' file");
            }
            if (fromFile.exists()) {
                if (!fromFile.renameTo(toFile))
                    Log.e(TAG, "Failed to rename file");
            }
        }
    }

    private File buildLogFile(int i) {
        return new File(getLogDirectoryName() + LOG_FILE_NAME + Integer.toString(i) + LOG_FILE_EXT);
    }

    protected String getLogDirectoryName() {
        return (rootLogDir + File.separator + LOG_DIR + File.separator);
    }

    protected void cleanupLogcat() {
        PSUtils.iteratePSresults("logcat", null, new PSUtils.PSResultHandler() {
            @Override
            public void onResult(PSUtils.PSResult result) {
                if (result.getPpid() == android.os.Process.myPid()) {
                    Log.d(TAG, "Removing orphaned logcat process ID: " + result.getPid());
                    // NOTE: We only have permission to kill processes we created
                    android.os.Process.killProcess(result.getPid());
                }
            }

            @Override
            public void onException(Exception ex) {
            }
        });
    }

    private static final String processId = Integer.toString(android.os.Process.myPid());

    protected void capture() {
        running.set(true);
        boolean restart = false;
        try {
            // Remove any orphaned logcat instances before starting a new one.
            cleanupLogcat();

            // Then, start logcat streaming
            String logFilter = "*:D";
            if (logLevel != null) {
                switch (logLevel) {
                    case NO:
                        logFilter = "*:S";
                        break;
                    case ERROR:
                        logFilter = "*:E";
                        break;
                    case WARNING:
                        logFilter = "*:W";
                        break;
                    case INFO:
                        logFilter = "SQ:W wels:W *:I";
                        break;
                    case DEBUG:
                        logFilter = "SQ:W wels:W UTIL:W *:D";
                        break;
                    case VERBOSE:
                        logFilter = "*:V";
                        break;
                    case ALL:
                        logFilter = "*:V";
                }
            }
            String[] streamLogcat = {"logcat", "-v", "threadtime", logFilter};
            logcatProcess = new ProcessBuilder()
                    .command(streamLogcat)
                    .redirectErrorStream(true)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
            try {
                String inputLine;
                while (running.get() && !restart) {
                    getNewLogFile();
                    FileOutputStream writer = new FileOutputStream(logFile);
                    Log.v(TAG, "Opening log file: " + logFile.getPath());
                    try {
                        boolean fileNotFull = true;
                        while (fileNotFull && !restart) {
                            inputLine = reader.readLine();
                            if (inputLine != null) {
                                if (inputLine.contains("Unable to open log device '/dev/log/main': Permission denied")) {
                                    // This means we don't have the permissions to read from logcat.
                                    // It typically happens on custom roms such as Cyanogenmod.
                                    // Not much we can do in this scenario so kill the logcat process and break out of the loop.
                                    stopLogcat();
                                    running.set(false);
                                    break;
                                } else if (logLevel != Webex.LogLevel.ALL && !inputLine.contains(processId)) {
                                    continue;
                                }
                                writer.write(inputLine.getBytes());
                                writer.write('\n');
                                currentLogFileSize += inputLine.length();
                                if (currentLogFileSize > MAX_LOG_FILE_SIZE) {
                                    Log.v(TAG, "Log file has reached max size. Closing and rotating files now.");
                                    fileNotFull = false;
                                }

                                if (BuildConfig.DEBUG) {
                                    // (For test automation) watch for a string appearance in the input stream
                                    writer.flush();
                                }
                            } else {
                                Log.w("wx2", "Logcat process has died, restart the capture.");
                                writer.write("Logcat process has died, restart the capture.\n".getBytes());
                                restart = true;
                            }
                            if (!running.get()) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "Error during read/write of logs", e);
                    } finally {
                        try {
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            Log.d(TAG, "Error closing log writer", e);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error capturing logcat output:", e);
        }
        if (restart) {
            restartCapture();
        } else {
            Log.v(TAG, "Log capture terminated");
        }
    }
}
