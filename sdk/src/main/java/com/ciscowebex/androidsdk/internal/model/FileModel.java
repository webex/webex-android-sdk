/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import com.ciscowebex.androidsdk.internal.crypto.SecureContentReference;
import com.ciscowebex.androidsdk.utils.GiphyUtils;
import com.github.benoitdion.ln.Ln;
import com.nimbusds.jose.JOSEException;
import me.helloworld.utils.Checker;

import java.util.Date;

public class FileModel extends ObjectModel {

    public enum DownloadProgressState {
        PROGRESS_DEFAULT,
        UPLOAD_IN_PROGRESS,
        UPLOAD_COMPLETE,
        UPLOAD_FAILED
    }

    private String contentId;
    private long fileSize;
    private String version;
    private PersonModel author;
    private String mimeType;
    private Date updated;
    private ImageModel image;
    private String scr;
    private String scrLocReference;
    private SecureContentReference secureContentReference;
    private boolean hidden;
    private DownloadProgressState progressState;
    private boolean isGiphyFromSearchCategory;

    public FileModel() {
        super(ObjectModel.Type.file);
    }

    public FileModel(String uri) {
        this();
        setUrl(uri);
    }

    public FileModel(String contentId, String version, String displayName, SecureContentReference scr,
                     String uri, String loc, long fileSize, String mimeType, ImageModel image) {
        this();
        super.setDisplayName(displayName);
        setSecureContentReference(scr);
        if (uri != null) {
            setUrl(uri);
        }
        setScrLocReference(loc);
        this.contentId = contentId;
        this.fileSize = fileSize;
        this.version = version;
        this.progressState = DownloadProgressState.PROGRESS_DEFAULT;
        this.mimeType = mimeType;
        this.image = image;
    }

    @Override
    public String getId() {
        return contentId;
    }

    public ObjectModel setId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public PersonModel getAuthor() {
        return author;
    }

    public void setAuthor(PersonModel author) {
        this.author = author;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public ImageModel getImage() {
        return image;
    }

    public void setImage(ImageModel image) {
        this.image = image;
    }

    public boolean isGiphyFromSearchCategory() {
        return isGiphyFromSearchCategory;
    }

    public void setGiphyFromSearchCategory(boolean giphyFromSearchCategory) {
        isGiphyFromSearchCategory = giphyFromSearchCategory;
    }

    public void setProgressState(DownloadProgressState progressState) {
        this.progressState = progressState;
    }

    public DownloadProgressState getProgressState() {
        return progressState;
    }

    public String getScrLocReference() {
        return scrLocReference;
    }

    public void setScrLocReference(String scrLocReference) {
        this.scrLocReference = scrLocReference;
    }

    public String getScr() {
        return scr;
    }

    public void setScr(String scr) {
        this.scr = scr;
    }

    public String getUrlOrSecureLocation() {
        if (isSecureContentReference()) {
            return secureContentReference.getLoc();
        } else {
            return getUrl();
        }
    }

    public void setSecureContentReference(SecureContentReference secureContentReference) {
        this.secureContentReference = secureContentReference;
        if (secureContentReference != null) {
            setUrl(secureContentReference.getLoc());
        }
    }

    public boolean isSecureContentReference() {
        return secureContentReference != null;
    }

    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public String toString() {
        return "File{" +
                "fileSize=" + fileSize +
                ", version='" + version + '\'' +
                ", author=" + author +
                ", mimeType='" + mimeType + '\'' +
                ", updated=" + updated + '\'' +
                ", contentId=" + contentId + '\'' +
                ", image=" + ((image != null) ? image.toString() : "") + '\'' +
                ", isHidden=" + hidden +
                '}';
    }

    @Override
    public void encrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.encrypt(key);
        if (!Checker.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
        }
        if (secureContentReference != null) {
            try {
                secureContentReference.setIsGiphy(isFileGiphy());
                if (getScrLocReference() != null) {
                    secureContentReference.setLoc(getScrLocReference());
                    setScrLocReference(null);
                } else {
                    secureContentReference.setLoc(getUrl());
                }
                setScr(secureContentReference.toJWE(key.getKeyBytes()));
                setSecureContentReference(null);
            } catch (JOSEException e) {
                Ln.d(e);
            }
        }
        if (getImage() != null) {
            getImage().encrypt(key);
        }
    }

    @Override
    public void decrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.decrypt(key);
        if (!Checker.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }
        if (getScr() != null) {
            try {
                if (isFileGiphy()) {
                    setSecureContentReference(SecureContentReference.fromJWEForGiphy(key.getKeyBytes(), getScr()));
                } else {
                    setSecureContentReference(SecureContentReference.fromJWE(key.getKeyBytes(), getScr()));
                }
            } catch (Throwable e) {
                Ln.d(e);
            }
        }
        if (getImage() != null) {
            getImage().decrypt(key);
        }
    }

    private boolean isFileGiphy() {
        if (ObjectModel.Type.giphy.equalsIgnoreCase(getObjectType())) {
            return true;
        } else {
            // This check is does since the backend systems cannot support an objectType of 'Giphy" presently.
            // Once it is supported this return can be refactored.
            return GiphyUtils.isMimeTypeGiphy(mimeType) && GiphyUtils.isImageGiphy(image);
        }
    }
}
