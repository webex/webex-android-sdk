package com.cisco.spark.android.whiteboard.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardEncryptor;
import com.cisco.spark.android.whiteboard.WhiteboardError;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.exception.DecryptingContentFailedException;
import com.cisco.spark.android.whiteboard.exception.DownloadAnnotationBackgroundFailedException;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelImage;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelType;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;

public class WhiteboardLoader {

    private final WhiteboardService whiteboardService;
    private final WhiteboardCache whiteboardCache;
    private final SchedulerProvider schedulerProvider;
    private final Gson gson;
    private final FileLoader fileLoader;

    private final BitmapProvider bitmapProvider;
    private final Context context;
    private final ApiClientProvider apiClientProvider;

    private final WhiteboardEncryptor whiteboardEncryptor;
    private final MediaEngine mediaEngine;
    private final JsonParser jsonParser;

    private final Object loadingLock = new Object();

    public WhiteboardLoader(final WhiteboardService whiteboardService, final WhiteboardCache whiteboardCache,
                            final SchedulerProvider schedulerProvider, final Gson gson,
                            final BitmapProvider bitmapProvider, final Context context,
                            final ApiClientProvider apiClientProvider,
                            final FileLoader fileLoader, final MediaEngine mediaEngine) {
        this.whiteboardService = whiteboardService;
        this.whiteboardCache = whiteboardCache;
        this.schedulerProvider = schedulerProvider;
        this.gson = gson;
        this.bitmapProvider = bitmapProvider;
        this.context = context;
        this.apiClientProvider = apiClientProvider;

        this.fileLoader = fileLoader;

        this.whiteboardEncryptor = whiteboardService.getWhiteboardEncryptor();
        this.mediaEngine = mediaEngine;
        this.jsonParser = new JsonParser();
    }

    public Observable<Whiteboard> load(final String channelId) {
        return load(channelId, new LoaderArgs());
    }

    public Observable<Whiteboard> load(final String channelId, LoaderArgs loaderArgs) {

        Ln.d("Loading whiteboard with id %s ", channelId);

        if (channelId == null) {
            return Observable.error(new NullPointerException("Can't load board with null channel ID"));
        }

        synchronized (loadingLock) {
            String loadingBoard = whiteboardService.getLoadingChannel();
            if (loadingBoard != null) {
                if (channelId.equals(loadingBoard)) {
                    Ln.i("Already loading the same board");
                    return Observable.empty();
                } else {
                    return Observable.error(new IllegalStateException("Already loading a different board")); // TODO: queue this request up?
                }
            }
        }

        whiteboardService.setLoadingChannel(channelId);

        synchronized (loadingLock) {

            return Observable.just(channelId)
                    .concatMap(cId -> loadChannel(cId, loaderArgs))
                    .concatMap(channel -> loadBoardContents(channel, loaderArgs))
                    .doOnError(error -> {
                        synchronized (loadingLock) {
                            whiteboardService.setCurrentChannel(null);
                            whiteboardService.setLoadingChannel(null);
                            if (whiteboardService.getOnWhiteboardEventListener() != null) {
                                if (error instanceof HttpException) {
                                    HttpException httpException = (HttpException) error;
                                    whiteboardService.getOnWhiteboardEventListener().onBoardError(new WhiteboardError(WhiteboardError.ErrorData.LOAD_BOARD_ERROR, null, httpException.getMessage(), httpException.code()));
                                } else {
                                    whiteboardService.getOnWhiteboardEventListener().onBoardError(new WhiteboardError(WhiteboardError.ErrorData.LOAD_BOARD_ERROR));
                                }
                            }
                        }
                    }).doOnCompleted(() -> {
                        synchronized (loadingLock) {
                            whiteboardService.setLoadingChannel(null);
                        }
                    })
                    .doOnUnsubscribe(() -> {
                        synchronized (loadingLock) {
                            whiteboardService.setLoadingChannel(null);
                        }
                    });
        }
    }

    protected Observable<Whiteboard> loadBoardContents(Channel channel, LoaderArgs loaderArgs) {
        Observable<Whiteboard> base;

        if (loaderArgs.forceReload) {
            base = loadBoardFromCloud(channel, loaderArgs);
        } else {
            base = Observable.concat(loadBoardFromCache(channel), loadBoardFromCloud(channel, loaderArgs));
        }

        return base.first(whiteboard -> whiteboard != null && !whiteboard.isStale());
    }

