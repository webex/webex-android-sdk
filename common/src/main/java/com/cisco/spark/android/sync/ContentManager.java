package com.cisco.spark.android.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AvatarProvider;
import com.cisco.spark.android.events.UIBackgroundTransition;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricNames;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.metrics.value.EncryptionMetrics;
import com.cisco.spark.android.metrics.value.GenericMetricTagEnums;
import com.cisco.spark.android.model.GetAvatarUrlsResponse;
import com.cisco.spark.android.model.SingleUserAvatarUrlsInfo;
import com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.BitmapUtils;
import com.cisco.spark.android.util.BitmapUtils.ScaleProvider.ScaleType;
import com.cisco.spark.android.util.CompletedFuture;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.LifoBlockingDeque;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.CONTENT_URI;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.Cache;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.DATA_REMOTE_URI;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.LAST_ACCESS;
import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.TYPE;
import static com.cisco.spark.android.sync.ConversationContract.vw_ContentCacheSize;
import static com.cisco.spark.android.ui.BitmapProvider.RETENTION_MODE_STANDARD;

/**
 * ContentManager <p/> Class for tracking message content files and garbage collecting
 */
@Singleton
public class ContentManager {
    public static final String FILEPROVIDER_AUTHORITY = "com.cisco.wx2.android.fileprovider";
    private static final int THREAD_POOL_SIZE = 3;
    private static final String CONTENTDIR = "content";

    private static final int ONE_MEG = 1024 * 1024;
    private static final int MAX_CONTENT_CACHE_SIZE = 150 * ONE_MEG;
    private static final int MAX_THUMBNAIL_CACHE_SIZE = 50 * ONE_MEG;
    private static final int MAX_AVATAR_CACHE_SIZE = 50 * ONE_MEG;

    // To gracefully handle downloading (or uploading!) large files, do not let GC trim the cache to fewer than min elements
    private static final int MIN_ITEMS_IN_CACHE = 4;
    private static final int MAX_THUMBNAIL_BYTES = MAX_THUMBNAIL_CACHE_SIZE / 100;

    private final EventBus bus;
    private final LoggingLock taskLock;
    private final LoggingLock dbLock;

    private boolean hasScannedForOrphans;

    private class ContentCache extends ConcurrentHashMap<Uri, ContentDataCacheRecord> {
        ContentCache(Cache cacheType, int maxSize) {
            this.cacheType = cacheType;
            this.maxSize = maxSize;
        }

        private long currentSize;
        private long maxSize;
        private Cache cacheType;

        public Cache getCacheType() {
            return cacheType;
        }

        public void addRecord(@NonNull ContentDataCacheRecord cacheRecord) {
            removeRecord(cacheRecord);

            put(cacheRecord.getRemoteUri(), cacheRecord);
            currentSize += cacheRecord.getDataSize();
        }

        public void updateRecord(@NonNull ContentDataCacheRecord cacheRecord) {
            ContentDataCacheRecord record = remove(cacheRecord.getRemoteUri());
            if (record != null) {
                currentSize -= record.getDataSize();
                put(cacheRecord.getRemoteUri(), cacheRecord);
                currentSize += cacheRecord.getDataSize();
            }
        }

        public void removeRecord(@NonNull ContentDataCacheRecord cacheRecord) {
            ContentDataCacheRecord record = remove(cacheRecord.getRemoteUri());
            if (record != null) {
                currentSize -= record.getDataSize();
            }
        }
    }

    private ContentCache contentCache = new ContentCache(Cache.MEDIA, MAX_CONTENT_CACHE_SIZE);
    private ContentCache thumbnailCache = new ContentCache(Cache.THUMBNAIL, MAX_THUMBNAIL_CACHE_SIZE);
    private ContentCache avatarCache = new ContentCache(Cache.AVATAR, MAX_AVATAR_CACHE_SIZE);

    public static class CacheRecordRequestParameters {
        private Uri remoteUri;
        private String uuidOrEmail;
        private String fileName;
        private @BitmapProvider.RetentionMode int retentionMode;
        private BitmapProvider.BitmapType bitmapType;

        public CacheRecordRequestParameters(Uri remoteUri, String uuidOrEmail, String fileName, @BitmapProvider.RetentionMode int retentionMode, BitmapProvider.BitmapType bitmapType) {
            this.remoteUri = remoteUri;
            this.uuidOrEmail = uuidOrEmail;
            this.fileName = fileName;
            this.retentionMode = retentionMode;
            this.bitmapType = bitmapType;
        }

        public Uri getRemoteUri() {
            return remoteUri;
        }

        public String getUuidOrEmail() {
            return uuidOrEmail;
        }

        public String getFileName() {
            return fileName;
        }

        public @BitmapProvider.RetentionMode int getRetentionMode() {
            return retentionMode;
        }

        public BitmapProvider.BitmapType getBitmapType() {
            return bitmapType;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    private long lastDbSyncTime;

    private int workerThreadId;
    private ExecutorService executor;
    private ExecutorService remoteContentRefreshExecutor;

    private final Context context;
    private final ApiClientProvider apiClientProvider;
    private final ApiTokenProvider apiTokenProvider;
    private final MetricsReporter metricsReporter;
    private final AvatarProvider avatarProvider;
    private final Provider<Batch> batchProvider;
    private final OperationQueue operationQueue;
    private final ConcurrentHashMap<Uri, FetchContentTask> fetchesInProgress = new ConcurrentHashMap<>();
    private volatile int taskQueueSize;

    @Inject
    public ContentManager(Context context, ApiClientProvider apiClientProvider, ApiTokenProvider apiTokenProvider, EventBus bus, MetricsReporter metricsReporter, AvatarProvider avatarProvider, Provider<Batch> batchProvider, OperationQueue operationQueue) {
        this.context = context;
        this.apiClientProvider = apiClientProvider;
        this.apiTokenProvider = apiTokenProvider;
        this.bus = bus;
        this.metricsReporter = metricsReporter;
        this.avatarProvider = avatarProvider;
        this.batchProvider = batchProvider;
        this.operationQueue = operationQueue;

        this.bus.register(this);

        taskLock = new LoggingLock(BuildConfig.DEBUG, "ContentManager Tasks");
        dbLock = new LoggingLock(BuildConfig.DEBUG, "ContentManager DB");

        createDirectories();

        initializeExecutors();
    }

    public void initializeExecutors() {
        workerThreadId = 0;

        executor = new ThreadPoolExecutor(
                THREAD_POOL_SIZE, THREAD_POOL_SIZE, 30,
                TimeUnit.SECONDS,
                new LifoBlockingDeque<>(),
                runnable -> {
                    Thread ret = new Thread(runnable);
                    ret.setName("ContentManager Executor " + workerThreadId++);
                    ret.setDaemon(true);
                    return ret;
                });

        remoteContentRefreshExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread ret = new Thread(runnable);
            ret.setName("ContentManager Refresh Executor");
            ret.setDaemon(true);
            return ret;
        });
    }

