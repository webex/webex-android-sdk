package com.ciscowebex.androidsdk.message.internal;

import android.net.Uri;
import android.support.annotation.NonNull;
import com.cisco.spark.android.model.conversation.File;
import com.cisco.spark.android.model.conversation.Image;
import com.ciscowebex.androidsdk.message.RemoteFile;

public class RemoteFileImpl implements RemoteFile {

    public class ThumbnailImpl implements Thumbnail{

        private Image image;
        private String mimeType;

        public ThumbnailImpl(@NonNull Image image) {
            this.image = image;
        }

        Image getImage() {
            return this.image;
        }

        public int getWidth() {
            return image.getWidth();
        }

        public int getHeight() {
            return image.getHeight();
        }

        public String getMimeType() {
            return mimeType;
        }

        @Deprecated
        public String getUrl() {
            return image.getUrl().toString();
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

    private File file;
    private Thumbnail thumbnail;

    public RemoteFileImpl(@NonNull File file) {
        this.file = file;
        if (file.getImage() != null) {
            this.thumbnail = new ThumbnailImpl(file.getImage());
        }
    }

    File getFile() {
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
        return file.getUrl().toString();
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
        file.setUrl(Uri.parse(url));
    }

    @Override
    @Deprecated
    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
