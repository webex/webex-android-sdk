package com.ciscowebex.androidsdk.message;

public class MessagesAttachments {

    private String filename;
    private String contentType;
    private double size;

    public MessagesAttachments(){}

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public double getSize() {
        return size;
    }


}
