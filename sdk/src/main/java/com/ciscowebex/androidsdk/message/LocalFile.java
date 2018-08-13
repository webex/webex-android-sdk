package com.ciscowebex.androidsdk.message;

import java.io.File;

/**
 * A data type represents a local file.
 *
 * @since 1.4.0
 */
public class LocalFile {

    /**
     * A data type represents a local file thumbnail.
     *
     * @since 1.4.0
     */
    public static class Thumbnail {
        private String path;
        private int width;
        private int height;
        private long size;
        private String mimeType;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    private File file;
    private String path;
    private String name;
    private long size;
    private String mimeType;
    private MessageClient.ProgressHandler progressHandler;
    private Thumbnail thumbnail;

    public LocalFile(File file) {
        this.file = file;
        if (this.file.exists() && this.file.isFile()) {
            this.name = file.getName();
            this.size = file.length();
            this.path = file.getPath();
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public MessageClient.ProgressHandler getProgressHandler() {
        return progressHandler;
    }

    public void setProgressHandler(MessageClient.ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
