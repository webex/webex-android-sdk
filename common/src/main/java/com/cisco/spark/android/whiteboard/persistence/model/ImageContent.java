package com.cisco.spark.android.whiteboard.persistence.model;

public class ImageContent {

    private String type;
    private int zIndex;
    private String sourceType;
    private String imageSource;

    public ImageContent(byte[] imageData) {
        this.type = "image";
        this.zIndex = 1;
        this.sourceType = "dataURL";
        this.imageSource = Content.encodeImageSource(imageData);
    }
}
