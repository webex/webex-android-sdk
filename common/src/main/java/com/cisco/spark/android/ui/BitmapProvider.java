package com.cisco.spark.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.LruCache;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.R;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.BitmapUtils;
import com.cisco.spark.android.util.BitmapUtils.ScaleProvider.ScaleType;
import com.cisco.spark.android.util.CircleTransform;
import com.cisco.spark.android.util.CompletedFuture;
import com.cisco.spark.android.util.LifoBlockingDeque;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.RoundedCornerTransform;
import com.cisco.spark.android.util.UIUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

@Singleton
public class BitmapProvider implements ContentManager.ContentListener {
    public static final int LARGE_DEFAULT_PIXELSIZE = 1024 * 1024;
    private static final int BYTES_PER_PIXEL_RGB8 = 4;
    private static final int BYTES_PER_PIXEL_565 = 2;

    // If the image takes more than FADE_THRESHOLD_LOAD_TIME ms to load, fade it in. This threshold
    // is to prevent fades in situations like AsyncImageView reuse in ListViews where we don't want
    // fades every time the user scrolls
    private static final int FADE_THRESHOLD_LOAD_TIME = 50;

    private static final int THREAD_POOL_SIZE = 5;

    ArrayList<BitmapProviderListener> implicitListeners = new ArrayList<>();

    NaturalLog ln = Ln.get("$BITMAP");
    private static final List<BitmapProviderListener> EMPTY_LISTENER_LIST = Collections.unmodifiableList(new ArrayList<BitmapProviderListener>());

    private final Map<Uri, Integer> failedToParseBitmapBlacklist = new HashMap<>();

    // NOTE these are stored as attributes in styles.xml, any changes here should be reflected there
    public enum BitmapType {
        THUMBNAIL,
        VIDEO_THUMBNAIL,
        LARGE,
        AVATAR,
        AVATAR_READRECEIPT,
        AVATAR_NOTIFICATION,
        MULTIPAGE_DOCUMENT,
        AVATAR_CALL_PARTICIPANT,
        SIDE_NAV_AVATAR,
        CHIP,
        SETTINGS_AVATAR,
        AVATAR_LARGE,
        AVATAR_ROOM_DETAILS_DIALOG,
        IMAGE_URI,
        AVATAR_EDIT;

        public int maxH, maxW, maxPixels;
        public Bitmap.Config rgbConfig = Bitmap.Config.RGB_565;

        public void setSquareDims(Context context, int resDim) {
            maxH = (int) context.getResources().getDimension(resDim);
            maxW = maxH;
            maxPixels = maxH * maxW;
        }

        public void setDims(Context context, int resH, int resW) {
            maxH = (int) context.getResources().getDimension(resH);
            maxW = (int) context.getResources().getDimension(resW);
            maxPixels = maxH * maxW;
        }

        public ScaleType getScaleType() {
            if (this == THUMBNAIL) return ScaleType.SCALE_BY_SCREEN_DENSITY;
            if (this == LARGE) return ScaleType.DOWNSCALE_ONLY;
            return ScaleType.ENLARGE_IF_NEEDED;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {RETENTION_MODE_STANDARD, RETENTION_MODE_DETACHED})
    public @interface RetentionMode {
    }

    public static final int RETENTION_MODE_STANDARD = 0;
    /**
     * Use RETENTION_MODE_DETACHED to let the calling code manage the lifecycle of the bitmap. It
     * will not be recycled and it will not be shared with other BitmapProvider clients. The caller
     * should recycle the bitmap when finished with it.
     */
    public static final int RETENTION_MODE_DETACHED = 1;

    private final LoggingLock taskLock;

    private int executorIndex = 0;

