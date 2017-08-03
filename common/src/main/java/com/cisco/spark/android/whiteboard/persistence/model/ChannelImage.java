package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;

public class ChannelImage implements ContentReference {

    public static final String MIME_PNG = "image/png";

    private Long updatedTime;
    private Uri encryptionKeyUrl;
    private Long fileSize;
    private Uri url;
    private String scr;
    private Integer height;
    private Integer width;
    private String mimeType;
    private ChannelImageThumbnail thumbnail;
    private SecureContentReference secureContentReference;

    public ChannelImage(Uri imageUri, int width, int height) {
        this.url = imageUri;
        this.width = width;
        this.height = height;
        this.mimeType = MIME_PNG;
    }

    public ChannelImage() { }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public Uri getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public void setEncryptionKeyUrl(Uri encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }

    @Override
    public String getScr() {
        return scr;
    }

    @Override
    public void setScr(String scr) {
        this.scr = scr;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public ChannelImageThumbnail getThumbnail() {
        return thumbnail;
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }


    @Override
    public boolean isSecureContentReference() {
        return secureContentReference != null;
    }

    @Override
    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    @Override
    public void setSecureContentReference(SecureContentReference secureContentReference) {
        this.secureContentReference = secureContentReference;
    }

    @Override
    public Uri getUrlOrSecureLocation() {
        if (isSecureContentReference()) {
            return UriUtils.parseIfNotNull(secureContentReference.getLoc());
        } else {
            return getUrl();
        }
    }

    public void encrypt(KeyObject keyObject) throws IOException {
        if (getEncryptionKeyUrl() == null) {
            return;
        }
        try {
            if (secureContentReference != null) {
                secureContentReference.setLoc(getUrl().toString());
                setScr(secureContentReference.toJWE(keyObject.getKeyBytes()));
                setSecureContentReference(null);
            }

            if (thumbnail != null) {
                thumbnail.encrypt(keyObject);
            }
        } catch (JOSEException e) {
            throw new IOException("Unable to encrypt", e);
        }
    }

    public void decrypt(KeyObject keyObject) throws IOException, ParseException, NullPointerException {
        if (keyObject == null) {
            return;
        }
        try {
            if (getScr() != null) {
                setSecureContentReference(SecureContentReference.fromJWE(keyObject.getKeyBytes(), getScr()));
            }
            if (thumbnail != null) {
                thumbnail.decrypt(keyObject);
            }
        } catch (ParseException e) {
            throw new IOException("Unable to decrypt", e);
        } catch (JOSEException e) {
            String exceptionMessage = "Unable to decrypt ";
            if (keyObject.getKeyValue() != null && !UriUtils.equals(keyObject.getKeyUrl(), keyObject.getKeyId())) {
                exceptionMessage += "Mismatched keys";
            }
            throw new IOException(exceptionMessage, e);
        }
    }


}
