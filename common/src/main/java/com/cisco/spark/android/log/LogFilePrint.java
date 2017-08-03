package com.cisco.spark.android.log;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.core.PSUtils;
import com.cisco.spark.android.media.MediaSessionEngine;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.ZipUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.inject.Inject;

/**
 * LogFilePrint reads from logcat and writes the output to files.
 *
 * Internal FSLP creates and writes logs to a rolling set of files in the apps 'files/logs' storage.
 * The number and size of files is currently hard-coded below and start at zero (e.g. files/logs/wx2log0.txt)
 * If the logs/ zipDirectory does not exist, it is created.  If an available log index does not exist
 * (wx2log[0-N].txt) it will be created and used.  If the whole range of log files exist, then the
 * oldest file will be found and logging and rotation will commence from there.
 *
 * TODO: The file rolling functionality is home-grown.  Java offers a more full-featured implementation
 * that could/should be investigated.
 *
 * FSLP implements the println() method of Print to handle logging requests and write them to the
 * current log file.
 *
 * Externally, FSLP acts as a FileProvider when logs need to be shared to other apps.  The AndroidManifest.xml
 * defines a (FileProvider) <provider>.  NOTE: The authorities parameter must match LOG_FILE_PROVIDER.
 * That block also points to the file_providerider.xml file, which provides URI indirection to the storage
 * location.  This XML indicates that our ZIP_FILE_NAME will can be referenced by:
 *  X,
 * while it is stored in:
 *  Y
 *
 * Finally, an external ZipUtils class was created to compress all the files in the logs/ zipDirectory into
 * one single ZIP file.  When getZippedLogsUri() is called, a recursive zip of logs/ is done to produce
 * one file in the files/ zipDirectory.  This is referenced by getUriForFile() to generate a URI another
 * app can use to access the FileProvider and retrieve the file.
 *
 * NOTE: Be aware that changing the LOG_FILE_PROVIDER will require corresponding changes to these
 * XML files.
 */
public class LogFilePrint implements Runnable {
    protected static final String TAG = "LogFilePrint";
    private static final String LOG_DIR = "logs";
    protected static final String LOG_FILE_NAME = "wx2log";
    protected static final String LOG_FILE_EXT = ".txt";
    protected static final String ZIP_FILE_NAME = "wx2logs.zip";
    private static final int  MAX_DEBUG_LOG_FILES = 5;
    private static final int  MAX_RELEASE_LOG_FILES = 5;
    protected static final long MAX_LOG_FILE_SIZE = 2 * 1024 * 1024; // 2MB file size

    protected Context context;
    protected Process logcatProcess;
    protected File logFile;
    private String rootLogDir;
    protected int maxLogFiles;
    protected int currentLogFileSize;
    protected AtomicBoolean running;
    private Thread captureThread;
    private LogCallIndex logCallIndex;
    // For test automation
    private AtomicReferenceArray<String> watchForStrings = null; // new AtomicReferenceArray<String>(1);
    protected AtomicReferenceArray<Boolean> watchForStringsFound = null; // new AtomicBoolean(false);

    @Inject
    public LogFilePrint(Context context) {
        this.context = context;
        this.running = new AtomicBoolean(false);
        rootLogDir = context.getCacheDir().toString();

        // The upper range of log files MUST be set before getNewLogFile() is called.
        if (BuildConfig.DEBUG) {
            maxLogFiles = MAX_DEBUG_LOG_FILES;
        } else {
            maxLogFiles = MAX_RELEASE_LOG_FILES;
        }

        this.logCallIndex = new LogCallIndex(this);
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

    public Uri generateZippedLogs() {
        return generateZippedLogs(false);
    }

    public Uri generateZippedLogs(boolean isUpload) {
        File zipFile = getZipFile();
        if (zipFile != null) {
            // TODO: Temporary?  If a team member, they may have captured audio samples, so pull them into directory before zipping
            moveAudioSampleFile();
            // Zip the log directory contents to the specified file
            ZipUtils.zipDirectory(getLogDirectory(), getZipFile());
            // TODO: Remove when no longer needed!
            removeAudioSampleFiles();
            if (isUpload) {
                return Uri.fromFile(getZipFile());
            } else {
                return FileProvider.getUriForFile(context, ContentManager.FILEPROVIDER_AUTHORITY, getZipFile());
            }
        } else {
            return null;
        }
    }

    public File getZipFile() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return new File(FileUtils.getCacheDir(context), ZIP_FILE_NAME);
        } else {
            return null;
        }
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

