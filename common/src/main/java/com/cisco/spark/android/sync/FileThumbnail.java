package com.cisco.spark.android.sync;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.model.Image;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;

public class FileThumbnail implements Parcelable, ContentReference {
    private Uri url;
    private int height;
    private int width;
    private String scr;
    private SecureContentReference secureContentReference;

    public static FileThumbnail from(Image image) {
        if (image == null) {
            return null;
        }

        FileThumbnail thumbnail = new FileThumbnail(image.getUrl(), image.getHeight(), image.getWidth());
        thumbnail.setScr(image.getScr());
        thumbnail.setSecureContentReference(image.getSecureContentReference());
        return thumbnail;
    }

    public FileThumbnail() {
        this.url = null;
        this.height = -1;
        this.width = -1;
    }

    public FileThumbnail(Uri url, int height, int width) {
        this.url = url;
        this.height = height;
        this.width = width;
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
    }

    @Override
    public boolean isSecureContentReference() {
        return secureContentReference != null;
    }

    @Override
    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    //
    // Implement Parcelable interface
    //
    public FileThumbnail(Parcel in) {
        url = in.readParcelable(Uri.class.getClassLoader());
        height = in.readInt();
        width = in.readInt();
    }

    public static final Parcelable.Creator<FileThumbnail> CREATOR
            = new Parcelable.Creator<FileThumbnail>() {
        public FileThumbnail createFromParcel(Parcel in) {
            return new FileThumbnail(in);
        }

        public FileThumbnail[] newArray(int size) {
            return new FileThumbnail[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(url, flags);
        dest.writeInt(height);
        dest.writeInt(width);
    }

    public void decrypt(KeyObject key) throws IOException {
        if (Strings.notEmpty(getScr())) {
            try {
                setSecureContentReference(SecureContentReference.fromJWE(key.getKeyBytes(), getScr()));
            } catch (ParseException e) {
                throw new IOException(e);
            } catch (JOSEException e) {
                String exceptionMessage = "Unable to decrypt ";
                if (key.getKeyValue() != null && !UriUtils.equals(key.getKeyUrl(), key.getKeyId())) {
                    exceptionMessage += "Mismatched keys";
                }
                throw new IOException(exceptionMessage, e);
            }
        }
    }

    public void encrypt(KeyObject key) throws IOException {
        if (getSecureContentReference() != null) {
            try {
                setScr(getSecureContentReference().toJWE(key.getKeyBytes()));
            } catch (JOSEException e) {
                throw new IOException(e);
            }
        }
    }
}
