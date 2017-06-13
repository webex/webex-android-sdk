package com.cisco.spark.android.client;

import com.cisco.spark.android.content.ContentUploadMonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;


public class CountedTypedOutput extends RequestBody implements ContentUploadMonitor.ContentUploadProgressProvider {
    private static final int BUFFER_SIZE = 4096;
    private final String mimeType;
    protected final File file;

    private int percentage;
    private boolean complete;

    public CountedTypedOutput(String mimeType, File file) {
        this.mimeType = mimeType;
        this.file = file;
    }

    @Override
    public int getProgress() {
        return percentage;
    }

    @Override
    public boolean isCompleted() {
        return complete;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(mimeType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        writeTo(sink.outputStream());
    }

    protected void writeTo(OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        FileInputStream fileInputStream = new FileInputStream(file);
        long bytesRead = 0;

        try {
            int read;

            while ((read = fileInputStream.read(buffer)) != -1) {
                bytesRead += read;
                long size = file.length();
                double percent = (bytesRead / (double) size);

                percentage = (int) (percent * 100);

                out.write(buffer, 0, read);
            }

            complete = true;
        } finally {
            fileInputStream.close();
        }
    }
}