    ExecutorService executor = new ThreadPoolExecutor(
            THREAD_POOL_SIZE, THREAD_POOL_SIZE, 30,
            TimeUnit.SECONDS,
            new LifoBlockingDeque<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread ret = new Thread(runnable);
                    ret.setName("BitmapProvider Executor " + executorIndex++);
                    ret.setDaemon(true);
                    return ret;
                }
            });

    private class BitmapEntry {
        Bitmap bitmap;
        BitmapType type;
        String lastModified;
        Uri localUri;
        boolean processingFailed;

        BitmapEntry(Uri localUri, BitmapType type, String remoteLastModified) {
            this.type = type;
            this.lastModified = remoteLastModified;
            this.localUri = localUri;
        }

        public void processBitmap() throws IOException, URISyntaxException {
            try {
                this.bitmap = fileToBitmap(localUri, type);
            } catch (Exception e) {
                ln.e(e, "Failed processing bitmap ");
            }

            if (this.bitmap == null) {
                processingFailed = true;
                MimeUtils.ContentType contentType = MimeUtils.getContentTypeByFilename(localUri.getLastPathSegment());
                // If we get here with an IMAGE content type it's probably a corrupt bitmap.
                if (contentType != MimeUtils.ContentType.IMAGE)
                    this.bitmap = BitmapFactory.decodeResource(context.getResources(), contentType.drawableResource);
            }
        }
    }

    public void registerListener(BitmapProviderListener listener, Uri uri) {
        if (uri == null)
            return;

        synchronized (bitmapListenerMap) {
            ArrayList<WeakReference<BitmapProviderListener>> listenersForUri = bitmapListenerMap.get(uri);
            if (listenersForUri == null) {
                listenersForUri = new ArrayList<>();
                bitmapListenerMap.put(uri, listenersForUri);
            }
            if (!isListenerRegistered(listener, uri))
                listenersForUri.add(new WeakReference<>(listener));
        }
    }

    // H@CK The implicit listener has no reference to it so its WeakReference gets cleaned up too fast.
    public void registerImplicitListener(BitmapProviderListener listener, Uri uri) {
        implicitListeners.add(listener);
        registerListener(listener, uri);
    }

    public boolean isListenerRegistered(BitmapProviderListener listener, Uri uri) {
        if (uri == null)
            return false;

        synchronized (bitmapListenerMap) {
            return getListenersForUri(uri).contains(listener);
        }
    }

    public void unregisterListener(BitmapProviderListener listenerToUnregister, Uri uri, boolean forceUnregister) {
        implicitListeners.remove(listenerToUnregister);

        if (uri == null)
            return;

        if (uri.toString().contains("avatar") && !forceUnregister)
            return;

        synchronized (bitmapListenerMap) {
            ArrayList<WeakReference<BitmapProviderListener>> listenersForUri = bitmapListenerMap.get(uri);
            if (listenersForUri == null)
                return;

            for (int i = listenersForUri.size() - 1; i >= 0; i--) {
                if (listenersForUri.get(i).get() == null || listenersForUri.get(i).get() == listenerToUnregister)
                    listenersForUri.remove(i);
            }
            if (listenersForUri.isEmpty())
                bitmapListenerMap.remove(uri);
        }
    }

    public List<BitmapProviderListener> getListenersForUri(Uri uri) {
        if (uri == null)
            return EMPTY_LISTENER_LIST;

        synchronized (bitmapListenerMap) {
            ArrayList<WeakReference<BitmapProviderListener>> listenersForUri = bitmapListenerMap.get(uri);
            if (listenersForUri == null || listenersForUri.isEmpty())
                return EMPTY_LISTENER_LIST;

            ArrayList<BitmapProviderListener> ret = new ArrayList<>();
            for (int i = listenersForUri.size() - 1; i >= 0; i--) {
                BitmapProviderListener listener = listenersForUri.get(i).get();
                if (listener == null)
                    listenersForUri.remove(i);
                else
                    ret.add(listener);
            }
            return ret;
        }
    }

    public interface BitmapProviderListener {
        /**
         * Called on the main thread when a bitmap is ready or updated.
         *
         * @param uri    the bitmap's remote Uri
         * @param type   One of BitmapType enum
         * @param bitmap the bitmap
         */
        void onBitmapLoaded(Uri uri, BitmapType type, Bitmap bitmap, boolean shouldFade);
    }

    public interface Transformation {
        Bitmap transform(Bitmap bitmap);
    }

    private Context context;
    private final DeviceRegistration deviceRegistration;
    private ContentManager contentManager;
    private final ConcurrentHashMap<Uri, Future<Bitmap>> tasksInProgress = new ConcurrentHashMap<>();
    private Handler mainLooperHandler = new Handler(Looper.getMainLooper());
    private ConcurrentHashMap<Uri, ArrayList<WeakReference<BitmapProviderListener>>> bitmapListenerMap = new ConcurrentHashMap<>();
    private final RoundedCornerTransform roundedCornerTransformBig;
    private final CircleTransform circleTransform;

    // main cache
    private BitmapLruCache bitmapLruCache;
    // second-chance bucket for avatars that get evicted from the main cache
    private BitmapLruCache avatarLruCache;

    private int maxCacheSize;
    private static final int MAX_AVATAR_CACHE_SIZE = 1 * 1024 * 1024;

    @Inject
    public BitmapProvider(Context context, ContentManager contentManager, EventBus bus, DeviceRegistration deviceRegistration) {
        this.context = context;
        this.deviceRegistration = deviceRegistration;
        this.taskLock = new LoggingLock(BuildConfig.DEBUG, "BitmapProvider Tasks");
        this.contentManager = contentManager;
        this.roundedCornerTransformBig = new RoundedCornerTransform(context, 3);
        this.circleTransform = new CircleTransform(context);

        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        try {
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory());

            // Use 1/5th of the available heap for this memory cache, up to 24 megs.
            maxCacheSize = Math.min(24 * 1024 * 1024, maxMemory / 5);

        } catch (Exception e) {
            // Some devices don't support this call, 4 megs for them
            maxCacheSize = 4 * 1024 * 1024;
        }

        ln.i("Max Bitmap Cache size " + maxCacheSize);

        bitmapLruCache = new BitmapLruCache(maxCacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, Uri key, BitmapEntry oldValue, BitmapEntry newValue) {
                if (ContentManager.getCacheType(oldValue.type) == ConversationContract.ContentDataCacheEntry.Cache.AVATAR) {
                    // Move avatars to the second-chance cache
                    avatarLruCache.put(key, oldValue);
                    ln.d("Bitmap moved to second-chance cache " + oldValue.localUri);
                } else {
                    super.entryRemoved(evicted, key, oldValue, newValue);
                }
            }
        };
        avatarLruCache = new BitmapLruCache(MAX_AVATAR_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, Uri key, BitmapEntry oldValue, BitmapEntry newValue) {
                // If the entry has been promoted back into the main cache we're done here
                if (bitmapLruCache.snapshot().containsKey(key))
                    return;

                super.entryRemoved(evicted, key, oldValue, newValue);
            }
        };

        refreshDims();

        contentManager.registerListener(this);
        bus.register(this);
    }

    private int getDimenDpValue(int dimenId) {
        return (int) (context.getResources().getDimension(dimenId) / context.getResources().getDisplayMetrics().density);
    }

    private boolean bitmapsDiffer(BitmapEntry rhs, BitmapEntry lhs) {
        if (rhs == null || lhs == null)
            return true;
        if (rhs.bitmap == null || lhs.bitmap == null)
            return true;

        return rhs.bitmap != lhs.bitmap;
    }

    private void refreshDims() {
        // Ensure the max size of large images allows 6 to fit.
        BitmapType.LARGE.maxPixels = Math.min(LARGE_DEFAULT_PIXELSIZE, maxCacheSize / 6 / BYTES_PER_PIXEL_RGB8);
        BitmapType.LARGE.rgbConfig = Bitmap.Config.ARGB_8888;

        // Ensure the max size of transcoded pages allows 12 to fit.
        BitmapType.MULTIPAGE_DOCUMENT.maxPixels = maxCacheSize / 12 / BYTES_PER_PIXEL_565;

        ln.i("Max bitmap size in pixels : " + BitmapType.LARGE.maxPixels);

        BitmapType.THUMBNAIL.setDims(context, R.dimen.thumbnail_height_max, R.dimen.thumbnail_width_max);
        BitmapType.VIDEO_THUMBNAIL.maxH = BitmapType.THUMBNAIL.maxH;
        BitmapType.VIDEO_THUMBNAIL.maxW = BitmapType.THUMBNAIL.maxW;
        BitmapType.VIDEO_THUMBNAIL.maxPixels = BitmapType.THUMBNAIL.maxPixels;

        BitmapType.IMAGE_URI.setSquareDims(context, R.dimen.image_uri_size);

        BitmapType.AVATAR_LARGE.setSquareDims(context, R.dimen.avatar_large_dim);
        BitmapType.SETTINGS_AVATAR.setSquareDims(context, R.dimen.preferences_avatar_size);
        BitmapType.AVATAR_EDIT.setSquareDims(context, R.dimen.avatar_edit_size);
        BitmapType.SIDE_NAV_AVATAR.setSquareDims(context, R.dimen.side_nav_avatar_size);
        BitmapType.AVATAR_CALL_PARTICIPANT.setDims(context, R.dimen.avatar_callparticipant_height, R.dimen.avatar_callparticipant_width);
        BitmapType.AVATAR_NOTIFICATION.setDims(context, android.R.dimen.notification_large_icon_height, android.R.dimen.notification_large_icon_width);
        BitmapType.AVATAR_ROOM_DETAILS_DIALOG.setSquareDims(context, R.dimen.room_details_dialog_avatar);
        BitmapType.AVATAR.setSquareDims(context, R.dimen.small_avatar_size);
        BitmapType.CHIP.setSquareDims(context, R.dimen.chip_height);
        BitmapType.AVATAR_READRECEIPT.setSquareDims(context, R.dimen.avatar_readreceipt_size);
    }

    public void clear() {
        taskLock.lock();
        try {
            tasksInProgress.clear();
        } catch (Exception e) {
            ln.w(e, "Failed clearing tasksInProgress");
        } finally {
            taskLock.unlock();
        }

        try {
            bitmapLruCache.evictAll();
        } catch (Exception e) {
            ln.w(e, "Failed clearing bitmapLruCache");
        }

        try {
            avatarLruCache.evictAll();
        } catch (Exception e) {
            ln.w(e, "Failed clearing avatarLruCache");
        }
    }

    public boolean isIdle() {
        for (Uri remoteUri : tasksInProgress.keySet()) {
            ln.d("BitmapProvider task in progress: " + remoteUri);
        }

        return tasksInProgress.size() == 0;
    }

    /**
     * Recursive function. Get the best available bitmap currently in memory for the given Uri.
     *
     * @param uri              The uri
     * @param type             The largest acceptable type
     * @param requestIfMissing If the requested bitmap is not available request it asynchronously
     * @param fileName         A friendly filename to use if the image must be downloaded (ignored
     *                         if requestIfMissing=false)
     * @return A bitmap from the LruCache. The bitmap's LRU record will be bumped to the top.
     */
    public Bitmap getBestAvailableNoWait(Uri uri, BitmapType type, boolean requestIfMissing, String fileName) {
        if (uri == null) {
            return null;
        }

        Uri bitmapKeyUri = getBitmapKeyUri(uri, type);
        BitmapEntry entry = bitmapLruCache.get(bitmapKeyUri);

        if (entry != null && entry.bitmap != null) {
            return entry.bitmap;
        }

        if (requestIfMissing) {
            getBitmap(uri, type, fileName, null);
        }

        Bitmap bitmap = getBiggerAvailableNoWait(bitmapKeyUri, type);
        if (bitmap == null) {
            bitmap = getSmallerAvailableNoWait(bitmapKeyUri, type);
        }

        return bitmap;
    }

    private Bitmap getBiggerAvailableNoWait(Uri bitmapKeyUri, BitmapType type) {
        BitmapEntry entry = bitmapLruCache.get(bitmapKeyUri);

        if (entry != null && entry.bitmap != null) {
            Ln.d("getBiggerAvailableNoWait, bitmapKeyUri: " + bitmapKeyUri);
            return entry.bitmap;
        }

        switch (type) {
            // Grouped by transformation. These have none
            case THUMBNAIL:
            case VIDEO_THUMBNAIL:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.MULTIPAGE_DOCUMENT);
            case MULTIPAGE_DOCUMENT:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.LARGE);
            case LARGE:
                return null;

            // Grouped by transformation. Circular
            case AVATAR_READRECEIPT:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR);
            case AVATAR:
            case CHIP:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_ROOM_DETAILS_DIALOG);
            case AVATAR_ROOM_DETAILS_DIALOG:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_NOTIFICATION);
            case AVATAR_NOTIFICATION:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_CALL_PARTICIPANT);
            case AVATAR_CALL_PARTICIPANT:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.SIDE_NAV_AVATAR);
            case SIDE_NAV_AVATAR:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_EDIT);
            case AVATAR_EDIT:
                return getBiggerAvailableNoWait(bitmapKeyUri, BitmapType.SETTINGS_AVATAR);
            case SETTINGS_AVATAR:
                return null;
        }

        return null;
    }

    private Bitmap getSmallerAvailableNoWait(Uri bitmapKeyUri, BitmapType type) {
        BitmapEntry entry = bitmapLruCache.get(bitmapKeyUri);

        if (entry != null && entry.bitmap != null) {
            Ln.d("getSmallerAvailableNoWait, bitmapKeyUri: " + bitmapKeyUri);
            return entry.bitmap;
        }

        switch (type) {
            // Grouped by transformation. These have none
            case LARGE:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.MULTIPAGE_DOCUMENT);
            case MULTIPAGE_DOCUMENT:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.THUMBNAIL);
            case THUMBNAIL:
            case VIDEO_THUMBNAIL:
                return null;

            // Grouped by transformation. Circular
            case SETTINGS_AVATAR:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_EDIT);
            case AVATAR_EDIT:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.SIDE_NAV_AVATAR);
            case SIDE_NAV_AVATAR:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_CALL_PARTICIPANT);
            case AVATAR_CALL_PARTICIPANT:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_NOTIFICATION);
            case AVATAR_NOTIFICATION:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_ROOM_DETAILS_DIALOG);
            case AVATAR_ROOM_DETAILS_DIALOG:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR);
            case AVATAR:
            case CHIP:
                return getSmallerAvailableNoWait(bitmapKeyUri, BitmapType.AVATAR_READRECEIPT);
        }

        // TODO check the ContentManager for existing files we can render asynchronously
        return null;
    }

    public Future<Bitmap> getBitmap(Uri uri, BitmapType type, String filename, final Action<Bitmap> callback) {
        return getBitmap(uri, null, type, filename, RETENTION_MODE_STANDARD, callback);
    }

    public Future<Bitmap> getBitmap(Uri uri, BitmapType type, String filename, @RetentionMode int retentionMode, final Action<Bitmap> callback) {
        return getBitmap(uri, null, type, filename, retentionMode, callback);
    }

    public Future<Bitmap> getBitmap(SecureContentReference secureContentReference, BitmapType type, String filename, final Action<Bitmap> callback) {
        return getBitmap(Uri.parse(secureContentReference.getLoc()), secureContentReference, type, filename, RETENTION_MODE_STANDARD, callback);
    }

    public Future<Bitmap> getBitmap(ContentReference contentReference, BitmapType type, String filename, @RetentionMode int retentionMode, final Action<Bitmap> callback) {
        return getBitmap(contentReference.getUrlOrSecureLocation(),
                contentReference.getSecureContentReference(),
                type,
                filename,
                retentionMode,
                callback);
    }

    public Future<Bitmap> getBitmap(Uri uri, String uuidOrEmail, BitmapType type, String filename) {
        return getBitmap(uri, null, type, filename, uuidOrEmail, RETENTION_MODE_STANDARD, null);
    }

    public Future<Bitmap> getBitmap(Uri uri,
                                    SecureContentReference secureContentReference,
                                    BitmapType type,
                                    String filename,
                                    @RetentionMode int retentionMode,
                                    final Action<Bitmap> callback) {
        return getBitmap(uri, secureContentReference, type, filename, null, retentionMode, callback);
    }


    /**
     * Retrieve a bitmap from a Uri. If the bitmap is not in memory, the file will be requested from
     * the ContentManager and the bitmap will be built from that. The ContentManager will download
     * the file if needed. <p/> If the Bitmap is already in memory and a callback is provided, the
     * callback occurs on the calling thread. Otherwise the callback occurs on the main thread. <p/>
     * Bitmaps will be sized and transformed (e.g. rounded corners) depending on the BitmapType
     * requested.
     *
     * @param uri           Uri of the bitmap. This should be an http(s), file, or content uri.
     * @param type          One of BitmapType enum
     * @param filename      The filename. This is used to name the file should it require
     *                      downloading.
     * @param uuidOrEmail   When user avatar is not exist will use this value to request real
     *                      avatar URI.
     * @param retentionMode Customize how the Bitmap will be managed by {@link BitmapProvider}
     * @param callback      Optional callback for when the bitmap processing is finished. The
     *                      callback will be called on the main thread.
     * @return A Future Bitmap, which may already be done if the bitmap was already in memory.
     * Bitmaps may be updated and recycled if the source content changes. BitmapProviderListeners
     * will be notified when that happens. AsyncImageViews handle this transparently. Other bitmap
     * clients should handle this case and pass the DETACHED flag.
     */
    private Future<Bitmap> getBitmap(final Uri uri,
                                     SecureContentReference secureContentReference,
                                     final BitmapType type,
                                     String filename,
                                     String uuidOrEmail,
                                     @RetentionMode int retentionMode,
                                     final Action<Bitmap> callback) {
        if (uri == null)
            return null;

        final boolean detached = retentionMode == RETENTION_MODE_DETACHED;

        Future<Bitmap> ret = null;
        final Uri keyUri = getBitmapKeyUri(uri, type);

        // Pull from the bitmap cache. If it's not there check the second-chance cache.
        // If the second-chance cache gets a hit, promote the entry back into the main cache.
        BitmapEntry bitmapentry = bitmapLruCache.get(keyUri);
        if (bitmapentry == null) {
            bitmapentry = avatarLruCache.get(keyUri);
            if (bitmapentry != null) {
                ln.d("Bitmap returned to main cache. " + uri);
                bitmapLruCache.put(keyUri, bitmapentry);
            }
        }

        // If the cached bitmap has been recycled out from under us, null it out.
        if (bitmapentry != null && bitmapentry.bitmap != null && bitmapentry.bitmap.isRecycled()) {
            ln.d("bitmap was recycled " + uri);
            bitmapentry.bitmap = null;
            bitmapLruCache.remove(keyUri);
            avatarLruCache.remove(keyUri);
        }

        // If we have a valid bitmap, that's our return value.
        if (bitmapentry != null && bitmapentry.bitmap != null && !detached) {
            contentManager.touchCacheRecord(uri, null);
            ret = new CompletedFuture<>(bitmapentry.bitmap);

            // Shortcut for the happy case
            // Call the callback with the bitmap if it's ready
            if (callback != null) {
                ln.d("calling back with bitmap " + uri);
                try {
                    callback.call(ret.get());
                } catch (Exception e) {
                    ln.e(e);
                }
            }
            return ret;
        }

        if (callback != null) {
            final BitmapProviderListener listener = new BitmapProviderListener() {
                @Override
                public void onBitmapLoaded(Uri uri, BitmapType type, Bitmap bitmap, boolean shouldFade) {
                    Ln.v("onBitmapLoaded, unregister callback on uri: %s (%08x)", uri, this.hashCode());
                    unregisterListener(this, uri, false);
                    if (bitmap != null && bitmap.isRecycled()) {
                        bitmap = null;
                    }
                    Ln.v("callback from implicit listener with bitmap (%08x)", this.hashCode());
                    callback.call(bitmap);
                }
            };
            Ln.d("Registering implicit listener for %s (%08x)", uri, listener.hashCode());
            registerImplicitListener(listener, uri);
        }

        //TODO keep the main thread from hitting this lock
        taskLock.lock();
        try {
            // ... Otherwise see if we're already waiting for this bitmap
            ln.v("cache miss " + uri);
            if (!detached)
                ret = tasksInProgress.get(keyUri);

            // If it's done make sure it was successful
            if (ret != null) {
                ln.v("task already in progress " + uri);
                if (ret.isDone()) {
                    try {
                        ret.get();
                    } catch (Exception e) {
                        ln.e(e, "failed future.get");
                        ret = null;
                    }
                }
            }

            // Call the callback with the bitmap if it's ready
            if (ret != null && ret.isDone() && callback != null) {
                ln.d("calling back with bitmap " + uri);
                notifyListeners(uri, type, ret.get(), false);
            }

            if (ret == null) {
                ret = executor.submit(new GetBitmapTask(uri, uuidOrEmail, secureContentReference, filename, type, retentionMode));
                if (!detached)
                    tasksInProgress.put(keyUri, ret);
            }
        } catch (Exception e) {
            ln.e(e, "Failed getting bitmap");
        } finally {
            taskLock.unlock();
        }
        return ret;
    }

    protected class GetBitmapTask implements Callable<Bitmap> {
        private final Uri remoteUri;
        private final String uuidOrEmail;
        private final BitmapType type;
        private final SecureContentReference secureContentReference;
        private String filename;
        private long startTime, createTime;
        private int retentionMode;

        GetBitmapTask(Uri remoteUri, String uuidOrEmail, SecureContentReference secureContentReference, String filename, BitmapType type, @RetentionMode int retentionMode) {
            this.remoteUri = remoteUri;
            this.uuidOrEmail = uuidOrEmail;
            this.secureContentReference = secureContentReference;
            this.filename = filename;
            this.type = type;
            this.retentionMode = retentionMode;

            if (TextUtils.isEmpty(filename)) {
                this.filename = context.getString(R.string.generic_image_filename);
            }

            createTime = System.currentTimeMillis();
        }

        @Override
        public Bitmap call() throws Exception {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            startTime = System.currentTimeMillis();

            final Uri keyUri = getBitmapKeyUri(remoteUri, type);

            ln.v("Starting task to get bitmap " + keyUri);

            try {
                if (isBlacklisted(remoteUri)) {
                    Ln.d("Skipping blacklisted bitmap " + remoteUri);
                    return null;
                }

                final BitmapEntry entry = getBitmapEntry(keyUri);

                if (entry == null) {
                    notifyListeners(remoteUri, type, null, false);
                    return null;
                }

                taskLock.lock();
                try {
                    if (retentionMode != RETENTION_MODE_DETACHED)
                        tasksInProgress.remove(keyUri);

                    notifyListeners(remoteUri, entry, (System.currentTimeMillis() - startTime) > FADE_THRESHOLD_LOAD_TIME);

                    if (entry != null)
                        return entry.bitmap;
                } finally {
                    taskLock.unlock();
                }
            } catch (RetrofitError e) {
                if (e.getKind() != RetrofitError.Kind.NETWORK)
                    ln.e(false, e);
                else
                    ln.v(e);
                notifyListeners(remoteUri, null, false);
            } catch (Exception e) {
                ln.e(e, "Caught Exception in GetBitmapTask");
                notifyListeners(remoteUri, null, false);
            } finally {
                taskLock.lock();
                try {
                    if (retentionMode != RETENTION_MODE_DETACHED)
                        tasksInProgress.remove(keyUri);
                } finally {
                    taskLock.unlock();
                }
                if (startTime - createTime > 200 || System.currentTimeMillis() - startTime > 1000)
                    ln.d("$PERF Bitmap task waited " + (startTime - createTime) + " ms and worked for " + (System.currentTimeMillis() - startTime) + " ms. " + remoteUri);
            }
            return null;
        }

        private BitmapEntry getBitmapEntry(final Uri keyUri) throws ExecutionException, InterruptedException, IOException, URISyntaxException {
            ContentDataCacheRecord record;
            File localFile;

            if ("file".equals(remoteUri.getScheme())) {
                localFile = new File(remoteUri.getPath());
                record = new ContentDataCacheRecord();
                record.setLocalUri(remoteUri);
            } else {
                ln.v("getting ContentDataCacheRecord for " + remoteUri);
                ContentManager.CacheRecordRequestParameters parameters = new ContentManager.CacheRecordRequestParameters(remoteUri, uuidOrEmail, filename, retentionMode, type);
                Future<ContentDataCacheRecord> futureRecord
                        = contentManager.getCacheRecord(parameters, secureContentReference, null);

                record = futureRecord.get();

                if (record == null || record.getLocalUri() == null) {
                    ln.v("Failed getting ContentDataCacheRecord for " + remoteUri);
                    return null;
                }

                localFile = record.getLocalUriAsFile();
                ln.v("got ContentDataCacheRecord with file " + localFile + " for " + remoteUri);
            }

            BitmapEntry entry = null;

            if (retentionMode == RETENTION_MODE_STANDARD)
                entry = bitmapLruCache.get(keyUri);

            if (entry == null) {
                entry = new BitmapEntry(record.getLocalUri(), type, record.getRemoteLastModifiedTime());

                if (retentionMode != RETENTION_MODE_STANDARD || !getListenersForUri(remoteUri).isEmpty()) {
                    entry.processBitmap();
                    if (entry.bitmap != null && retentionMode == RETENTION_MODE_STANDARD) {
                        bitmapLruCache.put(keyUri, entry);
                        ln.d("Added bitmap of size " + entry.bitmap.getByteCount() + ". bitmap cache size now " + bitmapLruCache.size() + " of " + bitmapLruCache.maxSize());
                    }
                } else {
                    // whoever requested this bitmap no longer cares about it, don't bother inflating it
                    Ln.d("No listeners for " + remoteUri);
                }
            } else {
                ln.v("Updating existing bitmap in lrucache for " + remoteUri);
                Bitmap bitmap = fileToBitmap(localFile, type);
                if (bitmap != null && !bitmap.sameAs(entry.bitmap)) {
                    final Bitmap oldBitmap = entry.bitmap;
                    entry.bitmap = bitmap;

                    // Give the main thread time to update existing views before recycling the bitmap
                    mainLooperHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (oldBitmap != null && !oldBitmap.isRecycled()) {
                                ln.d("Recycling old bitmap " + keyUri);
                                oldBitmap.recycle();
                            }
                        }
                    }, 1000);
                } else {
                    if (bitmap != null)
                        bitmap.recycle();
                }
            }

            // This can happen if a bitmap is corrupt, e.g. if the download was truncated
            if (entry != null && entry.bitmap == null && entry.processingFailed) {
                onBitmapParseFailed(remoteUri, localFile);
                return null;
            }

            return entry;
        }
    }

    protected Uri getBitmapKeyUri(Uri uri, BitmapType type) {
        if (uri == null || type == null)
            return null;

        return uri.buildUpon().clearQuery().appendQueryParameter("type", type.name()).build();
    }

    private Bitmap fileToBitmap(Uri localUri, BitmapType type) throws URISyntaxException, IOException {
        File localFile = new File(new URI(localUri.toString()));
        return fileToBitmap(localFile, type);
    }

    private Bitmap fileToBitmap(File file, BitmapType type) throws IOException {
        if (!file.exists()) {
            ln.d("Failed opening bitmap; file does not exist " + file.getAbsolutePath());
            ln.w("Failed opening bitmap; file does not exist " + file.getParent() + "/xxxxxx");
            return null;
        }

        Bitmap bitmap;
        if (type.maxH > 0 && type.maxW > 0) {
            bitmap = BitmapUtils.fileToBitmap(file, type.maxW, type.maxH, type.rgbConfig, type.getScaleType());
            //FIXME fileToBitmap shouldn't return oversize images in the first place
            if (bitmap != null && (bitmap.getWidth() > type.maxW || bitmap.getHeight() > type.maxH)) {
                Matrix m = new Matrix();
                m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, type.maxW, type.maxH), Matrix.ScaleToFit.CENTER);
                Bitmap temp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                bitmap.recycle();
                bitmap = temp;
            }
        } else if (type.maxPixels > 0) {
            bitmap = BitmapUtils.fileToBitmap(file, type.maxPixels, type.rgbConfig, type.getScaleType());
        } else {
            throw new UnsupportedOperationException("Can't convert " + type + " to bitmap");
        }

        if (bitmap == null) {
            ln.e("ERROR file exists but is not a bitmap.");
            return null;
        }

        Transformation transformation = null;

        switch (type) {
            case THUMBNAIL:
                transformation = roundedCornerTransformBig;
                break;
            case AVATAR:
            case AVATAR_READRECEIPT:
            case SETTINGS_AVATAR:
            case AVATAR_ROOM_DETAILS_DIALOG:
            case CHIP:
            case AVATAR_NOTIFICATION:
            case SIDE_NAV_AVATAR:
            case AVATAR_EDIT:
                transformation = circleTransform;
                break;
            case AVATAR_CALL_PARTICIPANT:
                if (deviceRegistration.getFeatures().isRoundAvatarFilmstripEnabled()) {
                    transformation = circleTransform;
                }
                break;
            case LARGE:
            case MULTIPAGE_DOCUMENT:
            case AVATAR_LARGE:
            default:
                break;
        }

        if (transformation != null) {
            Bitmap transformedBitmap = transformation.transform(bitmap);
            if (bitmap != transformedBitmap)
                bitmap.recycle();
            return transformedBitmap;
        }

        return bitmap;
    }

    @Override
    public void onFetchRealURIComplete(ContentManager.CacheRecordRequestParameters parameters, SecureContentReference secureContentReference) {
        if (parameters == null || parameters.getRemoteUri() == null) {
            return;
        }

        if (getListenersForUri(parameters.getRemoteUri()).isEmpty()) {
            Ln.v("unnecessary to download image because no listeners for uri: " + parameters.getRemoteUri());
            return;
        }

        getBitmap(parameters.getRemoteUri(), secureContentReference, parameters.getBitmapType(), parameters.getFileName(), null, parameters.getRetentionMode(), null);
    }

    @Override
    public void onFetchComplete(final ContentDataCacheRecord cdr) {
        if (!cdr.validate())
            return;

        for (BitmapType bitmapType : BitmapType.values()) {
            final Uri key = getBitmapKeyUri(cdr.getRemoteUri(), bitmapType);

            final BitmapEntry entry = bitmapLruCache.get(key);
            if (entry == null)
                continue;

            ln.v("Content Updated Event for " + cdr.getRemoteUri());

            taskLock.lock();
            try {
                final Bitmap oldBitmap = entry.bitmap;

                try {
                    Uri localUri = cdr.getLocalUri();
                    Bitmap newBitmap = fileToBitmap(localUri, entry.type);
                    if (newBitmap == null) {
                        if (cdr.getRemoteUri().getScheme().startsWith("http")) {
                            onBitmapParseFailed(cdr.getRemoteUri(), new File(new URI(localUri.toString())));
                        }
                        continue;
                    }

                    if (oldBitmap.sameAs(newBitmap)) {
                        newBitmap.recycle();
                        continue;
                    }

                    entry.bitmap = newBitmap;
                } catch (URISyntaxException e) {
                    ln.e(e, "Failed parsing local URI for bitmap file");
                    ln.d(e, "Failed parsing local URI for bitmap file " + cdr.getLocalUri());
                } catch (IOException e) {
                    ln.e(e, "Failed reading bitmap file");
                    ln.d(e, "Failed reading bitmap file " + cdr.getLocalUri());
                }

                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyListeners(cdr.getRemoteUri(), entry, false);
                    }
                });

                // Give the main thread time to update existing views before recycling the bitmap
                mainLooperHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (oldBitmap != null && !oldBitmap.isRecycled()) {
                            ln.i("Recycling old bitmap");
                            ln.d("Recycling old bitmap " + key);
                            oldBitmap.recycle();
                        }
                    }
                }, 1000);
            } finally {
                taskLock.unlock();
            }
        }
    }

    private void notifyListeners(final Uri remoteUri, final BitmapEntry entry, final boolean shouldFade) {
        if (entry == null)
            return;

        notifyListeners(remoteUri, entry.type, entry.bitmap, shouldFade);
    }

    private void notifyListeners(final Uri remoteUri, final BitmapType type, final Bitmap bitmap, final boolean shouldFade) {
        if (remoteUri != null && remoteUri.getPathSegments().size() >= 2) {
            ln.d("Notifying listeners " + remoteUri.getPathSegments().get(remoteUri.getPathSegments().size() - 2));
            for (final BitmapProviderListener listenerRef : getListenersForUri(remoteUri)) {
                if (listenerRef != null) {
                    mainLooperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listenerRef.onBitmapLoaded(remoteUri, type, bitmap, shouldFade);
                        }
                    });
                }
            }
        } else {
            ln.w(false, "Unable to load bitmap type (%s) due to invalid URI: %s", type.toString(), remoteUri == null ? "null" : remoteUri.toString());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LogoutEvent event) {
        clear();
    }

    protected class BitmapLruCache extends LruCache<Uri, BitmapEntry> {
        BitmapLruCache(int size) {
            super(size);
        }

        @SuppressLint("NewApi")
        @Override
        protected int sizeOf(Uri key, BitmapEntry value) {
            if (value.bitmap == null)
                return 0;

            if (UIUtils.hasKitKat())
                return value.bitmap.getAllocationByteCount();
            else
                return value.bitmap.getByteCount();
        }

        @Override
        protected void entryRemoved(boolean evicted, Uri key, BitmapEntry oldValue, BitmapEntry newValue) {
            if (oldValue != newValue) {
                ln.d("Bitmap recycled. evicted=" + evicted + " remoteUri=" + key + " cache size=" + bitmapLruCache.size());
                if (bitmapsDiffer(oldValue, newValue)
                        && oldValue.bitmap != null
                        && !oldValue.bitmap.isRecycled()) {

                    Bitmap toRecycle = oldValue.bitmap;
                    oldValue.bitmap = null;

                    if (getBitmapKeyUri(oldValue.localUri, oldValue.type).equals(key)) {
                        notifyListeners(oldValue.localUri, newValue, false);
                    } else {
                        notifyListeners(oldValue.localUri, oldValue, false);
                    }

                    toRecycle.recycle();
                }
            }
        }
    }

    @Override
    public void onFetchStart(Uri uri) {

    }

    private boolean isBlacklisted(Uri uri) {
        Integer attempts = failedToParseBitmapBlacklist.get(uri);
        return attempts != null && attempts >= 3;
    }

    private void onBitmapParseFailed(Uri uri, File localFile) {
        Integer attempts = failedToParseBitmapBlacklist.get(uri);
        if (attempts == null)
            attempts = 0;
        attempts++;
        failedToParseBitmapBlacklist.put(uri, attempts);

        ln.v("bitmap is null so deleting bitmap file and returning. " + uri);

        if (localFile != null) {
            try {
                ln.e("Failed opening bitmap for file: Deleting. " + localFile.getParent() + "/xxxxxx");

                //Note: The ContentManager will detect the deleted file on next request and re-fetch it.
                localFile.delete();
            } catch (Throwable e) {
                ln.e(e, "Failed deleting corrupt file");
            }
        }
    }
}
