package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.model.conversation.File;
import com.ciscowebex.androidsdk.message.RemoteFile;

public class RemoteFileImpl extends RemoteFile {

    private File file;

    public RemoteFileImpl(File file) {
        this.file = file;
        this.size = file.getFileSize();
        this.displayName = file.getDisplayName();
        this.mimeType = file.getMimeType();
        this.url = file.getUrl().toString();
        if (file.getImage() != null) {
            this.thumbnail = new Thumbnail();
            this.thumbnail.setUrl(file.getImage().getUrl().toString());
            this.thumbnail.setWidth(file.getImage().getWidth());
            this.thumbnail.setHeight(file.getImage().getHeight());
        }
    }

    public File getFile() {
        return file;
    }

    public String toString() {
        return file.toString();
    }
}