    protected Observable<Whiteboard> loadBoardFromCache(Channel channel) {

        return Observable.just(channel)
                .subscribeOn(schedulerProvider.io())
                .map(c -> whiteboardCache.getWhiteboard(c.getChannelId()))
                .map(whiteboard -> {
                    if (whiteboard != null && !whiteboard.isStale()) {
                        Ln.i("Found %s in cache", whiteboard);
                        if (whiteboard.getBackgroundBitmap() != null && whiteboard.getBackgroundContent() == null) {
                            whiteboard.setStale(true);
                            Ln.i("Whiteboard in cache doesn't have a background content as it should, setting stale", whiteboard);
                        }
                    }
                    return whiteboard;
                });
    }

    protected Observable<Whiteboard> loadBoardFromCloud(Channel channel, LoaderArgs loaderArgs) {

        Observable<WhiteboardContents> contents = Observable.just(channel)
                                                            .subscribeOn(schedulerProvider.network())
                                                            .concatMap(this::loadContentsForChannel)
                                                            .map(this::decryptPayload)
                                                            .toList()
                                                            .map(this::parseWhiteboardContents);

        if (channel.getType() == ChannelType.ANNOTATION) {
            if (loaderArgs.sideloadBackgroundImage) {
                contents = sideloadBackgroundImage(channel, contents);
            } else {
                contents = contents.map(this::downloadBackgroundImage);
            }
        }

        return contents.map(whiteboardContents -> {
            Whiteboard whiteboard = whiteboardCache.createAndAddBoardToCache(
                            channel.getChannelId(),
                            whiteboardContents.getStrokes(),
                            whiteboardContents.getBackgroundContent(),
                            whiteboardContents.getBackground());
            Ln.d("Fetched %s from cloud with %d strokes", whiteboard, whiteboardContents.getStrokes().size());
            return whiteboard;
        });
    }

    @NonNull
    private Observable<WhiteboardContents> sideloadBackgroundImage(final Channel channel, Observable<WhiteboardContents> contentObservable) {

        Ln.i("Trying to side load background image from media engine");
        return contentObservable.map(wbContents -> {

            Bitmap lastContentFrame = mediaEngine.getLastContentFrame();
            if (lastContentFrame != null) {
                Ln.d("Received last content frame from media engine, side loading background image");
                wbContents.setBackground(lastContentFrame);
                Observable.just(wbContents).subscribeOn(schedulerProvider.network()).subscribe(contents -> {

                    downloadBackgroundImage(contents);
                    Whiteboard whiteboard = whiteboardCache.getWhiteboard(channel.getChannelId());
                    if (whiteboard != null) {
                        whiteboard.setBackgroundBitmap(contents.getBackground());
                        Ln.d("Downloaded original background image %s", channel.getChannelId());
                    } else {
                        Ln.w("Downloaded background image for whiteboard that is no longer cached %s", channel.getChannelId());
                    }
                }, t -> Ln.e(t, "Failed to download background for sideloaded annotation"));
            } else {
                return downloadBackgroundImage(downloadBackgroundImage(wbContents));
            }
            return wbContents;
        });
    }

    protected Observable<Channel> loadChannel(String channelId, LoaderArgs loaderArgs) {
        Channel currentChannel = whiteboardService.getCurrentChannel();
        if (!loaderArgs.forceReload && currentChannel != null && channelId.equals(currentChannel.getChannelId())) {
            Ln.d("Currently loaded channel is the correct one. Using that.");
            initMercury(currentChannel.getChannelId());
            return Observable.just(currentChannel);
        } else {
            Ln.d("Loading channel info for channel with id %s", channelId);
            return apiClientProvider.getWhiteboardPersistenceClient().getChannelRx(channelId)
                    .subscribeOn(schedulerProvider.network())
                    .map(channel -> {
                        whiteboardService.setCurrentChannel(channel);
                        String id = channel.getChannelId();
                        whiteboardCache.prepareRealtimeForBoard(id);
                        initMercury(id);

                        return channel;
                    });
        }
    }

    private Observable<Content> loadContentsForChannel(Channel channel) {
        return apiClientProvider.getWhiteboardPersistenceClient().getContentsRx(channel.getChannelId(), WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE)
                .concatMap(response -> Observable.concat(Observable.just(response.body()), loadNextContentPageUntilDone(WhiteboardUtils.getNextUrl(response))))
                .concatMapIterable(ContentItems::getItems);
    }

