package com.cisco.spark.android.sync;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.model.SingleUserAvatarUrlsInfo;
import com.cisco.spark.android.util.FileUtils;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.DATA_LOCAL_URI;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.DATA_REAL_URI;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.DATA_REMOTE_URI;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.DATA_SIZE;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.LAST_ACCESS;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.REMOTE_LAST_MODIFIED;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.TYPE;

public class ContentDataCacheRecord {
    long id;
    private Uri remoteUri, localUri, realUri;
    private long lastAccessTime;
    private String remoteLastModifiedTime;
    private long nextCheckForUpdateTime;
    private long dataSize;
    private ContentDataCacheEntry.Cache cacheType;

    /**
     * @return The numeric value from the _ID column
     */
    public long getId() {
        return id;
    }

    /**
     * @return The URI for downloading the remote content or the key to mapping the real URI of user's avatar.
     */
    public Uri getRemoteUri() {
        return remoteUri;
    }

    /**
     * @return The URI for content in local storage. Currently always a file:// but that may change.
     */
    public Uri getLocalUri() {
        return localUri;
    }

    /**
     * @return The URI for downloading the user's avatar from remote.
     */
    public Uri getRealUri() {
        return realUri;
    }

    public File getLocalUriAsFile() {
        if (localUri == null)
            return null;

        try {
            return new File(new URI(localUri.toString()));
        } catch (URISyntaxException e) {
            Ln.w("Invalid URI for local file " + localUri);
        }
        return null;
    }

    /**
     * @return The last time we accessed the content, for example when an avatar or thumbnail is
     * displayed in the UI. This value is cached in memory and occasionally flushed to disk.
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * @return The time value from the 'Last-Modified' header from the response when we downloaded
     * the content. Stored as a string to be passed back to the server in an 'If-Modified-Since'
     * header when checking for updates.
     */
    public String getRemoteLastModifiedTime() {
        return TextUtils.isEmpty(remoteLastModifiedTime)
                ? null
                : remoteLastModifiedTime;
    }

    /**
     * @return The size of the content file in bytes
     */
    public long getDataSize() {
        return dataSize;
    }

    /**
     * @return a value from the ContentDataCacheEntry.Cache enum
     */
    public ContentDataCacheEntry.Cache getCacheType() {
        return cacheType;
    }

    /**
     * @return Time we last checked the remote URI for modifications. Used for avatars. This value
     * is not persisted to the database.
     */
    public long getNextCheckForUpdateTime() {
        return nextCheckForUpdateTime;
    }

    public static ContentDataCacheRecord fromFileUri(ContentDataCacheEntry.Cache cacheType, Uri uri) {
        ContentDataCacheRecord ret = new ContentDataCacheRecord();
        ret.setCacheType(cacheType);
        ret.setLastAccessTime(System.currentTimeMillis());
        ret.setLocalUri(uri);
        ret.setRemoteUri(uri);

        return ret;
    }

    public static ContentDataCacheRecord getCacheRecordFromCursor(Cursor c) {

        ContentDataCacheRecord ret = new ContentDataCacheRecord();

        ret.id = c.getLong(ContentDataCacheEntry._id.ordinal());
        ret.remoteUri = Uri.parse(c.getString(DATA_REMOTE_URI.ordinal()));
        ret.lastAccessTime = c.getLong(LAST_ACCESS.ordinal());
        ret.dataSize = c.getInt(DATA_SIZE.ordinal());
        ret.cacheType = ContentDataCacheEntry.Cache.values()[c.getInt(TYPE.ordinal())];
        ret.remoteLastModifiedTime = (c.getString(REMOTE_LAST_MODIFIED.ordinal()));

        String localUriString = c.getString(DATA_LOCAL_URI.ordinal());
        if (!TextUtils.isEmpty(localUriString)) {
            ret.setLocalUri(Uri.parse(localUriString));
        }

        String realUriString = c.getString(DATA_REAL_URI.ordinal());
        if (!TextUtils.isEmpty(realUriString)) {
            ret.setRealUri(Uri.parse(realUriString));
        }

        return ret;
    }

    public static ContentDataCacheRecord getCacheRecordFromAvatarUrlsAPIResponse(SingleUserAvatarUrlsInfo avatarUrlsInfo, Uri remoteUri) {

        ContentDataCacheRecord ret = new ContentDataCacheRecord();

        ret.remoteUri = remoteUri;
        ret.cacheType = ContentDataCacheEntry.Cache.AVATAR;
        ret.lastAccessTime = System.currentTimeMillis();
        if (!avatarUrlsInfo.isDefaultAvatar() && !TextUtils.isEmpty(avatarUrlsInfo.getUrl())) {
            ret.setRealUri(Uri.parse(avatarUrlsInfo.getUrl()));
        }

        return ret;
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DATA_REMOTE_URI.name(), remoteUri.toString());
        cv.put(DATA_SIZE.name(), dataSize);
        cv.put(LAST_ACCESS.name(), lastAccessTime);
        cv.put(TYPE.name(), cacheType.ordinal());
        cv.put(REMOTE_LAST_MODIFIED.name(), remoteLastModifiedTime);

