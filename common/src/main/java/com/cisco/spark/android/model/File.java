package com.cisco.spark.android.model;

import android.net.Uri;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class File extends ActivityObject implements ContentReference {
    private String contentId;
    private long fileSize;
    private String version;
    private Person author;
    private String mimeType;
    private Date updated;
    private Image image;
    private String scr;
    private ItemCollection<TranscodedContent> transcodedCollection;
    private SecureContentReference secureContentReference;
    private List<ContentAction> actions;
    private boolean hidden;

    public File() {
        super(ObjectType.file);
    }

    private File(String contentId) {
        this();
        this.contentId = contentId;
    }

    public File(String contentId, String version, String displayName, Uri uri, long fileSize, List<ContentAction> actions) {
        this();
        super.setDisplayName(displayName);
        setUri(uri);
        this.contentId = contentId;
        this.fileSize = fileSize;
        this.version = version;
        this.actions = actions;
    }

    @Override
    public String getId() {
        return contentId;
    }

    public ActivityObject setId(String contentId) {
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

    public Person getAuthor() {
        return author;
    }

    public void setAuthor(Person author) {
        this.author = author;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public List<ContentAction> getActions() {
        return actions;
    }

    public void setActions(List<ContentAction> actions) {
        this.actions = actions;
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
            setUri(UriUtils.parseIfNotNull(secureContentReference.getLoc()));
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

    public ItemCollection<TranscodedContent> getTranscodedCollection() {
        if (transcodedCollection == null)
            transcodedCollection = new ItemCollection<>();
        return transcodedCollection;
    }

    public void setTranscodedCollection(ItemCollection<TranscodedContent> transcodedCollection) {
        this.transcodedCollection = transcodedCollection;
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
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
        }

        if (secureContentReference != null) {
            try {
                secureContentReference.setLoc(getUrl().toString());
                setScr(secureContentReference.toJWE(key.getKeyBytes()));
                setSecureContentReference(null);
            } catch (JOSEException e) {
                throw new IOException("Unable to encrypt", e);
            }
        }

        if (getImage() != null) {
            getImage().encrypt(key);
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }

        if (getScr() != null) {
            try {
                setSecureContentReference(SecureContentReference.fromJWE(key.getKeyBytes(), getScr()));
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

        if (getImage() != null) {
            getImage().decrypt(key);
        }

        if (getTranscodedCollection() != null) {
            for (TranscodedContent transcodedContent : getTranscodedCollection().getItems()) {
                transcodedContent.decrypt(key);
            }
        }
    }
}
