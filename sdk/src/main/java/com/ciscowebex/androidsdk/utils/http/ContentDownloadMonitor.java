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

package com.ciscowebex.androidsdk.utils.http;

import com.ciscowebex.androidsdk.internal.Closure;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;

public class ContentDownloadMonitor {

    public enum DownloadState {
        START, COMPLETE, CANCEL, FAIL
    }

    private final static long NO_LENGTH = -1;
    private final static int KB = 1024;
    private long contentLength = NO_LENGTH;
    private long downloadedLength = NO_LENGTH;

    private volatile DownloadState state;

    public ContentDownloadMonitor() {
        setDownloadState(DownloadState.START);
    }

    public DownloadState getState() {
        return state;
    }

    public float getProgress() {
        if (contentLength == NO_LENGTH) {
            return -1;
        }
        if (downloadedLength == NO_LENGTH) {
            return 0;
        }
        BigDecimal downloadedDecimal = new BigDecimal(downloadedLength);
        BigDecimal contentDecimal = new BigDecimal(contentLength);
        return downloadedDecimal.divide(contentDecimal, 2, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    public void cancelDownload() {
        setDownloadState(DownloadState.CANCEL);
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getDownloadedLength() {
        return downloadedLength;
    }

    public boolean isCanceled() {
        return state.equals(DownloadState.CANCEL);
    }

    private void setDownloadState(DownloadState state) {
        this.state = state;
    }

    public void download(InputStream is, OutputStream os, long contentLength, Closure<Long> callback) throws IOException {
        this.contentLength = contentLength;
        byte [] data = new byte[KB];
        InputStream bis = new BufferedInputStream(is, KB * 2);
        int count;
        try {
            while ((count = bis.read(data)) != -1 && !isCanceled()) {
                downloadedLength += count;
                os.write(data, 0, count);
                if (callback != null) {
                    callback.invoke(downloadedLength);
                }
            }
            os.flush();
            os.close();
            bis.close();
            if (!isCanceled()) {
                setDownloadState(DownloadState.COMPLETE);
            }

        } catch (Throwable e) {
            setDownloadState(DownloadState.FAIL);
            throw e;
        }
    }
}
