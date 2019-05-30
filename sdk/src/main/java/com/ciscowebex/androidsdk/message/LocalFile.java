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

    /**
     * Constructor to create a LocalFile based on a {@link java.io.File} pointing to a file on the Android device.
     *
     * @param file It must be an existing file (not a directory) on the device.
     */
    public LocalFile(File file) {
        this.file = file;
        if (this.file.exists() && this.file.isFile()) {
            this.name = file.getName();
            this.size = file.length();
            this.path = file.getPath();
        }
    }

    /**
     * @return the {@link java.io.File} object passed from {@link LocalFile}.
     */
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * @return the path to the file on the device.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the name of the file on the device.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the size of the file on the deivce
     */
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return the MIME type of this file. Null if no MIME type is set.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Set the MIME type of this file, according to {@link RFC6838 https://tools.ietf.org/html/rfc6838}.
     * @param mimeType the MIME type string
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the {@link ProgressHandler} for uploading this file. Null if no {@link ProgressHandler} is set.
     */
    public MessageClient.ProgressHandler getProgressHandler() {
        return progressHandler;
    }

    /**
     * Set the {@link ProgressHandler} to get uploading progress of this file.
     * @param progressHandler the handler to get uploading progress.
     */
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
