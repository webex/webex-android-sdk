package com.ciscowebex.androidsdk.message;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.net.URLConnection;

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

        private java.io.File file;
        private String mimeType;
        private int width;
        private int height;

        @Deprecated
        public Thumbnail() {

        }

        public Thumbnail(@NonNull java.io.File file, @Nullable String mime, int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Width and height must > 0");
            }
            if (!file.exists() && !file.isFile()) {
                throw new IllegalArgumentException("File isn't exist");
            }
            this.file = file;
            this.mimeType = mime != null ? mime : URLConnection.guessContentTypeFromName(file.getName());
            this.width = width;
            this.height = height;
        }

        /**
         * Returns the local thumbnail file to be uploaded.
         * @return The local thumbnail file to be uploaded.
         */
        public java.io.File getFile() {
            return this.file;
        }

        /**
         * Returns the width of the thumbnail.
         * @return The width of the thumbnail.
         */
        public int getWidth() {
            return width;
        }

        /**
         * Returns the height of the thumbnail.
         * @return The height of the thumbnail.
         */
        public int getHeight() {
            return height;
        }

        /**
         * Returns the size in bytes of the thumbnail.
         * @return The size in bytes of the thumbnail.
         */
        public long getSize() {
            return getFile().length();
        }

        /**
         * Returns the MIME type of thumbnail.
         * @return The MIME type of thumbnail.
         */
        public String getMimeType() {
            return mimeType;
        }

        @Deprecated
        public String getPath() {
            return getFile().getAbsolutePath();
        }

        @Deprecated
        public void setPath(String path) {
            this.file = new File(path);
            if (!file.exists() && !file.isFile()) {
                throw new IllegalArgumentException("File isn't exist");
            }
            this.mimeType = URLConnection.guessContentTypeFromName(file.getName());
        }

        @Deprecated
        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Deprecated
        public void setSize(long size) {
        }

        @Deprecated
        public void setWidth(int width) {
            if (width <= 0 ) {
                throw new IllegalArgumentException("Width must > 0");
            }
            this.width = width;
        }

        @Deprecated
        public void setHeight(int height) {
            if (height <= 0 ) {
                throw new IllegalArgumentException("Height must > 0");
            }
            this.height = height;
        }
    }

    private File file;
    private String mimeType;
    private MessageClient.ProgressHandler progressHandler;
    private Thumbnail thumbnail;

    /**
     * Constructs a {@link LocalFile} object out of a local {@link java.io.File}.
     *
     * @param file An existing local file.
     */
    public LocalFile(@NonNull File file) {
        this(file, null, null, null);
    }

    /**
     * Constructs a {@link LocalFile} object out of a local {@link java.io.File}.
     *
     * @param file An existing local file.
     * @param mime The MIME type of the file, according to <a href="https://tools.ietf.org/html/rfc6838">RFC6838</a>.
     * @param thumbnail The thumbnail for the local file. If not null, the thumbnail will be uploaded with the local file.
     * @param progressHandler The progress indicator callback for uploading progresses.
     *
     * @since 2.1.0
     */
    public LocalFile(@NonNull File file,
                     @Nullable String mime,
                     @Nullable Thumbnail thumbnail,
                     @Nullable MessageClient.ProgressHandler progressHandler) {
        if (!file.exists() && !file.isFile()) {
            throw new IllegalArgumentException("File isn't exist");
        }
        this.file = file;
        this.mimeType = mime != null ? mime : URLConnection.guessContentTypeFromName(file.getName());
        this.thumbnail = thumbnail;
        this.progressHandler = progressHandler;
    }

    /**
     * Returns the local {@link java.io.File} object to be uploaded.
     * @return The local {@link java.io.File} object to be uploaded.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the local path to the file to be uploaded..
     * @return The local path to the file.
     */
    public String getPath() {
        return getFile().getPath();
    }

    /**
     * Returns the display name of the uploaded file.
     * @return The display name of the uploaded file.
     */
    public String getName() {
        return getFile().getName();
    }

    /**
     * Returns the size in bytes of the file.
     * @return The size in bytes of the file
     */
    public long getSize() {
        return getFile().length();
    }

    /**
     * Returns the MIME type of this file.
     * @return The MIME type of this file. Null if no MIME type is unavailable.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the {@link MessageClient.ProgressHandler} used when uploading this file.
     * @return The {@link MessageClient.ProgressHandler}. Null if no {@link MessageClient.ProgressHandler} is set.
     */
    public MessageClient.ProgressHandler getProgressHandler() {
        return progressHandler;
    }

    /**
     * Return the thumbnail for the local file.
     * @return The thumbnail for the local file.
     */
    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    @Deprecated
    public void setFile(File file) {
        if (!file.exists() && !file.isFile()) {
            throw new IllegalArgumentException("File isn't exist");
        }
        this.file = file;
        this.mimeType = URLConnection.guessContentTypeFromName(file.getName());
    }

    @Deprecated
    public void setPath(String path) {
        setFile(new File(path));
    }

    @Deprecated
    public void setName(String name) {
    }

    @Deprecated
    public void setSize(long size) {
    }

    @Deprecated
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Deprecated
    public void setProgressHandler(MessageClient.ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }

    @Deprecated
    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