    public void clear() {
        taskLock.lock();
        try {
            // Shutdown the executors to remove all pending work.
            if (executor != null)
                executor.shutdownNow();
            if (remoteContentRefreshExecutor != null)
                remoteContentRefreshExecutor.shutdown();
            fetchesInProgress.clear();

            File dir = getContentDirectory();
            FileUtils.deleteRecursive(dir);
            contentCache.clear();
            thumbnailCache.clear();
            avatarCache.clear();

            // Restart the executors for the next time we need them.
            initializeExecutors();
        } finally {
            taskLock.unlock();
        }
    }

    /**
     * Asynchronously fetch a ContentDataCacheRecord for a bitmap. Safe to call from the main
     * thread.
     */
    public Future<ContentDataCacheRecord> getCacheRecord(CacheRecordRequestParameters parameters, SecureContentReference secureContentReference, Action<ContentDataCacheRecord> callback) {
        Cache cacheType = getCacheType(parameters.getBitmapType());

        return getCacheRecord(cacheType, parameters, secureContentReference, callback);
    }

    /**
     * Asynchronously fetch a ContentDataCacheRecord for a content file, downloading the file if
     * necessary. Safe to call from the main thread.
     *
     * @param callback Optional callback for when the content is ready
     * @return a future ContentDataCacheRecord
     */
    public Future<ContentDataCacheRecord> getCacheRecord(Cache cacheType, Uri uri, SecureContentReference secureContentReference, String filename,
                                                         Action<ContentDataCacheRecord> callback) {
        CacheRecordRequestParameters parameters = new CacheRecordRequestParameters(uri, null, filename, RETENTION_MODE_STANDARD, null);
        return getCacheRecord(cacheType, parameters, secureContentReference, callback);
    }

    /**
     * Asynchronously fetch a ContentDataCacheRecord for a series of bitmaps. Safe to call from the
     * main thread.
     */
    public void getCacheRecords(final Cache cacheType, @NonNull final List<Uri> uris, final Action<List<ContentDataCacheRecord>> callback) {
        final List<ContentDataCacheRecord> records = new ArrayList<>();
        final List<Uri> queue = new ArrayList<>(uris);

        for (Uri uri : uris) {
            final Uri finalUri = uri;
            final String correctFileName = getContentFileName(cacheType, uri, null);
            getCacheRecord(cacheType, uri, null, correctFileName, new Action<ContentDataCacheRecord>() {
                @Override
                public void call(ContentDataCacheRecord item) {
                    if (item != null) {
                        records.add(item);
                    }

                    queue.remove(finalUri);

                    if (queue.size() == 0) {
                        callback.call(records);
                    }
                }
            });
        }
    }

    /**
     * BitmapTypes (memory cache) are associated with a Cache type (disk cache). This resolves a
     * bitmap type to the bucket it belongs in.
     */
    public static Cache getCacheType(BitmapProvider.BitmapType bitmaptype) {
        Cache cachetype;

        switch (bitmaptype) {
            case THUMBNAIL:
            case VIDEO_THUMBNAIL:
                cachetype = Cache.THUMBNAIL;
                break;
            case AVATAR:
            case AVATAR_READRECEIPT:
            case AVATAR_NOTIFICATION:
            case AVATAR_CALL_PARTICIPANT:
            case SIDE_NAV_AVATAR:
            case CHIP:
            case SETTINGS_AVATAR:
            case AVATAR_LARGE:
            case AVATAR_ROOM_DETAILS_DIALOG:
            case AVATAR_EDIT:
                cachetype = Cache.AVATAR;
                break;
            case LARGE:
            case MULTIPAGE_DOCUMENT:
            default:
                cachetype = Cache.MEDIA;
                break;
        }
        return cachetype;
    }

