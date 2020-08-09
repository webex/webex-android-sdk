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

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.message.RemoteFile;

import java.util.ArrayList;
import java.util.List;

public class RemoteFileImpl implements RemoteFile {

    public static List<RemoteFile> mapRemoteFiles(ActivityModel activity) {
        ArrayList<RemoteFile> remoteFiles = new ArrayList<>();
        if (activity.getObject() != null && activity.getObject().isContent()) {
            ContentModel content = (ContentModel) activity.getObject();
            ItemsModel<FileModel> files = content.getContentFiles();
            for (FileModel file : files.getItems()) {
                RemoteFile remoteFile = new RemoteFileImpl(file);
                remoteFiles.add(remoteFile);
            }
        }
        return remoteFiles;
    }

    public static class ThumbnailImpl implements Thumbnail{

        private ImageModel image;
        private String mimeType;

        public ThumbnailImpl(@NonNull ImageModel image) {
            this.image = image;
        }

        ImageModel getImage() {
            return this.image;
        }

        public int getWidth() {
            return image == null ? 0 : image.getWidth();
        }

        public int getHeight() {
            return image == null ? 0 : image.getHeight();
        }

        @Deprecated
        public String getMimeType() {
            return mimeType;
        }

        @Deprecated
        public String getUrl() {
            return image.getUrl();
        }

        @Deprecated
        public void setUrl(String url) {
        }

        @Deprecated
        public void setWidth(int width) {
        }

        @Deprecated
        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Deprecated
        public void setHeight(int height) {
        }
    }

    private FileModel file;
    private Thumbnail thumbnail;

    public RemoteFileImpl(@NonNull FileModel file) {
        this.file = file;
        if (file.getImage() != null) {
            this.thumbnail = new ThumbnailImpl(file.getImage());
        }
    }

    FileModel getFile() {
        return file;
    }

    @Override
    public String getDisplayName() {
        return file.getDisplayName();
    }

    @Override
    public long getSize() {
        return file.getFileSize();
    }

    @Override
    public String getMimeType() {
        return file.getMimeType();
    }

    @Override
    public String getUrl() {
        return file.getUrl();
    }

    @Override
    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    @Deprecated
    public void setDisplayName(String displayName) {
        file.setDisplayName(displayName);
    }

    @Override
    @Deprecated
    public void setSize(Long size) {
        file.setFileSize(size);
    }

    @Override
    @Deprecated
    public void setMimeType(String mimeType) {
        file.setMimeType(mimeType);
    }

    @Override
    @Deprecated
    public void setUrl(String url) {
        file.setUrl(url);
    }

    @Override
    @Deprecated
    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
