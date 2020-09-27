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

package com.ciscowebex.androidsdk.message.internal;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.crypto.TypedSecureContentReferenceOutput;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.queue.SerialQueue;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import com.ciscowebex.androidsdk.internal.crypto.SecureContentReference;
import com.ciscowebex.androidsdk.internal.model.FileModel;
import com.ciscowebex.androidsdk.internal.model.ImageModel;
import com.ciscowebex.androidsdk.internal.model.UploadSessionModel;
import com.ciscowebex.androidsdk.message.LocalFile;
import com.ciscowebex.androidsdk.utils.MimeUtils;
import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.Checker;
import me.helloworld.utils.Objects;
import me.helloworld.utils.collection.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UploadFileOperations {

    private SerialQueue queue = new SerialQueue(SerialQueue.Mode.BACKGROUND);
    private List<Operation> operations = new ArrayList<>();

    public UploadFileOperations(List<LocalFile> locals) {
        if (locals != null) {
            for (LocalFile local : locals) {
                operations.add(new Operation(local));
            }
        }
    }

    public void run(Authenticator authenticator, Device device, String convUrl, KeyObject key, CompletionHandler<List<FileModel>> callback) {
        if (Checker.isEmpty(operations)) {
            callback.onComplete(ResultImpl.success(Collections.emptyList()));
            return;
        }
        getSpaceUrl(authenticator, device, convUrl, spaceUrl -> {
            if (spaceUrl == null || spaceUrl.getData() == null) {
                callback.onComplete(ResultImpl.error(spaceUrl));
                return;
            }
            List<FileModel> success = new ArrayList<>();
            for (Operation op : operations) {
                if (!op.done) {
                    queue.run(() -> op.run(authenticator, spaceUrl.getData(), key, result -> {
                        if (result.getData() != null) {
                            success.add(result.getData());
                        }
                        queue.yield();
                    }));
                }
            }
            queue.run(() -> callback.onComplete(ResultImpl.success(success)));
        });
    }

    private static class Operation {

        private LocalFile local;

        private boolean done = false;

        private Operation(LocalFile local) {
            this.local = local;
        }

        private void run(Authenticator authenticator, String spaceUrl, KeyObject key, CompletionHandler<FileModel> callback) {
            doUpload(authenticator, spaceUrl, local.getFile(), false, uploadResult -> {
                if (uploadResult.getData() == null || uploadResult.getError() != null) {
                    callback.onComplete(ResultImpl.error(uploadResult));
                    return;
                }
                FileModel uploaded = uploadResult.getData();
                uploaded.setDisplayName(local.getName());
                uploaded.setMimeType(local.getMimeType());
                LocalFile.Thumbnail thumbnail = local.getThumbnail();
                if (thumbnail != null && thumbnail.getFile() != null) {
                    doUpload(authenticator, spaceUrl, thumbnail.getFile(), true, thumbnailResult -> {
                        if (thumbnailResult != null && thumbnailResult.getData() != null) {
                            ImageModel image = new ImageModel(thumbnailResult.getData().getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), thumbnailResult.getData().getSecureContentReference(), true);
                            uploaded.setImage(image);
                        }
                        uploaded.encrypt(key);
                        callback.onComplete(ResultImpl.success(uploaded));
                    });
                    return;
                }
                uploaded.encrypt(key);
                callback.onComplete(ResultImpl.success(uploaded));
            });
        }

        private void doUpload(Authenticator authenticator, String spaceUrl, java.io.File file, boolean thumbnial, CompletionHandler<FileModel> callback) {
//            Metadata metadata = new Metadata(file);
//            metadata.sanitize();
            String mimeType = MimeUtils.getMimeType(file.getAbsolutePath());
            if (mimeType == null) {
                mimeType = "application/unknown";
            }
            TypedSecureContentReferenceOutput typedOutput = new TypedSecureContentReferenceOutput(mimeType, file, progress -> {
                if (local.getProgressHandler() != null && !thumbnial) {
                    Queue.main.run(() -> local.getProgressHandler().onProgress(progress));
                }
            });
            ServiceReqeust.make(spaceUrl).post(Maps.makeMap("uploadProtocol", "content-length")).to("upload_sessions").auth(authenticator).model(UploadSessionModel.class).error(callback)
                    .async((Closure<UploadSessionModel>) sessionModel -> ServiceReqeust.make(sessionModel.getUploadUrl()).put(typedOutput).apply().error(callback)
                            .async((Closure<Void>) uploaded -> ServiceReqeust.make(sessionModel.getFinishUploadUrl()).post(Maps.makeMap("size", file.length())).apply().auth(authenticator).model(FileModel.class).error(callback)
                                    .async((Closure<FileModel>) finish -> {
                                        if (finish == null) {
                                            callback.onComplete(ResultImpl.error("Cannot download the uploaded file"));
                                            return;
                                        }
                                        finish.setUrl(finish.getUrl() + "/versions/" + finish.getVersion() + "/bytes");
                                        SecureContentReference scr = typedOutput.getSecureContentReference();
                                        scr.setLoc(finish.getUrl());
                                        finish.setSecureContentReference(scr);
                                        finish.setFileSize(file.length());
                                        callback.onComplete(ResultImpl.success(finish));
                                    })));
        }
    }

    private void getSpaceUrl(Authenticator authenticator, Device device, String convUrl, CompletionHandler<String> callback) {
        ServiceReqeust.make(convUrl).put().to("space")
                .auth(authenticator)
                .model(new TypeToken<Map<String, Object>>() {
                }.getType())
                .error(callback)
                .async((Closure<Map<String, Object>>) map -> {
                    String spaceUrl = map == null ? null : Objects.toString(map.get("spaceUrl"), null);
                    if (spaceUrl == null) {
                        callback.onComplete(ResultImpl.error("No spaceUrl"));
                        return;
                    }
                    callback.onComplete(ResultImpl.success(spaceUrl));
                });
    }
}
