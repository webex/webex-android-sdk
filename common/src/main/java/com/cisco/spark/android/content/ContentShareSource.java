package com.cisco.spark.android.content;

public enum ContentShareSource {
    PHOTO_PICKER("photoPicker"),
    FILE_PICKER("filePicker"),
    CAMERA_PICTURE("cameraPicture"),
    CAMERA_VIDEO("cameraVideo"),
    SCREENSHOT("screenshot"),
    OS_SHARE("osShare"),
    OS_DIRECT_SHARE("osDirectShare"),
    WHITEBOARD("whiteboard");

    private final String source;

    ContentShareSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return source;
    }
}
