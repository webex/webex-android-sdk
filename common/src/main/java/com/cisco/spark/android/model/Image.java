package com.cisco.spark.android.model;

import android.net.Uri;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;

public class Image implements ContentReference {
    private Uri url;
    private int width;
    private int height;
    private String scr;
    private SecureContentReference secureContentReference;
    private boolean thumbanail;

    public Image() {
    }

    public Image(Uri url, int width, int height, boolean thumbanail) {
        this.url = url;
        this.width = width;
        this.height = height;
        this.thumbanail = thumbanail;
    }

    public Image(File file, int width, int height, boolean thumbanail) {
        this(file.getUrl(), width, height, thumbanail);
        setScr(file.getScr());
        setSecureContentReference(file.getSecureContentReference());
    }

    public Uri getUrl() {
        return url;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String toString() {
        return "Image{" +
                "url=" + url +
                ", width=" + Integer.toString(width) +
                ", height=" + Integer.toString(height) +
                '}';
    }

    public void setUrl(Uri url) {
        this.url = url;
    }

    public boolean isThumbanail() {
        return thumbanail;
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
    public Uri getUrlOrSecureLocation() {
        if (isSecureContentReference()) {
            return UriUtils.parseIfNotNull(secureContentReference.getLoc());
        } else {
            return getUrl();
        }
    }

    @Override
    public void setSecureContentReference(SecureContentReference secureContentReference) {
        this.secureContentReference = secureContentReference;
        if (secureContentReference != null) {
            setUrl(UriUtils.parseIfNotNull(secureContentReference.getLoc()));
        }
    }

    @Override
    public boolean isSecureContentReference() {
        return secureContentReference != null;
    }

    @Override
    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    public void encrypt(KeyObject key) throws IOException {
        if (secureContentReference != null) {
            try {
                setScr(secureContentReference.toJWE(key.getKeyBytes()));
                setSecureContentReference(null);
            } catch (JOSEException e) {
                throw new IOException("Unable to encrypt", e);
            }
        }
    }

    public void decrypt(KeyObject key) throws IOException {
        String scr = getScr();
        if (Strings.isEmpty(scr) && getUrl() != null && "secureContentReference".equals(getUrl().getScheme())) {
            scr = getUrl().getSchemeSpecificPart();
        }

        if (Strings.notEmpty(scr)) {
            try {
                setSecureContentReference(SecureContentReference.fromJWE(key.getKeyBytes(), scr));
            } catch (ParseException e) {
                throw new IOException("Unable to decrypt", e);
            } catch (JOSEException e) {
                String exceptionMessage = "Unable to decrypt ";
                if (key.getKeyValue() != null && !UriUtils.equals(key.getKeyUrl(), key.getKeyId())) {
                    exceptionMessage += "Mismatched keys";
                }
                throw new IOException(exceptionMessage, e);
            }
        }
    }

    public void copyFromFile(File file) {
        this.scr = file.getScr();
        this.url = file.getUrl();
        this.secureContentReference = file.getSecureContentReference();
    }
}