    public LogCallIndex getCallIndex() {
        return logCallIndex;
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

    //-------------------------------------------------------------------------
    // Private helper methods
    //-------------------------------------------------------------------------
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
            File toFile   = buildLogFile(i);
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
                if (result.getPpid() == 1) {
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

    protected void capture() {
        running.set(true);
        boolean restart = false;
        try {
            // Remove any orphaned logcat instances before starting a new one.
            cleanupLogcat();

            // Then, start logcat streaming
            String[] streamLogcat = { "logcat", "-v", "threadtime" };
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
                                    if (isWatchingForStrings()) {
                                        processLineForWatchedStrings(inputLine);
                                    }
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

    // The following methods exist for automated testing...
    public void watchFor(String msg) {
        setWatchForStrings(new String[]{msg});
    }

    public void watchFor(String[] msgs) {
        setWatchForStrings(msgs);
    }

    public boolean watchForWait(int maxWaitMsecs) {
        Log.i(TAG, "Waiting " + Integer.toString(maxWaitMsecs) + " msecs for 'log watch' string(s) to appear");
        int waitCount = 10;
        int waitInterval = maxWaitMsecs / waitCount;
        boolean found = false;

        while (!found && waitCount > 0) {
            Log.i(TAG, "Check " + Integer.toString(waitCount) + " for 'log watch' strings");
            try {
                Thread.sleep(waitInterval);
                waitCount--;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            found = watchForAllStringsFound();
        }

        // If string(s) were not found, save them to a string before emptying watch list.
        // Watch list MUST BE EMPTIED BEFORE LOGGING, or the search will then find them!
        String stringsNotFound = "";
        if (!found)
            stringsNotFound = reportWatchedForStringsNotFound();

        // Reset variables
        watchForStrings = null;
        watchForStringsFound = null;

        if (found)
            Log.i(TAG, "'Log watch' string(s) were found");
        else
            Log.i(TAG, "'Log watch' string(s) were not found (" + stringsNotFound + ")");

        return found;
    }


    private void setWatchForStrings(String [] messages) {
        watchForStrings = new AtomicReferenceArray<String>(messages.length);
        watchForStringsFound = new AtomicReferenceArray<Boolean>(messages.length);

        for (int i = 0; i < messages.length; i++) {
            watchForStrings.getAndSet(i, messages[i]);
            watchForStringsFound.getAndSet(i, false);
        }
    }

    private boolean isWatchingForStrings() {
        return (watchForStrings != null);
    }

    private void processLineForWatchedStrings(String line) {
        for (int i = 0; i < watchForStrings.length(); i++) {
            if (!watchForStringsFound.get(i) && line.contains(watchForStrings.get(i))) {
                watchForStringsFound.getAndSet(i, true);
            }
        }
    }

    private boolean watchForAllStringsFound() {
        for (int i = 0; i < watchForStringsFound.length(); i++) {
            if (!watchForStringsFound.get(i)) {
                return false;
            }
        }
        return true;
    }

    private String reportWatchedForStringsNotFound() {
        String msg = "";
        for (int i = 0; i < watchForStrings.length(); i++) {
            if (!watchForStringsFound.get(i))
                msg += watchForStrings.get(i) + ", ";
        }
        return msg.substring(0, msg.length() - 2);
    }

    private void moveAudioSampleFile() {
        Log.i(TAG, "audio sampling file search...");
        File dstDir = getLogDirectory();
        File srcDir = Environment.getExternalStorageDirectory();

        File[] files = srcDir.listFiles(audioSamplingFilter);
        int fileCount = (files != null) ? files.length : 0;
        Log.i(TAG, "found " + fileCount + " files to include in feedback report");

        if (fileCount == 0) {
            return;
        }

        for (File file : files) {
            File srcFile = new File(srcDir, file.getName());
            File dstFile = new File(dstDir, file.getName());
            try {
                Log.d(TAG, "moving " + srcFile.getAbsolutePath() + " to " + dstFile.getAbsolutePath());
                FileUtils.copyFile(srcFile, dstFile);
                if (!srcFile.delete()) {
                    Log.e(TAG, "Error copying audio sample files");
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error copying audio sample files", ex);
            }
        }
    }

    private void removeAudioSampleFiles() {
        Log.i(TAG, "removing any existing audio sampling files");
        File[] pcmFiles = getLogDirectory().listFiles(audioSamplingFilter);
        for (File file : pcmFiles) {
            if (!file.delete()) {
                Log.e(TAG, "Unable to delete " + file.getName());
            }
        }
    }

    private FilenameFilter audioSamplingFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.toLowerCase(Locale.US).endsWith("wav") &&
                   !filename.equals(MediaSessionEngine.AUDIO_MEDIA_INPUT_FILE);
        }
    };
}
