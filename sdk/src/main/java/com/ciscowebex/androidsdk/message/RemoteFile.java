package com.ciscowebex.androidsdk.message;

/**
 * Data struct for a remote file.
 * @since 1.4.0
 */
public class RemoteFile {

    /**
     * A data type represents a thumbnail file.
     * @since 1.4.0
     */
    public class Thumbnail {
        public int width;
        public int height;
        public String mimeType;
        public String url;
    }

    public String displayName;
    public String mimeType;
    public Long size;
    public String url;
    public Thumbnail thumbnail;

}
