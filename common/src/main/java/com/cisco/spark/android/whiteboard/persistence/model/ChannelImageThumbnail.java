package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;

public class ChannelImageThumbnail implements ContentReference {
    private Uri url;
    private String scr;
    private Integer width;
    private Integer height;
    private String mimeType;
    private SecureContentReference secureContentReference;

    public Uri getUrl() {
        return url;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getScr() {
        return scr;
    }

    @Override
    public void setScr(String scr) {
        this.scr = scr;
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
        if (secureContentReference != null) {
            try {
                secureContentReference.setLoc(getUrl().toString());
                setScr(secureContentReference.toJWE(keyObject.getKeyBytes()));
                setSecureContentReference(null);
            } catch (JOSEException e) {
                throw new IOException("Unable to encrypt", e);
            }
        }
    }

    public void decrypt(KeyObject keyObject) throws IOException, ParseException, NullPointerException {
        if (getScr() != null) {
            try {
                setSecureContentReference(SecureContentReference.fromJWE(keyObject.getKeyBytes(), getScr()));
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




}
