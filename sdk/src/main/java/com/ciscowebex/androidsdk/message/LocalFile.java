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
        private final java.io.File file;
        private final String mimeType;
        private final int width;
        private final int height;

        private Thumbnail(Builder builder) {
            this.file = builder.file;
            this.mimeType = builder.mimeType;
            this.width = builder.width;
            this.height = builder.height;
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

        public static class Builder {
            private java.io.File file;
            private String mimeType;
            private int width;
            private int height;

            public Builder(@NonNull java.io.File file) {
                this.file = file;
            }

            public Builder setPath(String path) {
                this.file = new File(path);
                return this;
            }

            public Builder setMimeType(String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            public Builder setWidth(int width) {
                this.width = width;
                return this;
            }

            public Builder setHeight(int height) {
                this.height = height;
                return this;
            }

            public Thumbnail build() {
                if (this.file == null) {
                    throw new NullPointerException();
                }

                this.mimeType = this.mimeType != null ? this.mimeType : URLConnection.guessContentTypeFromName(file.getName());
                Thumbnail thumbnail = new Thumbnail(this);

                if (!thumbnail.getFile().exists() && !thumbnail.getFile().isFile()) {
                    throw new IllegalArgumentException("File isn't exist");
                }

                return thumbnail;
            }
        }
    }

    private File file;
    private String mimeType;
    private MessageClient.ProgressHandler progressHandler;
    private Thumbnail thumbnail;

    /**
     * Constructs a {@link LocalFile} object out of a local {@link File file}.
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
