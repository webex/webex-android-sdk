package com.ciscowebex.androidsdk.message;

/**
 * A data type represents a remote file on the Cisco Webex cloud.
 * @since 1.4.0
 */
public interface RemoteFile {

    /**
     * A data type represents a thumbnail file.
     * @since 1.4.0
     */
    interface Thumbnail {

        int getWidth();

        int getHeight();

        String getMimeType();

        String getUrl();

        @Deprecated
        void setUrl(String url);

        @Deprecated
        void setWidth(int width);

        @Deprecated
        void setMimeType(String mimeType);

        @Deprecated
        void setHeight(int height);
    }

    /**
     * Return the display name of this remote file.
     * @return The display name of this remote file.
     */
    String getDisplayName();

    /**
     * Return the size of this remote file.
     * @return the size of this remote file, in bytes.
     */
    Long getSize();

    /**
     * Return the MIME type of this remote file.
     * @return The MIME type of this remote file.
     */
    String getMimeType();

    /**
     * Return the URL string for this remote file.
     * @return The URL string for this remote file.
     */
    String getUrl();

    /**
     * Return the thumbnail of this remote file.
     * @return the thumbnail of this remote file.
     */
    Thumbnail getThumbnail();

    /**
     * Set the display name of this remote file.
     * @param displayName the display name of this remote file.
     * @Deprecated
     */
    @Deprecated
    void setDisplayName(String displayName);

    /**
     * Set the size of this remote file, in bytes.
     * @param size the size of the file
     * @Deprecated
     */
    @Deprecated
    void setSize(Long size);

    /**
     * Set the MIME type string of this remote file, according to {@link RFC6838 https://tools.ietf.org/html/rfc6838}.
     * @param mimeType the MIME type string
     */
    @Deprecated
    void setMimeType(String mimeType);

    /**
     * Set the URL string of this remote file.
     * @param url the URL string of this remote file.
     * @Deprecated
     */
    @Deprecated
    void setUrl(String url);

    /**
     * Set the thumbnail of this remote file.
     * @param thumbnail The thumbnail of this remote file.
     * @Deprecated
     */
    @Deprecated
    void setThumbnail(Thumbnail thumbnail);
}
