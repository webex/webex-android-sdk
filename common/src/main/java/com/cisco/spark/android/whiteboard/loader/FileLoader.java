package com.cisco.spark.android.whiteboard.loader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.client.ConversationClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.util.BitmapUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.util.FileUtilities;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class FileLoader {

    public static final int THUMBNAIL_MAX_PIXELS = 756 * 426; // Size of thumbnails in whiteboard grid
    public static final int SMALL_MAX_PIXELS = 200 * 200;
    public static final int BIG_MAX_PIXELS = 1080 * 1920;

    //32MB
    private static final int MAX_BYTE_CACHE = 32 * 1024 * 1024;

    private final ApiClientProvider mApiClientProvider;

    LruCache<String, byte[]> mLruCache;

    public FileLoader(ApiClientProvider clientProvider) {
        mApiClientProvider = clientProvider;
        mLruCache = new LruCache<>(MAX_BYTE_CACHE);
    }

    public void getPreview(final SecureContentReference contentReference, @Nullable final Action3<Bitmap, SecureContentReference, Boolean> successCallback,
                           @Nullable final Action1<SecureContentReference> failureCallback, final int attempts) {

        if (attempts == 0) {
            if (failureCallback != null) {
                failureCallback.call(contentReference);
            }

            return;
        }

        final String key = Strings.sha256(contentReference.getLoc());

        synchronized (FileLoader.this) {
            byte[] bitmap = mLruCache.get(key);

            if (bitmap != null) {
                if (bitmap.length == 0) {
                    Ln.e(new Exception("Retried from cache a bitmap with length 0"));
                } else if (successCallback != null) {
                    Observable.just(bitmap).observeOn(Schedulers.newThread()).subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] b) {
                            successCallback.call(getScaledBitmap(b), contentReference, true);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Ln.e(throwable);
                        }
                    });
                    return;
                }
            }
        }

        Observable.just(mApiClientProvider.getConversationClient())
                .observeOn(Schedulers.newThread())
                .compose(downloadFile(contentReference))
                .observeOn(Schedulers.computation())
                .compose(scaleBitmap(key, contentReference, successCallback))
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean b) {
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable t) {
                        getPreview(contentReference, successCallback, failureCallback, attempts - 1);
                    }
                });
    }

    public void getPage(final SecureContentReference contentReference, @Nullable final Action2<Bitmap, SecureContentReference> successCallback,
                        @Nullable final Action1<SecureContentReference> failureCallback, final boolean isSmall, final int attempts) {

        if (attempts == 0) {
            if (failureCallback != null) {
                failureCallback.call(contentReference);
            }

            return;
        }

        final String baseKey = Strings.sha256(contentReference.getLoc());
        final String key = baseKey + (isSmall ? "s" : "b");

        synchronized (FileLoader.this) {
            byte[] bitmap = mLruCache.get(key);

            if (bitmap != null) {
                if (bitmap.length == 0) {
                    Ln.e(new Exception("Retried from cache a bitmap with length 0"));
                } else if (successCallback != null) {
                    Observable.just(bitmap).observeOn(Schedulers.newThread()).subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] b) {
                            successCallback.call(FileLoader.this.getScaledBitmap(b), contentReference);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Ln.e(throwable);
                        }
                    });
                    return;
                }
            }
        }

        Observable.just(mApiClientProvider.getConversationClient())
                .observeOn(Schedulers.newThread())
                .compose(downloadFile(contentReference))
                .compose(scaleBitmapBigAndSmall(baseKey, contentReference, isSmall, successCallback))
                .observeOn(Schedulers.computation())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean b) {
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable t) {
                        Ln.e(t);
                        FileLoader.this.getPage(contentReference, successCallback, failureCallback, isSmall, attempts - 1);
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                    }
                });
    }

    private Observable.Transformer<ConversationClient, Response> downloadFile(final SecureContentReference contentReference) {
        return new Observable.Transformer<ConversationClient, Response>() {
            @Override
            public Observable<Response> call(Observable<ConversationClient> observable) {
                return observable
                        .map(new Func1<ConversationClient, Response>() {
                            @Override
                            public Response call(ConversationClient conversationClient) {
                                Response response = null;
                                try {
                                    response = conversationClient.downloadFile(contentReference.getLoc()).execute();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                                if (!response.isSuccessful()) {
                                    throw new RuntimeException("Download file failed with error code " + response.code());
                                }

                                return response;
                            }
                        });
            }
        };
    }

    private Observable.Transformer<Response, Boolean> scaleBitmap(final String key, final SecureContentReference contentReference,
                                                                  @Nullable final Action3<Bitmap, SecureContentReference, Boolean> callBack) {
        return new Observable.Transformer<Response, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<Response> observable) {
                return observable
                        .map(new Func1<Response, Boolean>() {
                            @Override
                            public Boolean call(Response response) {
                                InputStream in = getInputStream(contentReference, response);

                                byte[] bitmapBytes = getBitmapBytes(in);

                                Ln.d("Loading thumbnail with " + bitmapBytes.length + " bytes");

                                Bitmap scaledBitmap = getScaledBitmap(in, bitmapBytes, THUMBNAIL_MAX_PIXELS);

                                try {
                                    in.close();
                                } catch (IOException e) {
                                    Ln.e(e);
                                }
                                putInCache(key, scaledBitmap);

                                if (callBack != null) {
                                    callBack.call(scaledBitmap, contentReference, false);
                                } else {
                                    scaledBitmap.recycle();
                                }

                                return true;
                            }
                        });
            }
        };
    }

    private Observable.Transformer<Response, Boolean> scaleBitmapBigAndSmall(final String baseKey, final SecureContentReference contentReference, final boolean small,
                                                                             @Nullable final Action2<Bitmap, SecureContentReference> callBack) {
        return new Observable.Transformer<Response, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<Response> observable) {
                return observable
                        .observeOn(Schedulers.computation())
                        .map(new Func1<Response, Boolean>() {
                            @Override
                            public Boolean call(Response response) {
                                InputStream in = getInputStream(contentReference, response);

                                byte[] bitmapBytes = getBitmapBytes(in);

                                Bitmap smallBitmap = getScaledBitmap(in, bitmapBytes, SMALL_MAX_PIXELS);

                                Bitmap bigBitmap = getScaledBitmap(in, bitmapBytes, BIG_MAX_PIXELS);

                                try {
                                    in.close();
                                } catch (IOException e) {
                                    Ln.e(e);
                                }

                                putInCache(baseKey + "s", smallBitmap);
                                putInCache(baseKey + "b", bigBitmap);

                                if (callBack != null) {
                                    if (small) {
                                        bigBitmap.recycle();
                                        callBack.call(smallBitmap, contentReference);
                                    } else {
                                        smallBitmap.recycle();
                                        callBack.call(bigBitmap, contentReference);
                                    }
                                } else {
                                    bigBitmap.recycle();
                                    smallBitmap.recycle();
                                }

                                return true;
                            }
                        });
            }
        };
    }

    private byte[] getBitmapBytes(InputStream in) {
        byte[] bitmapBytes;

        try {
            bitmapBytes = FileUtilities.readBytes(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bitmapBytes;
    }

    private InputStream getInputStream(SecureContentReference contentReference, Response response) {
        if (((ResponseBody) response.body()).contentLength() == 0) {
            throw new RuntimeException();
        }

        InputStream is = ((ResponseBody) response.body()).byteStream();

        if (is == null) {
            throw new RuntimeException("Downloaded file input stream is null");
        }

        InputStream in = null;
        try {
            in = contentReference.decrypt(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return in;
    }

    @NonNull
    private Bitmap getScaledBitmap(InputStream in, byte[] bitmapBytes, int size) {
        Bitmap bitmap;
        try {
            bitmap = FileUtilities.inputStreamToBitmap(bitmapBytes, size, BitmapUtils.ScaleProvider.ScaleType.DOWNSCALE_ONLY);
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException e1) {
                Ln.e(e1);
            }
            throw new RuntimeException(e);
        }

        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            try {
                in.close();
            } catch (IOException e) {
                Ln.e(e);
            }
            throw new RuntimeException("Bitmap has a width or height of one");
        }
        return bitmap;
    }

    public Bitmap getScaledBitmap(byte[] bitmap) {
        return BitmapFactory.decodeByteArray(bitmap, 0, bitmap.length);
    }

    public void putInCache(String key, Bitmap bitmap) {
        synchronized (this) {
            mLruCache.put(key, FileUtilities.getByteArray(bitmap));
        }
    }

    public void putInCache(String key, Uri fileUri) {
        synchronized (this) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(new File(fileUri.getPath()));
                mLruCache.put(key, FileUtilities.readBytes(in));
                in.close();
            } catch (Exception e) {
                Ln.e(e);
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        Ln.e(e);
                    }
            }
        }
    }
}
