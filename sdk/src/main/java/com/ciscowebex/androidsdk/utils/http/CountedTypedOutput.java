/*
 * Copyright 2016-2020 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.utils.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.ciscowebex.androidsdk.internal.Closure;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class CountedTypedOutput extends RequestBody {
    private static final int BUFFER_SIZE = 4096;
    private final String mimeType;
    protected final File file;

    private int percentage;
    private boolean complete;
    private Closure<Long> callback;

    public CountedTypedOutput(String mimeType, File file, Closure<Long> callback) {
        this.mimeType = mimeType;
        this.file = file;
        this.callback = callback;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    public int getProgress() {
        return percentage;
    }

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
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            long bytesRead = 0;
            long size = file.length();
            int read;
            while ((read = fileInputStream.read(buffer)) != -1) {
                bytesRead += read;
                percentage = (int) ((bytesRead / (double) size) * 100);
                if (callback != null) {
                    callback.invoke(bytesRead);
                }
                out.write(buffer, 0, read);
            }
            complete = true;
        }
    }
}
