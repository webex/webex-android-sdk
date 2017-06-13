package com.cisco.spark.android.log;

import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.github.benoitdion.ln.Ln;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.inject.Singleton;

@Singleton
public class LogCallIndex {
    public static final String LOGFILE = "callindex.txt";
    private static final int MAX_SIZE = 10000;
    private File indexFile;

    public static final String JOINING = "JOINING";
    public static final String CONNECTED = "CONNECTED";
    public static final String CANCELED = "CANCELED";
    public static final String DECLINED = "DECLINED";
    public static final String BUSY = "BUSY";

    public LogCallIndex(LogFilePrint logFilePrint) {
        indexFile = new File(logFilePrint.getLogDirectory(), LOGFILE);
    }

    public File getLogCallIndexFile() {
        return indexFile;
    }

    public synchronized void addCall(String type, LocusData locusData, String trackingId) {
        LogCallIndexEntry callEntry = new LogCallIndexEntry(type, locusData, trackingId);
        new AddCallTask(callEntry).execute();
    }

    public LogCallIndexEntry getLastCallIndexEntry() {
        String lastCall = "";

        if (indexFile.exists()) {
            try {
                String currentCall;
                BufferedReader reader = new BufferedReader(new FileReader(indexFile));
                while ((currentCall = reader.readLine()) != null) {
                    lastCall = currentCall;
                }
            } catch (IOException x) {
                Ln.e("Error reading last line of call index file");
            }
        }

        return new LogCallIndexEntry(lastCall);
    }


    private class AddCallTask extends SafeAsyncTask<Object> {
        private LogCallIndexEntry callEntry;

        public AddCallTask(LogCallIndexEntry callEntry) {
            this.callEntry = callEntry;
        }

        @Override
        public Object call() {
            try {
                String message = callEntry.getIndexFileEntry();

                BufferedWriter writer = getCallLog();
                writer.write(message);
                writer.newLine();
                writer.close();

                Ln.d("Writing call index: %s", message);
            } catch (IOException ex) {
                Ln.e("Failed writing to call index log.");
            }
            return null;
        }

        private BufferedWriter getCallLog() throws IOException {
            if (indexFile.exists() && (indexFile.length() > MAX_SIZE))
                indexFile.delete();
            if (!indexFile.exists())
                indexFile.createNewFile();
            return new BufferedWriter(new FileWriter(indexFile, true));
        }
    }
}
