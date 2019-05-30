package com.ciscowebex.androidsdk.message;

/**
 * A data type represents a remote file on the Cisco Webex cloud.
 * @since 1.4.0
 */
public class RemoteFile {

    /**
     * A data type represents a thumbnail file.
     * @since 1.4.0
     */
    public class Thumbnail {
        private int width;
        private int height;
        private String mimeType;
        private String url;

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
     * Return the display name of this remote file.
     * @return The display name of this remote file.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the display name of this remote file.
     * @param displayName the display name of this remote file.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Return the MIME type of this remote file.
     * @return The MIME type of this remote file.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Set the MIME type string of this remote file, according to {@link RFC6838 https://tools.ietf.org/html/rfc6838}.
     * @param mimeType the MIME type string
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Return the size of this remote file.
     * @return the size of this remote file, in bytes.
     */
    public Long getSize() {
        return size;
    }

    /**
     * Set the size of this remote file, in bytes.
     * @param size the size of the file
     */
    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * Return the URL string for this remote file.
     * @return The URL string for this remote file.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL string of this remote file.
     * @param url the URL string of this remote file.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Return the thumbnail of this remote file.
     * @return the thumbnail of this remote file.
     */
    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    /**
     * Set the thumbnail of this remote file.
     * @param thumbnail The thumbnail of this remote file.
     */
    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }
}
