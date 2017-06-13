package com.cisco.spark.android.metrics;

import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.MimeUtils;

public class ContentMetricsBuilder extends CirconusMetricsBuilder {
    public static final String CONTENTSHARING_UPLOAD_IMAGE_SUCCESS_TAG = "android.contentsharing.upload.image.success";
    public static final String CONTENTSHARING_UPLOAD_IMAGE_FAILURE_TAG = "android.contentsharing.upload.image.failure";
    public static final String CONTENTSHARING_UPLOAD_DOCUMENT_SUCCESS_TAG = "android.contentsharing.upload.document.success";
    public static final String CONTENTSHARING_UPLOAD_DOCUMENT_FAILURE_TAG = "android.contentsharing.upload.document.failure";
    public static final String CONTENTSHARING_UPLOAD_THUMBNAIL_SUCCESS_TAG = "android.contentsharing.upload.thumbnail.success";
    public static final String CONTENTSHARING_UPLOAD_THUMBNAIL_FAILURE_TAG = "android.contentsharing.upload.thumbnail.failure";
    public static final String CONTENTSHARING_UPLOAD_ENCRYPTED_SUCCESS_TAG = "android.contentsharing.upload.encrypted.success";
    public static final String CONTENTSHARING_UPLOAD_ENCRYPTED_FAILURE_TAG = "android.contentsharing.upload.encrypted.failure";
    public static final String CONTENTSHARING_DOWNLOAD_IMAGE_SUCCESS_TAG = "android.contentsharing.download.image.success";
    public static final String CONTENTSHARING_DOWNLOAD_IMAGE_FAILURE_TAG = "android.contentsharing.download.image.failure";
    public static final String CONTENTSHARING_DOWNLOAD_DOCUMENT_SUCCESS_TAG = "android.contentsharing.download.document.success";
    public static final String CONTENTSHARING_DOWNLOAD_DOCUMENT_FAILURE_TAG = "android.contentsharing.download.document.failure";
    public static final String CONTENTSHARING_DOWNLOAD_DECRYPTED_SUCCESS_TAG = "android.contentsharing.download.decrypted.success";
    public static final String CONTENTSHARING_DOWNLOAD_DECRYPTED_FAILURE_TAG = "android.contentsharing.download.decrypted.failure";
    public static final String CONTENTSHARING_DOWNLOAD_THUMBNAIL_SUCCESS_TAG = "android.contentsharing.download.thumbnail.success";
    public static final String CONTENTSHARING_DOWNLOAD_THUMBNAIL_FAILURE_TAG = "android.contentsharing.download.thumbnail.failure";
    public static final String CONTENTSHARING_DOWNLOAD_PAGE_SUCCESS_TAG = "android.contentsharing.download.page.success";
    public static final String CONTENTSHARING_DOWNLOAD_PAGE_FAILURE_TAG = "android.contentsharing.download.page.failure";
    public static final String CONTENTSHARING_DOCUMENT_VIEWED_TAG = "android.contentsharing.document.viewed";
    public static final String CONTENTSHARING_DOCUMENT_OPENED_TAG = "android.contentsharing.document.opened";
    public static final String CONTENTSHARING_DOCUMENT_SHARED_TAG = "android.contentsharing.document.shared";
    public static final String CONTENTSHARING_DOCUMENT_FORWARDED_TAG = "android.contentsharing.document.forwarded";

    public static final String AVATAR_DOWNLOAD_SUCCESS_TAG = "android.avatar.download.success";
    public static final String AVATAR_DOWNLOAD_FAILURE_TAG = "android.avatar.download.failure";
    public static final String AVATAR_UPLOAD_SUCCESS_TAG = "android.avatar.upload.success";
    public static final String AVATAR_UPLOAD_FAILURE_TAG = "android.avatar.upload.failure";
    public static final String AVATAR_SOURCE_CAMERA_TAG = "android.avatar.upload.fromCamera";
    public static final String AVATAR_SOURCE_ALBUM_TAG = "android.avatar.upload.fromPhotoAlbum";

    public ContentMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public void addContentMetrics(boolean upload, boolean isEncrypted, boolean isThumbnail, boolean isImage, boolean success) {
        if (upload) {
            if (isThumbnail) {
                incrementCounter(success ? CONTENTSHARING_UPLOAD_THUMBNAIL_SUCCESS_TAG : CONTENTSHARING_UPLOAD_THUMBNAIL_FAILURE_TAG);
            } else if (isImage) {
                incrementCounter(success ? CONTENTSHARING_UPLOAD_IMAGE_SUCCESS_TAG : CONTENTSHARING_UPLOAD_IMAGE_FAILURE_TAG);
            } else {
                incrementCounter(success ? CONTENTSHARING_UPLOAD_DOCUMENT_SUCCESS_TAG : CONTENTSHARING_UPLOAD_DOCUMENT_FAILURE_TAG);
            }
            if (isEncrypted) {
                incrementCounter(success ? CONTENTSHARING_UPLOAD_ENCRYPTED_SUCCESS_TAG : CONTENTSHARING_UPLOAD_ENCRYPTED_FAILURE_TAG);
            }
        } else {
            if (isThumbnail) {
                incrementCounter(success ? CONTENTSHARING_DOWNLOAD_THUMBNAIL_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_THUMBNAIL_FAILURE_TAG);
            } else if (isImage) {
                incrementCounter(success ? CONTENTSHARING_DOWNLOAD_IMAGE_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_IMAGE_FAILURE_TAG);
            } else {
                incrementCounter(success ? CONTENTSHARING_DOWNLOAD_DOCUMENT_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_DOCUMENT_FAILURE_TAG);
            }
            if (isEncrypted) {
                incrementCounter(success ? CONTENTSHARING_DOWNLOAD_DECRYPTED_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_DECRYPTED_FAILURE_TAG);
            }
        }
    }

    public void addDownloadMetrics(ConversationContract.ContentDataCacheEntry.Cache cacheType, String fileExtension, boolean isEncrypted, boolean isTranscoded, boolean succeeded) {
        switch (cacheType) {
            case THUMBNAIL:
                incrementCounter(succeeded ? CONTENTSHARING_DOWNLOAD_THUMBNAIL_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_THUMBNAIL_FAILURE_TAG);
                break;
            case MEDIA:
                if (isTranscoded)
                    incrementCounter(succeeded ? CONTENTSHARING_DOWNLOAD_PAGE_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_PAGE_FAILURE_TAG);
                else if (MimeUtils.isImageExt(fileExtension))
                    incrementCounter(succeeded ? CONTENTSHARING_DOWNLOAD_IMAGE_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_IMAGE_FAILURE_TAG);
                else if (succeeded)
                    incrementCounter(CONTENTSHARING_DOWNLOAD_DOCUMENT_SUCCESS_TAG);
                else
                    incrementCounter(CONTENTSHARING_DOWNLOAD_DOCUMENT_FAILURE_TAG);
                break;
            case AVATAR:
                if (succeeded)
                    incrementCounter(AVATAR_DOWNLOAD_SUCCESS_TAG);
                else
                    incrementCounter(AVATAR_DOWNLOAD_FAILURE_TAG);
                break;
        }
        if (isEncrypted) {
            incrementCounter(succeeded ? CONTENTSHARING_DOWNLOAD_DECRYPTED_SUCCESS_TAG : CONTENTSHARING_DOWNLOAD_DECRYPTED_FAILURE_TAG);
        }
    }
}