    private Future<ContentDataCacheRecord> getCacheRecord(Cache cacheType, CacheRecordRequestParameters parameters, SecureContentReference secureContentReference,
                                                          Action<ContentDataCacheRecord> callback) {
        final Uri uri = parameters.getRemoteUri();
        String filename = parameters.getFileName();
        if (TextUtils.isEmpty(parameters.getFileName())) {
            filename = uri.getLastPathSegment();
        }

        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())
                && MimeUtils.getContentTypeByFilename(parameters.getFileName()) != MimeUtils.ContentType.UNKNOWN) {
            ContentDataCacheRecord ret = ContentDataCacheRecord.fromFileUri(cacheType, uri);
            if (callback != null)
                callback.call(ret);
            return new CompletedFuture<>(ret);
        }

        if (!"https".equals(uri.getScheme())
                && !"http".equals(uri.getScheme())
                && !"content".equals(uri.getScheme())) {
            Ln.w("Unsupported scheme in requested uri " + uri);
            if (callback != null)
                callback.call(null);
            return new CompletedFuture<>(null);
        }

        if (filename != null && filename.contains(File.separator)) {
            String[] path = filename.split(File.separator);
            filename = path[path.length - 1];
        }

        ContentCache cache = getCache(cacheType);
        ContentDataCacheRecord record = cache.get(uri);
        boolean hasQueriedDB = false;

        if (record != null) {
            if (!record.isStale() && record.hasValidLocalContent()) {
                record.setLastAccessTime(System.currentTimeMillis());
                if (callback != null) {
                    callback.call(record);
                }
                return new CompletedFuture<>(record);
            }
        } else {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                // don't want to query DB again in FetchContentTask
                hasQueriedDB = true;
                record = queryValidCacheRow(uri, cacheType);
                if (record != null && record.hasValidLocalContent()) {
                    if (callback != null) {
                        callback.call(record);
                    }
                    cache.addRecord(record);
                    return new CompletedFuture<>(record);
                }
            }
        }

        taskLock.lock();
        try {
            FetchContentTask task = fetchesInProgress.get(uri);
            if (task == null) {
                Ln.v("$BITMAP starting async task to fetch CDR for " + uri);
                parameters.setFileName(filename);
                task = new FetchContentTask(record, cacheType, parameters, secureContentReference, callback, hasQueriedDB, false);
                task.setFuture(executor.submit(task));
                fetchesInProgress.put(uri, task);
            } else if (callback != null) {
                Ln.v("$BITMAP ContentManager task already in progress for " + uri);
                task.addCallbackAction(callback);
            }
            return task.getFuture();

        } finally {
            taskLock.unlock();
        }
    }

    /**
     * Add a local file to the cache as the user's avatar. Used to improve UX when users upload a
     * new avatar so network issues don't make it look like the upload didn't take. <p/> Call from a
     * background thread.
     */
    public void setSelfAvatar(File avatarFile) {
        if (avatarFile == null || !avatarFile.exists() || !avatarFile.isFile()) {
            Ln.e("Avatar file is not a file, skipping. " + avatarFile);
            return;
        }

        for (AvatarProvider.AvatarSize size : AvatarProvider.AvatarSize.values()) {
            ContentDataCacheRecord ret = new ContentDataCacheRecord();
            ret.setRemoteUri(avatarProvider.getUri(apiTokenProvider.getAuthenticatedUser().getUserId(), size));
            ret.setCacheType(Cache.AVATAR);

            File outfile = getContentFile(ret.getCacheType(), ret.getRemoteUri(), "avatar.png");

            if (outfile.exists())
                outfile.delete();

            try {
                outfile.createNewFile();
                FileUtils.copyFile(avatarFile, outfile);
                ret.setLocalUri(Uri.fromFile(outfile));
                ret.setDataSize(outfile.length());
                ret.setNextCheckForUpdateTime();
            } catch (IOException e) {
                Ln.e(false, e);
            }

            avatarCache.put(ret.getRemoteUri(), ret);
            writeCacheRow(ret);
            onFetchComplete(ret);
        }
    }

    private void onRemoteBitmapUpdated(final Uri remoteUri, BitmapProvider.BitmapType bitmapType) {
        callContentListeners(new Action<ContentListener>() {
            @Override
            public void call(ContentListener item) {
                item.onRemoteBitmapUpdated(remoteUri, bitmapType);
            }
        });
    }

    private void onFetchAvatarUrlComplete(final CacheRecordRequestParameters parameters) {
        callContentListeners(new Action<ContentListener>() {
            @Override
            public void call(ContentListener item) {
                item.onFetchAvatarUrlComplete(parameters);
            }
        });
    }

    private void onFetchComplete(final ContentDataCacheRecord cdr) {
        callContentListeners(new Action<ContentListener>() {
            @Override
            public void call(ContentListener item) {
                item.onFetchComplete(cdr);
            }
        });
    }

    public ContentDataCacheRecord addUploadedContent(File file, Uri remoteUri, Cache cacheType) {
        if (file == null || !file.exists() || !file.isFile()) {
            Ln.e("File is not a file, skipping");
            return null;
        }

        ContentDataCacheRecord ret = new ContentDataCacheRecord();
        ret.setRemoteUri(remoteUri);
        ret.setCacheType(cacheType);
        ret.setDataSize(file.length());
        ret.setLastAccessTime(System.currentTimeMillis());
        ret.setNextCheckForUpdateTime();

        File outfile = file;

        try {
            if (!file.getCanonicalPath().startsWith(getContentDirectory(cacheType).getCanonicalPath())) {
                outfile = getContentFile(ret.getCacheType(), ret.getRemoteUri(), file.getName());
                FileUtils.copyFile(file, outfile);
            }
        } catch (IOException e) {
            Ln.e(e, "Failed copying file");
        }

        ret.setLocalUri(Uri.fromFile(outfile));
        getCache(cacheType).put(ret.getRemoteUri(), ret);
        if (!"file".equals(ret.getRemoteUri().getScheme()))
            writeCacheRow(ret);
        onFetchComplete(ret);
        return ret;
    }

    /**
     * Task for getting data from the uri into a local file and storing the reference in our LRU
     * cache.
     */
    private class FetchContentTask implements Callable<ContentDataCacheRecord> {
        private final SecureContentReference secureContentReference;
        private CacheRecordRequestParameters parameters;
        private ContentDataCacheRecord cacheRecord;
        final Uri uri;
        ContentDataCacheEntry.Cache cacheType;
        private boolean isRefreshTask;
        private boolean hasQueriedDB;
        ArrayList<Action<ContentDataCacheRecord>> actions = new ArrayList<>();
        private Future<ContentDataCacheRecord> future;
        private long createTime, startTime;

        public FetchContentTask(ContentDataCacheRecord cacheRecord, Cache cacheType, CacheRecordRequestParameters parameters,
                                SecureContentReference secureContentReference, Action<ContentDataCacheRecord> action, boolean hasQueriedDB, boolean isRefreshTask) {
            this.cacheRecord = cacheRecord;
            this.cacheType = cacheType;
            this.parameters = parameters;
            this.uri = parameters.getRemoteUri();
            this.secureContentReference = secureContentReference;
            if (action != null)
                actions.add(action);
            this.hasQueriedDB = hasQueriedDB;
            this.isRefreshTask = isRefreshTask;

            ++taskQueueSize;
            if (taskQueueSize > THREAD_POOL_SIZE * 2) {
                Ln.d("$PERF fetches in progress: " + (taskQueueSize));
            }
            createTime = System.currentTimeMillis();
        }

        private void addCallbackAction(Action<ContentDataCacheRecord> action) {
            taskLock.lock();
            try {
                actions.add(action);
            } finally {
                taskLock.unlock();
            }
        }

        @Override
        public ContentDataCacheRecord call() {

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            startTime = System.currentTimeMillis();

            callContentListeners(new Action<ContentListener>() {
                @Override
                public void call(ContentListener listener) {
                    listener.onFetchStart(uri);
                }
            });

            try {
                ContentCache cache = getCache(cacheType);

                if (cacheRecord != null) {
                    if (!cacheRecord.validate()) {
                        context.getContentResolver().delete(ContentUris.withAppendedId(ContentDataCacheEntry.CONTENT_URI,
                                cacheRecord.getId()), null, null);
                        cacheRecord.deleteLocalFile();
                        cache.removeRecord(cacheRecord);
                        cacheRecord = null;
                    } else if (!cacheRecord.isStale() && cacheRecord.hasValidLocalContent()) {
                        Ln.v("$BITMAP CDR is not stale yet. Aborting fetch " + uri);
                        return cacheRecord;
                    }
                } else if (!hasQueriedDB) {
                    // don't want to query DB again.
                    cacheRecord = queryValidCacheRow(uri, cacheType);
                    if (cacheRecord != null && cacheRecord.hasValidLocalContent()) {
                        Ln.v("$BITMAP Got record from db; skipping download " + uri);
                        return cacheRecord;
                    }
                    Ln.v("$BITMAP ContentDataCacheRecord DB cache miss for " + uri + " or hasn't been downloaded");
                }

                if (cacheType == Cache.AVATAR && !TextUtils.isEmpty(parameters.getUuidOrEmail()) && (cacheRecord == null || cacheRecord.isStale())) {
                    Ln.d("$BITMAP getAvatarUrls: " + parameters.getUuidOrEmail());
                    if (cacheRecord != null) {
                        Ln.v("$BITMAP CDR is stale remove from cache: " + uri);
                        cache.removeRecord(cacheRecord);
                        cacheRecord = null;
                    }
                    operationQueue.getAvatarUrls(parameters);
                    return null;
                }

                Ln.v("$BITMAP fetching content record for " + uri);
                cacheRecord = fetchRemoteContent(cacheRecord, cacheType, uri, secureContentReference, parameters.getFileName(), parameters.getBitmapType());
                return cacheRecord;
            } catch (Throwable e) {
                Ln.e(e, "Failed getting content");
                Ln.d(e, "Failed getting content for " + uri);
                return null;
            } finally {
                taskFinished(uri, cacheRecord);
                --taskQueueSize;
                if (startTime - createTime > 1000 || System.currentTimeMillis() - startTime > 2000)
                    Ln.d("$PERF ContentManager task waited " + (startTime - createTime) + " ms and worked for " + (System.currentTimeMillis() - startTime) + " ms.  TIP=" + (taskQueueSize) + " " + uri);
            }
        }

        public void setFuture(Future<ContentDataCacheRecord> future) {
            this.future = future;
        }

        public Future<ContentDataCacheRecord> getFuture() {
            return future;
        }

        private void taskFinished(final Uri uri, final ContentDataCacheRecord record) {
            taskLock.lock();
            try {
                Ln.v("$BITMAP CDR task finished for " + uri);
                fetchesInProgress.remove(uri);

                if (record == null || !record.validate()) {
                    return;
                }

                record.setLastAccessTime(System.currentTimeMillis());

                Ln.v("$BITMAP calling CDR callbacks for " + uri);
                for (Action<ContentDataCacheRecord> action : actions) {
                    action.call(record);
                }

                ContentCache cache = getCache(record.getCacheType());
                cache.addRecord(record);
            } finally {
                taskLock.unlock();
            }

            onFetchComplete(record);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(GetAvatarUrlsResponse response) {
        Ln.d("$BITMAP onEvent GetAvatarUrlsResponse begin");

        Map<String, Map<Integer, SingleUserAvatarUrlsInfo>> avatarUrlsResponseMap = response.getAvatarUrlsMap();
        if (avatarUrlsResponseMap == null || avatarUrlsResponseMap.isEmpty()) {
            Ln.i("invalid avatar urls response.");
            return;
        }

        Map<Uri, ContentManager.CacheRecordRequestParameters> parametersMap = response.getParametersMap();
        Map<Uri, SingleUserAvatarUrlsInfo> avatarUrlsMap = new HashMap<>();
        List<String> remoteUriList = new ArrayList<>();

        for (String uuidOrEmail : avatarUrlsResponseMap.keySet()) {
            Map<Integer, SingleUserAvatarUrlsInfo> oneUserAvatarInfoMap = avatarUrlsResponseMap.get(uuidOrEmail);
            for (Integer size : oneUserAvatarInfoMap.keySet()) {
                Uri remoteUri = avatarProvider.getUri(uuidOrEmail, String.valueOf(size));
                avatarUrlsMap.put(remoteUri, oneUserAvatarInfoMap.get(size));
                remoteUriList.add(remoteUri.toString());
                Ln.d("$BITMAP remoteUri: " + remoteUri.toString() + "; realUri: " + (oneUserAvatarInfoMap.get(size).isDefaultAvatar() ? "defaultAvatar" : oneUserAvatarInfoMap.get(size).getUrl()));
            }
        }

        if (remoteUriList.isEmpty()) {
            Ln.i("no valid avatar urls response.");
            return;
        }

        Cursor c = null;
        Batch batch = batchProvider.get();
        ContentCache cache = getCache(Cache.AVATAR);
        List<ContentDataCacheRecord> cdrList = new ArrayList<>();
        try {
            if (remoteUriList.size() == 1) {
                c = context.getContentResolver().query(ContentDataCacheEntry.CONTENT_URI,
                        ContentDataCacheEntry.DEFAULT_PROJECTION,
                        DATA_REMOTE_URI + "=? AND " + TYPE + "=" + String.valueOf(Cache.AVATAR.ordinal()),
                        new String[]{remoteUriList.get(0)}, null);
            } else {
                c = context.getContentResolver().query(ContentDataCacheEntry.CONTENT_URI,
                        ContentDataCacheEntry.DEFAULT_PROJECTION,
                        DATA_REMOTE_URI + " IN (" + Strings.makeInClausePlaceholders(remoteUriList.size()) + ") AND " + TYPE + "=" + String.valueOf(Cache.AVATAR.ordinal()),
                        remoteUriList.toArray(new String[0]), null);
            }
            while (c != null && c.moveToNext()) {
                ContentDataCacheRecord ret = ContentDataCacheRecord.getCacheRecordFromCursor(c);
                SingleUserAvatarUrlsInfo singleUserAvatarUrlsInfo = avatarUrlsMap.remove(ret.getRemoteUri());
                if (singleUserAvatarUrlsInfo == null) {
                    continue;
                }
                if (ret.updateRealUriByAvatarUrlsAPIResponse(singleUserAvatarUrlsInfo)) {
                    cache.removeRecord(ret);
                    batch.add(ret.getUpdate());

                    CacheRecordRequestParameters parameters = parametersMap.get(ret.getRemoteUri());
                    if (parameters != null) {
                        onRemoteBitmapUpdated(ret.getRemoteUri(), parametersMap.get(ret.getRemoteUri()).getBitmapType());
                    }
                } else {
                    if (cache.containsKey(ret.getRemoteUri())) {
                        Ln.v("$BITMAP update cache: " + ret.getRemoteUri());
                        // update stale info of data which is already in cache
                        ret.setLastAccessTime(System.currentTimeMillis());
                        ret.setNextCheckForUpdateTime();
                        cache.updateRecord(ret);
                    }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        batch.apply();

        batch.clear();
        for (Uri uri : avatarUrlsMap.keySet()) {
            ContentDataCacheRecord record = ContentDataCacheRecord.getCacheRecordFromAvatarUrlsAPIResponse(avatarUrlsMap.get(uri), uri);
            batch.add(ContentProviderOperation.newInsert(CONTENT_URI).withValues(record.getContentValues()).build());
        }
        batch.apply();


        if (parametersMap != null && !parametersMap.isEmpty()) {
            for (Uri remoteUri : parametersMap.keySet()) {
                onFetchAvatarUrlComplete(parametersMap.get(remoteUri));
            }
        }
        Ln.d("$BITMAP onEvent GetAvatarUrlsResponse end");
    }

    private ContentDataCacheRecord fetchRemoteContent(ContentDataCacheRecord ret, Cache cacheType, Uri uri, SecureContentReference secureContentReference, String filename, BitmapProvider.BitmapType bitmapType) {
        Ln.d("fetchRemoteContent " + uri);

        if (ret == null || !ret.validate()) {
            ret = new ContentDataCacheRecord();
            ret.setRemoteUri(uri);
            ret.setCacheType(cacheType);
        }

        File outFile = getContentFile(cacheType, uri, filename);

        Ln.d("fetching " + uri);
        try {
            if ("content".equals(uri.getScheme())) {
                ret = getFileFromContentResolver(ret, uri, outFile);
            } else {
                if (ret.getRealUri() != null) {
                    // use real uri to download user's avatar.
                    ret = downloadFile(ret, ret.getRealUri(), outFile, secureContentReference, bitmapType);
                } else {
                    ret = downloadFile(ret, uri, outFile, secureContentReference, bitmapType);
                }
            }
        } catch (Exception e) {
            Ln.i(e, "Failed fetching " + uri);
        }

        if (!outFile.exists()) {
            Ln.d("Failed writing file for uri " + uri);
            Ln.e("Failed writing file");
            return null;
        }

        if (MimeUtils.getContentTypeByFilename(outFile.getName()) == MimeUtils.ContentType.UNKNOWN) {
            String ext = MimeUtils.guessExtensionForUnknownFile(outFile);
            if (!TextUtils.isEmpty(ext)) {
                File newFile = new File(outFile.getAbsoluteFile() + "." + ext);
                if (outFile.exists() && !newFile.exists()) {
                    outFile.renameTo(newFile);
                    outFile = newFile;
                }
            }
        }

        ret.setRemoteUri(uri);
        ret.setLocalUri(Uri.fromFile(outFile));
        ret.setDataSize(outFile.length());
        ret.setCacheType(cacheType);
        ret.setLastAccessTime(System.currentTimeMillis());
        ret.setNextCheckForUpdateTime();

        writeCacheRow(ret);

        Ln.v("$BITMAP Writing cache row succeeded for " + uri);

        return ret;
    }

    private ContentDataCacheRecord getFileFromContentResolver(ContentDataCacheRecord record, Uri uri, File outFile) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            FileUtils.streamCopy(is, new FileOutputStream(outFile));
            record.setLocalUri(Uri.fromFile(outFile));
        } catch (FileNotFoundException e) {
            Ln.e(e, "Failed opening stream");
            Ln.d(e, "Failed opening stream from " + uri);
        }
        return record;
    }

    private boolean isTranscoded(Response response) {
        String contentDisposition = response.headers().get("Content-Disposition");
        return contentDisposition != null && contentDisposition.contains("filename=\"page-");
    }

    private ContentDataCacheRecord downloadFile(ContentDataCacheRecord ret, Uri url, File outFile, SecureContentReference secureContentReference, BitmapProvider.BitmapType bitmapType) throws IOException {
        GenericMetric metric = GenericMetric.buildOperationalMetric(ClientMetricNames.CLIENT_FILE_DOWNLOAD);

        if (bitmapType != null) {
            switch (bitmapType) {
                case THUMBNAIL:
                case VIDEO_THUMBNAIL:
                    metric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_THUMBNAIL);
                    break;
                case AVATAR:
                case AVATAR_READRECEIPT:
                case AVATAR_NOTIFICATION:
                case AVATAR_CALL_PARTICIPANT:
                case SIDE_NAV_AVATAR:
                case CHIP:
                case SETTINGS_AVATAR:
                case AVATAR_LARGE:
                case AVATAR_ROOM_DETAILS_DIALOG:
                case AVATAR_EDIT:
                    metric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_AVATAR);
                    break;
                case MULTIPAGE_DOCUMENT:
                    metric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_PREVIEW);
                    break;
                case LARGE:
                default:
                    metric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_FILE);
                    break;

            }
        } else {
            metric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_UNKNOWN);
        }

        Response response;
        long startTime = System.currentTimeMillis();
        response = apiClientProvider.getConversationClient().downloadFileIfModified(url.toString(), ret.getRemoteLastModifiedTime()).execute();
        long time = System.currentTimeMillis() - startTime;
        metric.addField(ClientMetricField.METRIC_FIELD_STORAGE_TRADCKING_ID, response.raw().header("X-Trans-Id"));
        metric.addField(ClientMetricField.METRIC_FIELD_DURATION_TO_FIRST_RECORD, time);
        metric.addNetworkStatus(response);

        if (response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            metric.addField(ClientMetricField.METRIC_FIELD_DURATION_FROM_FIRST_RECORD_TO_END, time);
            operationQueue.postGenericMetric(metric);
            return ret;
        }

        if (!response.isSuccessful()) {
            Ln.e("Failed download." + (Ln.isDebugEnabled() ? ret.getRemoteUri() : ""));
            metric.addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, false);
            operationQueue.postGenericMetric(metric);
            return ret;
        }

        ret.setRemoteLastModifiedTime(response.headers().get("Last-Modified"));

        Ln.v("$BITMAP Download succeeded for " + ret.getRemoteUri());
        Ln.d("$BITMAP Download succeeded for " + ret.getRemoteUri() + "; realUri: " + ret.getRealUri());

        long downloadStart = System.currentTimeMillis();
        InputStream is = ((ResponseBody) response.body()).byteStream();

        if (is == null) {
            Ln.v("$BITMAP Failed downloading from uri " + ret.getRemoteUri() + " : " + response.code() + " " + response.errorBody().string());
            metric.addField(ClientMetricField.METRIC_FIELD_DURATION_FROM_FIRST_RECORD_TO_END, time);
            metric.addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, false);
            metric.addField(ClientMetricField.METRIC_FIELD_ERROR_DESCRIPTION, "Failed to download due to null input stream");
            operationQueue.postGenericMetric(metric);
            return ret;
        }

        try {
            if (outFile.exists()) {
                Ln.d("$BITMAP Deleting old version of file for uri " + ret.getRemoteUri());
                getCache(ret.getCacheType()).currentSize -= outFile.length();
                outFile.delete();
            }

            // If we have a secureContentReference, then we need to decrypt it.
            InputStream in = secureContentReference != null ? secureContentReference.decrypt(is) : is;
            OutputStream out = new FileOutputStream(outFile);
            FileUtils.streamCopy(in, out);

            metric.addField(ClientMetricField.METRIC_FIELD_DURATION_FROM_FIRST_RECORD_TO_END, System.currentTimeMillis() - downloadStart);
            metric.addField(ClientMetricField.METRIC_FIELD_CONTENT_SIZE, outFile.length());

            if (secureContentReference != null) {
                EncryptionMetrics.ContentMetric contentMetric = new EncryptionMetrics.ContentMetric(
                        EncryptionMetrics.ContentMetric.Direction.down,
                        outFile.length(),
                        System.currentTimeMillis() - startTime,
                        ret.getCacheType() == Cache.THUMBNAIL
                                ? EncryptionMetrics.ContentMetric.Kind.thumbnail
                                : EncryptionMetrics.ContentMetric.Kind.fromFilename(outFile.getName())
                );

                MetricsReportRequest request = metricsReporter.newEncryptionSplunkMetricsBuilder().reportValue(contentMetric).build();
                metricsReporter.enqueueMetricsReport(request);
            }

            Ln.d("Wrote new file " + outFile.getAbsolutePath());

            // Prevent a misbehaving client from monopolizing our thumbnail cache with giant thumbnails
            if (ret.getCacheType() == Cache.THUMBNAIL && outFile.length() > MAX_THUMBNAIL_BYTES) {
                BitmapFactory.Options o = BitmapUtils.getBitmapDims(outFile);
                Ln.w(false, "Thumbnail too big (" + outFile.length() + " bytes; " + o.outWidth + " x " + o.outHeight + "), resizing. " + ret.getRemoteUri());
                Bitmap scaledBitmap = BitmapUtils.fileToBitmap(outFile, ImageUtils.OUTGOING_THUMBNAIL_MAX_PIXELS, ScaleType.DOWNSCALE_ONLY);
                outFile.delete();
                ImageUtils.writeBitmap(outFile, scaledBitmap);
                if (scaledBitmap != null)
                    scaledBitmap.recycle();
                else
                    Ln.w("Failed parsing thumbnail");
            }

            metric.addField(ClientMetricField.METRIC_FIELD_PERCIEVED_DURATION, System.currentTimeMillis() - startTime);
            metric.addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, true);
            operationQueue.postGenericMetric(metric);
        } catch (Exception e) {
            Ln.d(e, "Failed creating file " + outFile);
            metric.addField(ClientMetricField.METRIC_FIELD_PERCIEVED_DURATION, System.currentTimeMillis() - startTime);
            metric.addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, false);
            metric.addField(ClientMetricField.METRIC_FIELD_ERROR_DESCRIPTION, "Failed creating output file for bitmap");
            operationQueue.postGenericMetric(metric);
            Ln.e(e);
        }
        return ret;
    }

    private ContentDataCacheRecord queryValidCacheRow(Uri uri, Cache cacheType) {
        if (uri == null)
            return null;

        Cursor c = null;
        try {
            c = context.getContentResolver().query(ContentDataCacheEntry.CONTENT_URI,
                    ContentDataCacheEntry.DEFAULT_PROJECTION,
                    DATA_REMOTE_URI + "=? AND " + TYPE + "=?",
                    new String[]{uri.toString(), String.valueOf(cacheType.ordinal())}, null);

            if (c != null && c.moveToNext()) {
                ContentDataCacheRecord ret = ContentDataCacheRecord.getCacheRecordFromCursor(c);
                if (ret.validate()) {
                    ret.setLastAccessTime(System.currentTimeMillis());
                    ret.setNextCheckForUpdateTime();
                    return ret;
                } else {
                    context.getContentResolver().delete(ContentUris.withAppendedId(ContentDataCacheEntry.CONTENT_URI,
                            ret.getId()), null, null);
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    private void writeCacheRow(ContentDataCacheRecord record) {

        if (record.getId() == 0) {
            Uri newUri = context.getContentResolver().insert(CONTENT_URI, record.getContentValues());
            record.id = ContentUris.parseId(newUri);
        } else {
            Uri recordUri = ContentUris.withAppendedId(ContentDataCacheEntry.CONTENT_URI, record.id);
            context.getContentResolver().update(recordUri, record.getContentValues(), null, null);
        }

        ContentCache cache = getCache(record.getCacheType());

        if (lastDbSyncTime == 0) {
            refreshCacheStats();
        }

        cache.currentSize += record.getDataSize();

        if (cache.currentSize > cache.maxSize)
            trimCache(cache);
    }

    private void trimCache(ContentCache cache) {
        if (cache.size() <= MIN_ITEMS_IN_CACHE)
            return;

        ArrayList<ContentDataCacheRecord> recordsToDelete = new ArrayList<>();
        Cursor c = null;
        try {
            c = context.getContentResolver().query(ContentDataCacheEntry.CONTENT_URI,
                    ContentDataCacheEntry.DEFAULT_PROJECTION,
                    TYPE + "=?",
                    new String[]{String.valueOf(cache.getCacheType().ordinal())},
                    LAST_ACCESS + " ASC");

            while (c != null && c.moveToNext() && cache.currentSize > cache.maxSize && cache.size() > MIN_ITEMS_IN_CACHE) {
                ContentDataCacheRecord toDelete = ContentDataCacheRecord.getCacheRecordFromCursor(c);
                cache.remove(toDelete.getRemoteUri());
                recordsToDelete.add(toDelete);
                cache.currentSize -= toDelete.getDataSize();
            }
        } finally {
            if (c != null)
                c.close();
        }

        Batch batch = batchProvider.get();
        final File cacheContentDirectory = getContentDirectory(cache.getCacheType());
        final File contentDirectory = getContentDirectory();
        for (ContentDataCacheRecord record : recordsToDelete) {
            ContentProviderOperation op = ContentProviderOperation.newDelete(
                    ContentUris.withAppendedId(ContentDataCacheEntry.CONTENT_URI, record.id))
                    .build();

            batch.add(op);
            try {
                Ln.d("Recycling file " + record.getLocalUri());
                File f = record.getLocalUriAsFile();
                if (f != null && f.exists()) {
                    File parent = f.getParentFile();
                    if (parent == null || cacheContentDirectory.equals(parent) || contentDirectory.equals(parent)) {
                        // Do not delete the root directory for content or the parent directory for specific
                        // content type, e.g. AVATAR, MEDIA
                        f.delete();
                    } else {
                        FileUtils.deleteRecursive(parent);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        batch.apply();
    }

    private void refreshCacheStats() {
        // Synchronize here just to make sure only one gets in at a time. Don't sync on tasklock
        // for this because that blocks the ui thread.
        dbLock.lock();
        try {
            Cursor c = null;
            try {
                c = context.getContentResolver().query(vw_ContentCacheSize.CONTENT_URI,
                        vw_ContentCacheSize.DEFAULT_PROJECTION, null, null, null);

                while (c != null && c.moveToNext()) {
                    Cache cacheType = Cache.values()[c.getInt(vw_ContentCacheSize.TYPE.ordinal())];
                    getCache(cacheType).currentSize = c.getInt(vw_ContentCacheSize.DATA_SIZE.ordinal());
                }
            } finally {
                if (c != null)
                    c.close();
            }
        } finally {
            dbLock.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(UIBackgroundTransition event) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                writeCaches();
                if (!hasScannedForOrphans) {
                    removeOrphans();
                    hasScannedForOrphans = true;
                }
            }
        });
    }

    //exposed for testing
    public void removeOrphans() {
        Set<String> dbFileList = new HashSet<>();
        Cursor c = null;
        try {
            c = context.getContentResolver().query(ContentDataCacheEntry.CONTENT_URI, new String[]{ContentDataCacheEntry.DATA_LOCAL_URI.name()}, null, null, null);
            String path;
            while (c != null && c.moveToNext()) {
                path = c.getString(0);
                if (TextUtils.isEmpty(path))
                    continue;

                dbFileList.add(getRelativeContentPath(URI.create(path)));
            }
        } catch (Exception e) {
            Ln.e(e, "Failed scanning for orphaned files");
        } finally {
            if (c != null)
                c.close();
        }

        deleteFilesNotInList(dbFileList, getContentDirectory());
    }

    private void deleteFilesNotInList(Set<String> dbFileList, File fileOrDir) {
        if (!fileOrDir.exists())
            return;

        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            for (File file : files) {
                // recurse
                deleteFilesNotInList(dbFileList, file);
            }
        } else if (fileOrDir.isFile()) {
            String relativePath = getRelativeContentPath(fileOrDir.getAbsolutePath());
            if (!dbFileList.contains(relativePath)) {
                Ln.d("Removing orphaned file " + fileOrDir);
                fileOrDir.delete();
                deleteDirectoryIfEmpty(fileOrDir.getParentFile());
            }
        }
    }

    private void deleteDirectoryIfEmpty(File dir) {
        if (dir.isDirectory()) {
            if (dir.listFiles().length == 0) {
                Ln.d("Removing empty directory " + dir);
                dir.delete();
            }
        }
    }

    private String getRelativeContentPath(URI fileUri) {
        return getRelativeContentPath(new File(fileUri).getAbsolutePath());
    }

    private String getRelativeContentPath(String fullPath) {
        try {
            return fullPath.substring(fullPath.indexOf("/" + CONTENTDIR + "/"));
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LogoutEvent event) {
        clear();
    }

    private void writeCaches() {
        dbLock.lock();
        try {
            for (Cache cache : Cache.values()) {
                writeCache(getCache(cache));
            }

            lastDbSyncTime = System.currentTimeMillis();
            refreshCacheStats();
        } finally {
            dbLock.unlock();
        }
    }

    private void writeCache(ContentCache cache) {
        Batch batch = batchProvider.get();
        try {
            for (ContentDataCacheRecord record : cache.values()) {
                if (record.getLastAccessTime() > lastDbSyncTime) {
                    batch.add(record.getUpdate());
                }
            }
        } catch (Exception e) {
            Ln.e(e, "Failed writing content cache to db");
        }
        batch.apply();
    }

    public void touchCacheRecord(Uri uri, String uuidOrEmail, String fileName, BitmapProvider.BitmapType type) {
        Cache cacheType = getCacheType(type);
        if (Cache.AVATAR != cacheType || fetchesInProgress.containsKey(uri)) {
            return;
        }

        ContentCache cache = getCache(cacheType);
        ContentDataCacheRecord record = cache.get(uri);
        if (record != null) {
            if (!record.isStale() && record.hasValidLocalContent()) {
                record.setLastAccessTime(System.currentTimeMillis());
                return;
            }
        }

        if (!fetchesInProgress.containsKey(uri)) {
            CacheRecordRequestParameters parameters = new CacheRecordRequestParameters(uri, uuidOrEmail, fileName, RETENTION_MODE_STANDARD, type);
            FetchContentTask task = new FetchContentTask(record, cacheType, parameters, null, null, false, true);
            task.setFuture(remoteContentRefreshExecutor.submit(task));
            fetchesInProgress.put(uri, task);
        }
    }

    public boolean isIdle() {
        for (FetchContentTask task : fetchesInProgress.values()) {
            Ln.d("ContentManager task in progress: " + task.cacheType + " " + task.uri);
        }

        return fetchesInProgress.size() == 0;
    }

    public File getContentFile(Cache cacheType, Uri uri, String filename) {
        filename = getContentFileName(cacheType, uri, filename);
        return new File(getContentDirectory(cacheType, uri), filename);
    }

    public String getContentFileName(Cache cacheType, Uri uri, String filename) {
        return FileUtils.getContentFileName(context, cacheType, uri, filename);
    }

    public boolean isThumbnailFile(File file) {
        try {
            return file != null && file.getCanonicalPath().startsWith(getContentDirectory(ConversationContract.ContentDataCacheEntry.Cache.THUMBNAIL).getCanonicalPath());
        } catch (IOException e) {
            Ln.i("Failed checking file for thumbnailness " + file);
            return false;
        }
    }

    public File getContentDirectory(Cache cacheType, Uri uri) {
        File dir = new File(getContentDirectory(cacheType), Strings.md5(uri.toString()));
        dir.mkdirs();
        return dir;
    }

    static public File getContentDirectory(Context context) {
        return new File(context.getFilesDir(), CONTENTDIR);
    }

    static public File getContentDirectory(Context context, Cache cacheType) {
        return new File(getContentDirectory(context), cacheType.toString());
    }

    public File getContentDirectory(Cache cacheType) {
        return getContentDirectory(context, cacheType);
    }

    public File getContentDirectory() {
        return getContentDirectory(context);
    }

    private ContentCache getCache(Cache cacheType) {
        switch (cacheType) {
            case THUMBNAIL:
                return thumbnailCache;
            case AVATAR:
                return avatarCache;
            case MEDIA:
            default:
                return contentCache;
        }
    }

    public boolean isLoading(Uri uri) {
        return fetchesInProgress.get(uri) != null;
    }

    public void registerListener(ContentListener listener) {
        contentListeners.add(new WeakReference<>(listener));
    }

    private ArrayList<WeakReference<ContentListener>> contentListeners = new ArrayList<>();

    private void createDirectories() {
        if (!getContentDirectory(Cache.AVATAR).isDirectory()) {
            clear();
        }
        for (Cache cache : Cache.values()) {
            if (!getContentDirectory(cache).isDirectory()) {
                getContentDirectory(cache).mkdirs();
            }
        }
    }

    public interface ContentListener {
        void onFetchStart(Uri uri);

        void onFetchComplete(ContentDataCacheRecord cdr);

        void onFetchAvatarUrlComplete(CacheRecordRequestParameters parameters);

        void onRemoteBitmapUpdated(Uri remoteUri, BitmapProvider.BitmapType bitmapType);
    }

    private void callContentListeners(Action<ContentListener> action) {
        ArrayList<WeakReference<ContentListener>> listenersCopy = new ArrayList<>();
        listenersCopy.addAll(contentListeners);

        for (WeakReference<ContentListener> listenerReference : listenersCopy) {
            if (listenerReference.get() == null) {
                contentListeners.remove(listenerReference);
            } else {
                action.call(listenerReference.get());
            }
        }
    }

    //overridden for testing
    public int getUriLoadCount(Uri uri) {
        throw new RuntimeException("Not Implemented");
    }

}
