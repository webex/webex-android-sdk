package com.ciscowebex.androidsdk.message;

import java.io.File;

/**
 * A data type represents a local file.
 *
 * @since 1.4.0
 */
public class LocalFile {

    /**
     * A data type represents a thumbnail of this local file.
     *
     * The thumbnail typically is an image file to provide preview of the local file without opening.
     *
     * @since 1.4.0
     */
    public static class Thumbnail {
        private String path;
        private int width;
        private int height;
        private long size;
        private String mimeType;

        /**
         * Returns the local path of the thumbnail file to be uploaded.
         * @return The local path of the thumbnail file to be uploaded.
         */
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        /**
         * Returns the width of the thumbnail.
         * @return The width of the thumbnail.
         */
        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        /**
         * Returns the height of the thumbnail.
         * @return The height of the thumbnail.
         */
        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        /**
         * Returns the size in bytes of the thumbnail.
         * @return The size in bytes of the thumbnail.
         */
        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        /**
         * Returns the MIME type of thumbnail.
         * @return The MIME type of thumbnail.
         */
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
     * Constructs a {@link LocalFile} object out of a local {@link java.io.File}.
     *
     * @param file An existing local file.
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
     * Constructs a {@link LocalFile}  object based on a local {@Link java.io.File} and other parameters.
     * @param file An existing local file.
     * @param mime The MIME type of the local file.
     * @param thumbnail The {@link Thumbnail} of the local file.
     * @param handler The {@link ProgressHandler} for uploading.
     */
    public LocalFile(File file, String mime, Thumbnail thumbnail, ProgressHandler handler) {
        this(file);
        this.mimeType = mime;
        this.thumbnail = thumbnail;
        progressHandler = handler;
    }

    /**
     * Returns the local {@link java.io.File} object to be uploaded.
     * @return The local {@link java.io.File} object to be uploaded.
     */
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Returns the local path to the file to be uploaded..
     * @return The local path to the file.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the display name of the uploaded file.
     * @return The display name of the uploaded file.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the size in bytes of the file.
     * @return The size in bytes of the file
     */
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Returns the MIME type of this file.
     * @return The MIME type of this file. Null if no MIME type is set.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME type of this file in {@link RFC6838 https://tools.ietf.org/html/rfc6838} defined format.
     * @param mimeType the MIME type string.
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the {@link ProgressHandler} used when uploading this file.
     * @return The {@link ProgressHandler}. Null if no {@link ProgressHandler} is set.
     */
    public MessageClient.ProgressHandler getProgressHandler() {
        return progressHandler;
    }

    /**
     * Sets the {@link ProgressHandler} to get uploading progress of this file.
     * @param progressHandler the handler to get uploading progress.
     */
    public void setProgressHandler(MessageClient.ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }

    /**
     * Returns the thumbnail of the file.
     * @return The thumbnail of the file.
     */
    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    /**
     * Sets the thumbnail of the file.
     * @param thumbnail The thumbnail of the file.
     */
    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
