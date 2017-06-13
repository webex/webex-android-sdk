package com.cisco.spark.android.sync;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.model.ContentAction;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class DisplayableFile implements ContentReference, Parcelable {
    private String name;
    private Uri url;
    private FileThumbnail thumbnail;
    private String conversationId;
    private long filesize;
    private String mimeType;
    private List<Page> transcodedPages;
    private String scr;
    private SecureContentReference secureContentReference;
    private List<ContentAction> actions;

    public static DisplayableFile from(final File file, final String conversationId) {
        List<DisplayableFile.Page> pages = null;
        if (file.getTranscodedCollection().getItems().size() > 0)
            pages = file.getTranscodedCollection().getItems().get(0).getPages();

        DisplayableFile displayableFile = new DisplayableFile(file.getDisplayName(), file.getUrl(), conversationId, file.getFileSize(), file.getMimeType(), file.getScr(), FileThumbnail.from(file.getImage()), pages, file.getActions());
        displayableFile.setSecureContentReference(file.getSecureContentReference());
        displayableFile.setScr(file.getScr());
        return displayableFile;
    }

    public boolean hasThumbnail() {
        if (getThumbnail() == null)
            return false;

        Uri uri = getThumbnail().getUrl();

        if (uri == null)
            return false;

        if ("file".equals(uri.getScheme())) {
            try {
                return new java.io.File(new URI(uri.toString())).exists();
            } catch (URISyntaxException e) {
                return false;
            }
        }

        return true;
    }

    public static class Page implements Parcelable, ContentReference {
        private Uri url;
        private long fileSize;
        private String mimeType;
        private String displayName;
        private String scr;
        private SecureContentReference secureContentReference;

        public static Page from(File file) {
            Page page = new Page();
            page.url = file.getUrl();
            page.fileSize = file.getFileSize();
            page.mimeType = file.getMimeType();
            page.displayName = file.getDisplayName();
            page.scr = file.getScr();
            page.secureContentReference = file.getSecureContentReference();

            return page;
        }

        private Page() {
        }

        public Uri getUrl() {
            return url;
        }

        public long getPageFileSize() {
            return fileSize;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(url, flags);
            dest.writeString(displayName);
            dest.writeLong(fileSize);
            dest.writeString(mimeType);
        }

        //
        // Implement Parcelable interface
        //
        public Page(Parcel in) {
            url = in.readParcelable(Uri.class.getClassLoader());
            displayName = in.readString();
            fileSize = in.readLong();
            mimeType = in.readString();
        }

        public static final Parcelable.Creator<Page> CREATOR
                = new Parcelable.Creator<Page>() {
            public Page createFromParcel(Parcel in) {
                return new Page(in);
            }

            public Page[] newArray(int size) {
                return new Page[size];
            }
        };

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
    }

    public DisplayableFile(String name, Uri url, String conversationId, long fileSize, String mimeType, String scr, FileThumbnail thumbnail, List<Page> pages, List<ContentAction> actions) {
        this.name = name;
        this.url = url;
        this.conversationId = conversationId;
        this.filesize = fileSize;
        this.mimeType = mimeType;
        this.scr = scr;
        this.thumbnail = thumbnail;
        this.transcodedPages = pages;
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }

    public FileThumbnail getThumbnail() {
        return thumbnail;
    }

    public String getConversationId() {
        return conversationId;
    }

    public long getFilesize() {
        return filesize;
    }

    public List<ContentAction> getActions() {
        return actions;
    }

    public void setActions(List<ContentAction> actions) {
        this.actions = actions;
    }

    public String getMimeType() {
        if (MimeUtils.isEmptyOrGeneric(mimeType)) {
            mimeType = MimeUtils.getMimeType(url.getPath());
        }
        if (MimeUtils.isEmptyOrGeneric(mimeType)) {
            mimeType = MimeUtils.getMimeType(getName());
        }

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

    public void setThumbnail(FileThumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public boolean isImage() {
        boolean ret = MimeUtils.getContentTypeByFilename(getName()) == MimeUtils.ContentType.IMAGE;

        if (!ret)
            ret = MimeUtils.getContentTypeByFilename(getUrl().getLastPathSegment()) == MimeUtils.ContentType.IMAGE;

        if (!ret && !MimeUtils.isEmptyOrGeneric(mimeType)) {
            ret = (mimeType.endsWith("png") ||
                    mimeType.endsWith("jpg") ||
                    mimeType.endsWith("jpeg") ||
                    mimeType.endsWith("gif"));
        }
        return ret;
    }

    public boolean isGif() {
        return mimeType != null && mimeType.endsWith("gif");
    }

    public boolean isTranscoded() {
        if (transcodedPages == null)
            return false;
        else
            return transcodedPages.size() > 0;
    }

    public boolean isTranscodable() {
        switch(MimeUtils.getContentTypeByFilename(getName())) {
            case POWERPOINT:
            case WORD:
            case PDF:
                return true;
        }
        return false;
    }

    public boolean isVideo() {
        if (getMimeType() != null && getMimeType().startsWith("video"))
            return true;

        return MimeUtils.getContentTypeByFilename(getName()) == MimeUtils.ContentType.VIDEO
                || MimeUtils.getContentTypeByFilename(getUrl().getLastPathSegment()) == MimeUtils.ContentType.VIDEO;
    }

    public List<Page> getTranscodedPages() {
        return transcodedPages;
    }

    //
    // Implement Parcelable interface
    //
    public DisplayableFile(Parcel in) {
        thumbnail = in.readParcelable(FileThumbnail.class.getClassLoader());
        name = in.readString();
        url = in.readParcelable(Uri.class.getClassLoader());
        conversationId = in.readString();
        filesize = in.readLong();
        mimeType = in.readString();
        transcodedPages = new ArrayList<Page>();
        in.readList(transcodedPages, Page.class.getClassLoader());
    }

    public static final Parcelable.Creator<DisplayableFile> CREATOR
            = new Parcelable.Creator<DisplayableFile>() {
        public DisplayableFile createFromParcel(Parcel in) {
            return new DisplayableFile(in);
        }

        public DisplayableFile[] newArray(int size) {
            return new DisplayableFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeParcelable(thumbnail, flags);
        destination.writeString(name);
        destination.writeParcelable(url, flags);
        destination.writeString(conversationId);
        destination.writeLong(filesize);
        destination.writeString(mimeType);
        destination.writeList(transcodedPages);
    }

    @Override
    public String toString() {
        return "DisplayableFile : " + getName() + " : " + getUrl();
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

        if (getThumbnail() != null) {
            getThumbnail().decrypt(key);
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

        if (getThumbnail() != null) {
            getThumbnail().encrypt(key);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof DisplayableFile))
            return false;

        DisplayableFile df = (DisplayableFile) o;
        return TextUtils.equals(name, df.name)
                && UriUtils.equals(url, df.url)
                && TextUtils.equals(scr, df.scr)
                && TextUtils.equals(conversationId, df.conversationId)
                && TextUtils.equals(mimeType, df.mimeType);
    }
}
