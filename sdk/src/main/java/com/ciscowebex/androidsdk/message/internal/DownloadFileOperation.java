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

package com.ciscowebex.androidsdk.message.internal;

import android.net.Uri;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ServiceReqeust;
import com.ciscowebex.androidsdk.internal.crypto.SecureContentReference;
import com.ciscowebex.androidsdk.internal.model.FileModel;
import com.ciscowebex.androidsdk.internal.model.ImageModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.message.MessageClient;
import com.ciscowebex.androidsdk.utils.http.ContentDownloadMonitor;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;

public class DownloadFileOperation {

    private ContentDownloadMonitor monitor = new ContentDownloadMonitor();

    private MessageClient.ProgressHandler progressHandler;
    private Authenticator authenticator;
    private FileModel sourceFile;

    public DownloadFileOperation(Authenticator authenticator, FileModel decryptedFile, MessageClient.ProgressHandler progressHandler) {
        this.authenticator = authenticator;
        this.sourceFile = decryptedFile;
        this.progressHandler = progressHandler;
    }

    public void run(File outFile, boolean thumbnail, CompletionHandler<Uri> callback) {
        String url;
        SecureContentReference scr;
        if (thumbnail) {
            ImageModel image = sourceFile.getImage();
            if (image == null) {
                callback.onComplete(ResultImpl.error("No thumbnial"));
                return;
            }
            url = image.getUrl();
            scr = image.getSecureContentReference();
        }
        else {
            url = sourceFile.getUrl();
            scr = sourceFile.getSecureContentReference();
        }
        doDownload(url, scr, outFile, callback);
    }

    private void doDownload(String url, SecureContentReference scr, File outFile, CompletionHandler<Uri> callback) {
        ServiceReqeust.make(url).get().auth(authenticator).model(Response.class).error(callback).async((Closure<Response>) response -> {
            ResponseBody body = response.body();
            if (body == null) {
                ResultImpl.errorInMain(callback, "Failed downloading from uri: " + url);
                return;
            }
            InputStream is = body.byteStream();
            InputStream in = scr != null ? scr.decrypt(is) : is;
            try {
                OutputStream out = new FileOutputStream(outFile);
                long contentLength = Long.parseLong(response.headers().get("Content-Length"));
                monitor.download(in, out, contentLength, progress -> {
                    if (progressHandler != null) {
                        Queue.main.run(() -> progressHandler.onProgress(progress));
                    }
                });
                ResultImpl.inMain(callback, Uri.fromFile(outFile));
            } catch (Throwable t) {
                ResultImpl.errorInMain(callback, t);
            }
        });
    }
}
