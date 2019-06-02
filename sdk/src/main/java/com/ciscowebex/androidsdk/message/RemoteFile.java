package com.ciscowebex.androidsdk.message;

/**
 * A data type represents a remote file on the Cisco Webex.
 * The content of the remote file can be downloaded via {@link MessageClient#downloadFile}.
 * @since 1.4.0
 */
public class RemoteFile {

    /**
     * A data type represents a thumbnail file.
     * The thumbnail typically is an image file which provides preview of the remote file without downloading.
     * The content of the thumbnail can be downloaded via {@link MessageClient#downloadThumbnail}.
     * @since 1.4.0
     */
    public class Thumbnail {
        private int width;
        private int height;
        private String mimeType;
        private String url;

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
         * Returns the height of the thumbnail.
         * @return The height of the thumbnail.
         */
        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    protected String displayName;
    protected String mimeType;
    protected Long size;
    protected String url;
    protected Thumbnail thumbnail;

    /**
     * Returns the display name of this remote file.
     * @return The display name of this remote file.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the MIME type of this remote file.
     * @return The MIME type of this remote file.
     */
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the size in bytes of this remote file.
     * @return the size in bytes of this remote file.
     */
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the thumbnail of this remote file.
     * @return The thumbnail of this remote file.
     */
    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