        if (localUri == null) {
            cv.putNull(DATA_LOCAL_URI.name());
        } else {
            cv.put(DATA_LOCAL_URI.name(), localUri.toString());
        }

        if (realUri == null) {
            cv.putNull(DATA_REAL_URI.name());
        } else {
            cv.put(DATA_REAL_URI.name(), realUri.toString());
        }

        return cv;
    }

    public ContentProviderOperation getUpdate() {
        ContentProviderOperation.Builder ret = ContentProviderOperation
                .newUpdate(ContentUris.withAppendedId(ContentDataCacheEntry.CONTENT_URI, id))
                .withValue(DATA_REMOTE_URI.name(), remoteUri.toString())
                .withValue(DATA_SIZE.name(), dataSize)
                .withValue(LAST_ACCESS.name(), lastAccessTime)
                .withValue(REMOTE_LAST_MODIFIED.name(), remoteLastModifiedTime)
                .withValue(TYPE.name(), cacheType.ordinal());

        ret = ret.withValue(DATA_LOCAL_URI.name(), localUri == null ? null : localUri.toString());
        ret = ret.withValue(DATA_REAL_URI.name(), realUri == null ? null : realUri.toString());

        return ret.build();
    }

    /**
     * Update avatar CDR by the GetAvatarsURL api response
     *
     * @param avatarInfo api response
     * @return true if the avatar realUri has changed, otherwise false.
     */
    public boolean updateRealUriByAvatarUrlsAPIResponse(@NonNull SingleUserAvatarUrlsInfo avatarInfo) {
        if (realUri == null && avatarInfo.isDefaultAvatar()) {
            return false;
        }

        if (realUri != null && TextUtils.equals(realUri.toString(), avatarInfo.getUrl())) {
            return false;
        }

        Ln.d("$BITMAP update realUri of remoteUri: " + remoteUri.toString() + "; oldRealUri: "
                + (realUri == null ? "null" : realUri.toString()) + "; newRealUri: " + avatarInfo.getUrl());

        if (avatarInfo.isDefaultAvatar() || TextUtils.isEmpty(avatarInfo.getUrl())) {
            realUri = null;
        } else {
            realUri = Uri.parse(avatarInfo.getUrl());
        }

        deleteLocalFile();

        localUri = null;
        dataSize = 0;
        lastAccessTime = System.currentTimeMillis();
        remoteLastModifiedTime = null;

        return true;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void setRemoteUri(Uri remoteUri) {
        this.remoteUri = remoteUri;
    }

    public void setLocalUri(Uri localUri) {
        this.localUri = localUri;
    }

    public void setRealUri(Uri realUri) {
        this.realUri = realUri;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public void setCacheType(ContentDataCacheEntry.Cache cacheType) {
        this.cacheType = cacheType;
    }

    public void setNextCheckForUpdateTime() {
        if (cacheType == ContentDataCacheEntry.Cache.AVATAR) {
            setNextCheckForUpdateTime(getAvatarUpdateTime());
        } else {
            setNextCheckForUpdateTime(Long.MAX_VALUE);
        }
    }

    public void setNextCheckForUpdateTime(long nextCheckForUpdateTime) {
        this.nextCheckForUpdateTime = nextCheckForUpdateTime;
    }

    public void setRemoteLastModifiedTime(String remoteLastModifiedTime) {
        this.remoteLastModifiedTime = remoteLastModifiedTime;
    }

    public boolean hasValidLocalContent() {
        if (localUri == null) {
            return realUri == null;
        }

        File f = getLocalUriAsFile();

        if (f == null)
            return false;

        // Handle case where previously fetched file is now deleted
        return (f.exists());
    }

    public boolean validate() {
        if (localUri == null) {
            // only avatar case localUri can be null, either below case is valid.
            // 1) localUri == null && realUri == null: default avatar
            // 2) localUri == null && realUri != null: haven't download avatar yet
            return true;
        }

        return hasValidLocalContent();
    }

    public boolean isStale() {
        return System.currentTimeMillis() > getNextCheckForUpdateTime();
    }

    public void deleteLocalFile() {
        if (localUri != null) {
            File f = getLocalUriAsFile();
            if (f != null) {
                FileUtils.deleteRecursive(f);
            }
        }
    }

    private long getAvatarUpdateTime() {
        return System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
    }
}