    private Observable<ContentItems> loadNextContentPageUntilDone(String nextUrl) {
        if (nextUrl == null) {
            return Observable.empty();
        } else {
            return apiClientProvider.getWhiteboardPersistenceClient().getContentsRx(nextUrl)
                    .concatMap(response -> Observable.concat(Observable.just(response.body()), loadNextContentPageUntilDone(WhiteboardUtils.getNextUrl(response))));
        }
    }

    private Content decryptPayload(Content content) {
        if (Content.CONTENT_TYPE.equals(content.getType())) {
            String data = whiteboardEncryptor.decryptContent(content);
            if (!TextUtils.isEmpty(data)) {
                content.setPayload(data);
            } else {
                throw new DecryptingContentFailedException("Could not decrypt content payload");
            }
        } else if (Content.CONTENT_TYPE_FILE.equals(content.getType())) {
            whiteboardEncryptor.decryptBackgroundImageContent(content);
        }
        return content;
    }

    private WhiteboardContents parseWhiteboardContents(List<Content> contents) {
        List<Stroke> strokes = new ArrayList<>();
        Content backgroundContent = null;
        for (Content content : contents) {
            if (Content.CONTENT_TYPE.equals(content.getType())) {
                try {
                    Stroke stroke = WhiteboardUtils.createStroke(content, jsonParser, gson);
                    strokes.add(stroke);
                } catch (RuntimeException e) {
                    Ln.e(e, "Error when parsing Content (type text) to Stroke");
                }
            } else if (backgroundContent == null && Content.CONTENT_TYPE_FILE.equals(content.getType())) {
                //getting the first FILE content and setting it as background
                backgroundContent = content;
            }
        }
        return new WhiteboardContents(strokes, backgroundContent);
    }

    private WhiteboardContents downloadBackgroundImage(WhiteboardContents whiteboardContents) {
        ChannelImage backgroundImage = whiteboardContents.getBackgroundContent() != null ?
                whiteboardContents.getBackgroundContent().getBackgroundImage() : null;
        if (backgroundImage != null) {

            if (backgroundImage.getUrl() != null) {
                byte[] fromCache = fileLoader.getFromCache(backgroundImage.getUrl().toString());
                if (fromCache != null) {
                    Bitmap background = fileLoader.getScaledBitmap(fromCache);
                    if (background != null) {
                        whiteboardContents.setBackground(background);
                        return whiteboardContents;
                    }
                }
            }

            Bitmap background;
            // This callback should not be needed as the bitmap provider returns a
            // future, but something in the provider breaks if it is not set.
            Action<Bitmap> callback = new Action<Bitmap>() {
                @Override
                public void call(Bitmap bitmap) {
                    Ln.d("Successfully loaded bitmap %s");
                }
            };
            try {
                Future<Bitmap> futureBackground = bitmapProvider.getBitmap(
                        backgroundImage.getSecureContentReference(),
                        BitmapProvider.BitmapType.LARGE,
                        null,
                        callback);
                background = futureBackground.get();
            } catch (Exception e) {
                throw new DownloadAnnotationBackgroundFailedException(e);
            }
            if (background != null) {
                whiteboardContents.setBackground(background);
            } else {
                throw new DownloadAnnotationBackgroundFailedException("BitmapProvider future returned null");
            }
        }
        return whiteboardContents;
    }

    private void initMercury(String channelId) {
        // This is confusing as the naming is bad. It is used to initiate the mercury connection.
        // The isLoadboard parameter HAVE to be false or we will load the board two times
        whiteboardService.loadBoard(channelId, false);
    }

    private class WhiteboardContents {
        private final List<Stroke> strokes;
        private final Content backgroundContent;
        private Bitmap background;

        public WhiteboardContents(List<Stroke> strokes, Content backgroundContent) {
            this.strokes = strokes;
            this.backgroundContent = backgroundContent;
        }

        public List<Stroke> getStrokes() {
            return strokes;
        }

        public Content getBackgroundContent() {
            return backgroundContent;
        }

        public Bitmap getBackground() {
            return background;
        }

        public void setBackground(Bitmap background) {
            this.background = background;
        }
    }

    public static class LoaderArgs {

        boolean forceReload;
        boolean sideloadBackgroundImage;

        public LoaderArgs forceReload(boolean forceReload) {
            this.forceReload = forceReload;
            return this;
        }

        public LoaderArgs sideloadBackground(boolean sideloadBackgroundImage) {
            this.sideloadBackgroundImage = sideloadBackgroundImage;
            return this;
        }
    }
}
