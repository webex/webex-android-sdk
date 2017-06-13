package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;
import android.util.Base64;

public class Content {
    public static final String CONTENT_TYPE = "STRING";
    public static final String CONTENT_TYPE_FILE = "FILE";
    private String contentId;
    private Uri contentUrl;
    private Uri channelUrl;
    private String type;
    private String device;
    private long createdTime;
    private String creatorId;
    private long modifiedTime;
    private String modifierId;
    private String state;
    private String encryptionKeyUrl;
    private String payload;
    private ChannelImage file;

    public Content(String type, String device, String encryptionKeyUrl, String payload) {
        this(null, null, null, type, device, 0, null, null, encryptionKeyUrl, payload);
    }

    public Content(String contentId, Uri contentUrl, Uri channelUrl, String type, String device, long modifiedTime, String modifierId, String state, String encryptionKeyUrl, String payload) {
        this.contentId = contentId;
        this.contentUrl = contentUrl;
        this.channelUrl = channelUrl;
        this.type = type;
        this.device = device;
        this.modifiedTime = modifiedTime;
        this.modifierId = modifierId;
        this.state = state;
        this.encryptionKeyUrl = encryptionKeyUrl;
        this.payload = payload;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public Uri getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(Uri contentUrl) {
        this.contentUrl = contentUrl;
    }

    public Uri getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(Uri channelUrl) {
        this.channelUrl = channelUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getModifierId() {
        return modifierId;
    }

    public void setModifierId(String modifierId) {
        this.modifierId = modifierId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public void setEncryptionKeyUrl(String encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ChannelImage getBackgroundImage() {
        return file;
    }

    public void setBackgroundImage(ChannelImage backgroundImage) {
        this.file = backgroundImage;
    }

    public static String encodeImageSource(byte[] imageData) {
        return "data:image/png;base64," + Base64.encodeToString(imageData, Base64.NO_WRAP);
    }

    public Content copy() {
        //TODO: maybe we need to use reflection to hard copy the instance later, in case members change in the future

        Content result = new Content(this.type, this.device, this.encryptionKeyUrl, this.payload);
        result.channelUrl = this.channelUrl;
        result.contentId = this.contentId;
        result.contentUrl = this.contentUrl;
        result.createdTime = this.createdTime;
        result.creatorId = this.creatorId;
        result.modifierId = this.modifierId;
        result.modifiedTime = this.modifiedTime;
        return result;
    }
}
