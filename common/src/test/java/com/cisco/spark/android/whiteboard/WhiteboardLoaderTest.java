package com.cisco.spark.android.whiteboard;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.model.Json;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.CompletedFuture;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.exception.DecryptingContentFailedException;
import com.cisco.spark.android.whiteboard.exception.DownloadAnnotationBackgroundFailedException;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.cisco.spark.android.whiteboard.loader.WhiteboardLoader;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelImage;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelType;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wacom.ink.rasterization.BlendMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import retrofit2.Response;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@RunWith(RobolectricTestRunner.class)
public class WhiteboardLoaderTest {

    @Mock WhiteboardService whiteboardService;
    @Mock WhiteboardCache whiteboardCache;
    @Mock SchedulerProvider schedulerProvider;
    @Mock BitmapProvider bitmapProvider;
    @Mock ApiClientProvider apiClientProvider;
    @Mock WhiteboardPersistenceClient whiteboardPersistenceClient;
    @Mock WhiteboardEncryptor whiteboardEncryptor;
    @Mock MediaEngine mediaEngine;
    @Mock FileLoader fileLoader;
    @Mock Context context;

    Gson gson;
    TestScheduler networkScheduler;
    TestScheduler computationScheduler;
    TestScheduler ioScheduler;
    Scheduler mainThreadScheduler;
    WhiteboardLoader whiteboardLoader;


    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        gson = Json.buildGson();

        networkScheduler = new TestScheduler();
        computationScheduler = new TestScheduler();
        ioScheduler = new TestScheduler();
        mainThreadScheduler = Schedulers.immediate();

        when(whiteboardService.getWhiteboardEncryptor()).thenReturn(whiteboardEncryptor);
        // Make getCurrentChannel() return the same channel after call to setCurrentChannel()
        doAnswer(invocation -> {
            when(whiteboardService.getCurrentChannel()).thenReturn((Channel) invocation.getArguments()[0]);
            return null;
        }).when(whiteboardService).setCurrentChannel(any(Channel.class));

        Answer createWhiteboardAnswer = invocation -> {
            Object[] arguments = invocation.getArguments();
            String channelId = (String) arguments[0];
            List<Stroke> strokes = (List<Stroke>) arguments[1];
            Content backgroundContent = null;
            Bitmap background = null;
            if (arguments.length > 2) {
                backgroundContent = (Content) arguments[2];
                background = (Bitmap) arguments[3];
            }
            return new Whiteboard(channelId, strokes, backgroundContent, background);
        };
        doAnswer(createWhiteboardAnswer).when(whiteboardCache).createAndAddBoardToCache(any(String.class), any(List.class));
        doAnswer(createWhiteboardAnswer).when(whiteboardCache).createAndAddBoardToCache(any(String.class), any(List.class), any(Content.class), any(Bitmap.class));

        when(schedulerProvider.network()).thenReturn(networkScheduler);
        when(schedulerProvider.computation()).thenReturn(computationScheduler);
        when(schedulerProvider.io()).thenReturn(ioScheduler);
        when(schedulerProvider.mainThread()).thenReturn(mainThreadScheduler);

        when(apiClientProvider.getWhiteboardPersistenceClient()).thenReturn(whiteboardPersistenceClient);

        whiteboardLoader = new WhiteboardLoader(whiteboardService, whiteboardCache, schedulerProvider, gson, bitmapProvider, context, apiClientProvider, fileLoader, mediaEngine);
    }

    @Test
    public void testLoadFromCloud() {
        String channelId = "790eae94-8bd8-4116-b410-91517e6207d8";

        Channel mockChannel = createMockChannel(channelId);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        List<Content> contentList = new ArrayList<>();
        Content curve1 = createMockCurveContent("1df07cf8-90dc-4d45-a366-f0859492aa10", "abcdef1234567890");
        contentList.add(curve1);
        Content curve2 = createMockCurveContent("914f5a48-6116-4fdc-a669-76e438b2ef95", "0987654321fedcba");
        contentList.add(curve2);
        ContentItems mockContents = new ContentItems(contentList);
        Response<ContentItems> mockContentsResponse = Response.success(mockContents);
        when(whiteboardPersistenceClient.getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE))
                .thenReturn(Observable.just(mockContentsResponse));

        float[] curve1Points = new float[] { 128.0f, 543.0f, 1.0f, 999.0f, 666.0f, 1.0f };
        String curve1Json = createMockCurveJson(curve1Points, Color.BLACK, false);
        when(whiteboardEncryptor.decryptContent(curve1)).thenReturn(curve1Json);
        float[] curve2Points = new float[] { 1000.0f, 500.0f, 1.0f, 666.0f, 999.0f, 1.0f };
        String curve2Json = createMockCurveJson(curve2Points, Color.RED, false);
        when(whiteboardEncryptor.decryptContent(curve2)).thenReturn(curve2Json);

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertNull(testSubscriber.error);
        assertNotNull(testSubscriber.whiteboard);
        Whiteboard whiteboard = testSubscriber.whiteboard;
        assertEquals(channelId, whiteboard.getId());
        assertEquals(2, whiteboard.getStrokes().size());
        assertNull(whiteboard.getBackgroundBitmap());
        verify(whiteboardPersistenceClient).getChannelRx(channelId);
        // whiteboardService.loadBoard(channelId, false) initiates mercury (bad naming)
        verify(whiteboardService).loadBoard(channelId, false);
        verify(whiteboardCache).getWhiteboard(channelId);
        verify(whiteboardCache).prepareRealtimeForBoard(channelId);
        verify(whiteboardPersistenceClient).getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE);
    }

    @Test
    public void testLoadFromCache() {
        String channelId = "e250bc01-3651-4ee9-bb38-7816863894ae";

        Channel mockChannel = createMockChannel(channelId);
        when(whiteboardService.getCurrentChannel()).thenReturn(mockChannel);

        float[] points1 = new float[] { 0.0f, 0.0f, 1.0f, 1600.0f, 1000.0f, 1.0f };
        float[] points2 = new float[] { 1600.0f, 0.0f, 1.0f, 0.0f, 1000.0f, 1.0f };
        float[] points3 = new float[] { 0.0f, 500.f, 1.0f, 1600.f, 500.f, 1.0f};
        Whiteboard mockWhiteboard = createMockWhiteboard(
                channelId,
                Arrays.asList(
                    createMockStroke(points1, Color.BLACK, false),
                    createMockStroke(points2, Color.BLACK, true),
                    createMockStroke(points3, Color.BLUE, false)
                )
        );
        when(whiteboardCache.getWhiteboard(channelId)).thenReturn(mockWhiteboard);

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertNull(testSubscriber.error);
        assertNotNull(testSubscriber.whiteboard);
        Whiteboard whiteboard = testSubscriber.whiteboard;
        assertEquals(channelId, whiteboard.getId());
        assertEquals(3, whiteboard.getStrokes().size());
        assertNull(whiteboard.getBackgroundBitmap());
        verify(whiteboardPersistenceClient, never()).getChannelRx(any(String.class));
        // whiteboardService.loadBoard(channelId, false) initiates mercury (bad naming)
        verify(whiteboardService).loadBoard(channelId, false);
        verify(whiteboardCache).getWhiteboard(channelId);
        verify(whiteboardPersistenceClient, never()).getContentsRx(any(String.class), any(int.class));
    }

    @Test
    public void testLoadFromCacheWillReloadChannelIfIncorrectChannelInWhiteboardService() {
        String channelIdIncorrect = "57234ae8-0c55-4a2b-9672-d4c162d189e6";
        String channelId = "750a2a8d-6369-4d99-a3c8-0df554e38491";

        when(whiteboardService.getCurrentChannel()).thenReturn(createMockChannel(channelIdIncorrect));
        Channel mockChannel = createMockChannel(channelId);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        float[] points = new float[] { 800f, 0.0f, 1.0f, 800.0f, 1000.0f, 1.0f };
        Whiteboard mockWhiteboard = createMockWhiteboard(channelId, Collections.singletonList(createMockStroke(points, Color.BLACK, false)));
        when(whiteboardCache.getWhiteboard(channelId)).thenReturn(mockWhiteboard);

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertNull(testSubscriber.error);
        assertNotNull(testSubscriber.whiteboard);
        Whiteboard whiteboard = testSubscriber.whiteboard;
        assertEquals(channelId, whiteboard.getId());
        assertEquals(1, whiteboard.getStrokes().size());
        assertNull(whiteboard.getBackgroundBitmap());
        verify(whiteboardPersistenceClient).getChannelRx(channelId);
        // whiteboardService.loadBoard(channelId, false) initiates mercury (bad naming)
        verify(whiteboardService).loadBoard(channelId, false);
        verify(whiteboardCache).getWhiteboard(channelId);
        verify(whiteboardPersistenceClient, never()).getContentsRx(any(String.class), any(int.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBoardWithAnnotation() {
        String channelId = "5c362c65-b758-46a3-993e-d5416e968e79";

        Channel mockChannel = createMockChannel(channelId);
        mockChannel.setChannelType(ChannelType.ANNOTATION);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        List<Content> contentList = new ArrayList<>();
        SecureContentReference mockBackgroundScr = mock(SecureContentReference.class);
        Content background = createMockBackgroundContent("ecacd30d-e954-4f9d-9575-ebeff7a16d65", mockBackgroundScr);
        contentList.add(background);
        Content curve = createMockCurveContent("ca40a2c1-d486-4a4e-aaee-2f323d8db7c7", "abcdef1234567890");
        contentList.add(curve);
        ContentItems mockContents = new ContentItems(contentList);
        Response<ContentItems> mockContentsResponse = Response.success(mockContents);
        when(whiteboardPersistenceClient.getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE))
                .thenReturn(Observable.just(mockContentsResponse));

        float[] curvePoints = new float[] { 128.0f, 543.0f, 1.0f, 999.0f, 666.0f, 1.0f };
        String curveJson = createMockCurveJson(curvePoints, Color.GREEN, false);
        when(whiteboardEncryptor.decryptContent(curve)).thenReturn(curveJson);

        Bitmap mockBitmap = mock(Bitmap.class);
        when(bitmapProvider.getBitmap(eq(mockBackgroundScr), eq(BitmapProvider.BitmapType.LARGE), (String) isNull(), (Action<Bitmap>) isNotNull()))
                .thenReturn(new CompletedFuture<>(mockBitmap));

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        assertNull(testSubscriber.error);
        assertNotNull(testSubscriber.whiteboard);
        Whiteboard whiteboard = testSubscriber.whiteboard;
        assertEquals(channelId, whiteboard.getId());
        assertEquals(1, whiteboard.getStrokes().size());
        assertEquals(mockBitmap, whiteboard.getBackgroundBitmap());
        verify(whiteboardPersistenceClient).getChannelRx(channelId);
        // whiteboardService.loadBoard(channelId, false) initiates mercury (bad naming)
        verify(whiteboardService).loadBoard(channelId, false);
        verify(whiteboardCache).getWhiteboard(channelId);
        verify(whiteboardCache).prepareRealtimeForBoard(channelId);
        verify(whiteboardPersistenceClient).getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE);
    }

    @Test
    public void testMultiBatchLoading() {
        String channelId = "c11a520a-fb72-4e0a-8390-86db85c244c6";

        Channel mockChannel = createMockChannel(channelId);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        String[] batch1Ids = new String[] {
                "29303030-28a9-4b9c-b200-a26f0688f86c",
                "b836fe16-c1ba-4071-97ed-3809cbb600b6",
                "9ac4acaf-4811-47b3-bae2-28b3ece73088",
                "1de9b47a-1aea-4831-b63c-0d1ae2d2fc1c",
                "b5078eb3-fa21-4d57-ae47-4ac046baefa3"
        };
        List<Content> batch1ContentList = new ArrayList<>();
        for (String id : batch1Ids) {
            batch1ContentList.add(createMockCurveContent(id, "abcdef"));
        }
        ContentItems mockContentsBatch1 = new ContentItems(batch1ContentList);
        String nextUrlBatch2 = "http://get/batch/2";
        Headers mockHeadersBatch1 = createHeadersWithNextUrl(nextUrlBatch2);
        Response<ContentItems> mockContentsResponseBatch1 = Response.success(mockContentsBatch1, mockHeadersBatch1);
        when(whiteboardPersistenceClient.getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE))
                .thenReturn(Observable.just(mockContentsResponseBatch1));

        String[] batch2Ids = new String[] {
                "c4beeaf2-8f17-4dcc-bde3-70c9a20d335e",
                "b2638e31-48b3-4a3f-aaf6-77142fbdafea",
                "dfef7554-a2c3-4458-9efe-b38efeaf8eeb",
                "fafc94d3-0193-4e5e-80e7-faf1ba2d68d2",
                "8166b40a-a6d7-479f-8581-2317465cdafc"
        };
        List<Content> batch2ContentList = new ArrayList<>();
        for (String id : batch2Ids) {
            batch2ContentList.add(createMockCurveContent(id, "abcdef"));
        }
        ContentItems mockContentsBatch2 = new ContentItems(batch2ContentList);
        String nextUrlBatch3 = "http://get/batch/3";
        Headers mockHeadersBatch2 = createHeadersWithNextUrl(nextUrlBatch3);
        Response<ContentItems> mockContentsResponseBatch2 = Response.success(mockContentsBatch2, mockHeadersBatch2);
        when(whiteboardPersistenceClient.getContentsRx(nextUrlBatch2))
                .thenReturn(Observable.just(mockContentsResponseBatch2));

        String[] batch3Ids = new String[] {
                "106c7e93-4d84-481a-a255-6bd072cc1393",
                "0c42e137-eb6c-4777-a8c3-b34bb821ce54"
        };
        List<Content> batch3ContentList = new ArrayList<>();
        for (String id : batch3Ids) {
            batch3ContentList.add(createMockCurveContent(id, "abcdef"));
        }
        ContentItems mockContentsBatch3 = new ContentItems(batch3ContentList);
        Response<ContentItems> mockContentsResponseBatch3 = Response.success(mockContentsBatch3);
        when(whiteboardPersistenceClient.getContentsRx(nextUrlBatch3))
                .thenReturn(Observable.just(mockContentsResponseBatch3));

        List<Content> allMockContent = new ArrayList<>();
        allMockContent.addAll(batch1ContentList);
        allMockContent.addAll(batch2ContentList);
        allMockContent.addAll(batch3ContentList);

        for (Content mockContent : allMockContent) {
            when(whiteboardEncryptor.decryptContent(mockContent))
                    .thenReturn(createMockCurveJson(new float[0], Color.BLACK, false));
        }

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertNull(testSubscriber.error);
        assertNotNull(testSubscriber.whiteboard);
        Whiteboard whiteboard = testSubscriber.whiteboard;
        assertEquals(channelId, whiteboard.getId());
        assertEquals(12, whiteboard.getStrokes().size());
        assertNull(whiteboard.getBackgroundBitmap());
        verify(whiteboardPersistenceClient).getChannelRx(channelId);
        // whiteboardService.loadBoard(channelId, false) initiates mercury (bad naming)
        verify(whiteboardService).loadBoard(channelId, false);
        verify(whiteboardCache).getWhiteboard(channelId);
        verify(whiteboardCache).prepareRealtimeForBoard(channelId);
        verify(whiteboardPersistenceClient).getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE);
        verify(whiteboardPersistenceClient).getContentsRx(nextUrlBatch2);
        verify(whiteboardPersistenceClient).getContentsRx(nextUrlBatch3);
    }

    @Test
    public void testShouldFailIfChannelIdIsNull() {
        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(null)
                .subscribe(testSubscriber);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertThat(testSubscriber.error, instanceOf(NullPointerException.class));
    }

    @Test
    public void testShouldFailIfDecryptionFails() {
        String channelId = "fbec7872-f2d9-4097-b405-ad385db01a05";

        Channel mockChannel = createMockChannel(channelId);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        List<Content> contentList = new ArrayList<>();
        Content curve = createMockCurveContent("784a4dbd-0cbe-432d-88ac-075d31d84b3c", "abcdef1234567890");
        contentList.add(curve);
        ContentItems mockContents = new ContentItems(contentList);
        Response<ContentItems> mockContentsResponse = Response.success(mockContents);
        when(whiteboardPersistenceClient.getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE))
                .thenReturn(Observable.just(mockContentsResponse));

        when(whiteboardEncryptor.decryptContent(curve)).thenReturn(null);

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertThat(testSubscriber.error, instanceOf(DecryptingContentFailedException.class));
    }

    @Test
    public void testShouldFailIfBitmapProviderThrowsException() throws ExecutionException, InterruptedException {
        String channelId = "2a99fa3d-4cb3-4feb-b3fc-ccd839f89bd6";

        Channel mockChannel = createMockChannel(channelId);
        mockChannel.setChannelType(ChannelType.ANNOTATION);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        List<Content> contentList = new ArrayList<>();
        SecureContentReference mockBackgroundScr = mock(SecureContentReference.class);
        Content background = createMockBackgroundContent("ecacd30d-e954-4f9d-9575-ebeff7a16d65", mockBackgroundScr);
        contentList.add(background);
        ContentItems mockContents = new ContentItems(contentList);
        Response<ContentItems> mockContentsResponse = Response.success(mockContents);
        when(whiteboardPersistenceClient.getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE))
                .thenReturn(Observable.just(mockContentsResponse));

        ExecutionException causeException = new ExecutionException("FOO", new RuntimeException("BAR"));
        Future<Bitmap> mockFuture = mock(Future.class);
        when(bitmapProvider.getBitmap(eq(mockBackgroundScr), eq(BitmapProvider.BitmapType.LARGE), (String) isNull(), (Action<Bitmap>) isNotNull()))
                .thenReturn(mockFuture);
        when(mockFuture.get()).thenThrow(causeException);

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertThat(testSubscriber.error, instanceOf(DownloadAnnotationBackgroundFailedException.class));
        assertEquals(causeException, testSubscriber.error.getCause());
    }

    @Test
    public void testShouldFailIfBitmapProviderReturnsNull() {
        String channelId = "5880f090-8862-40d0-beec-c4c842f81961";

        Channel mockChannel = createMockChannel(channelId);
        mockChannel.setChannelType(ChannelType.ANNOTATION);
        when(whiteboardPersistenceClient.getChannelRx(channelId)).thenReturn(Observable.just(mockChannel));

        List<Content> contentList = new ArrayList<>();
        SecureContentReference mockBackgroundScr = mock(SecureContentReference.class);
        Content background = createMockBackgroundContent("ecacd30d-e954-4f9d-9575-ebeff7a16d65", mockBackgroundScr);
        contentList.add(background);
        ContentItems mockContents = new ContentItems(contentList);
        Response<ContentItems> mockContentsResponse = Response.success(mockContents);
        when(whiteboardPersistenceClient.getContentsRx(channelId, WhiteboardConstants.WHITEBOARD_CONTENT_BATCH_SIZE))
                .thenReturn(Observable.just(mockContentsResponse));

        when(bitmapProvider.getBitmap(eq(mockBackgroundScr), eq(BitmapProvider.BitmapType.LARGE), (String) isNull(), (Action<Bitmap>) isNotNull()))
                .thenReturn(new CompletedFuture<>(null));

        TestSubscriber testSubscriber = new TestSubscriber();
        whiteboardLoader.load(channelId)
                .subscribe(testSubscriber);
        // This will trigger channel loading from cloud
        networkScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        computationScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ioScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // This will trigger whiteboard contents loading from cloud
        networkScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertThat(testSubscriber.error, instanceOf(DownloadAnnotationBackgroundFailedException.class));
    }

    private Channel createMockChannel(String channelId) {
        Channel mockChannel = new Channel();
        mockChannel.setChannelId(channelId);
        return mockChannel;
    }

    private Content createMockCurveContent(String contentId, String payload) {
        Content content = new Content(Content.CONTENT_TYPE, "http://device/url", "http://encryption/url", payload);
        content.setContentId(contentId);
        return content;
    }

    private Content createMockBackgroundContent(String contentId, SecureContentReference secureContentReference) {
        Content content = new Content(Content.CONTENT_TYPE_FILE, "http://device/url", "http://encryption/url", null);
        content.setContentId(contentId);
        ChannelImage backgroundImage = new ChannelImage();
        backgroundImage.setSecureContentReference(secureContentReference);
        content.setBackgroundImage(backgroundImage);
        return content;
    }

    private String createMockCurveJson(float[] curvePoints, int color, boolean isErase) {
        JsonArray points = new JsonArray();
        for (float point : curvePoints) {
            points.add(point);
        }
        JsonObject curve = new JsonObject();
        curve.addProperty("curveId", UUID.randomUUID().toString());
        curve.add("curvePoints", points);
        curve.addProperty("type", "curve");
        curve.addProperty("drawMode", isErase ? "ERASE" : "NORMAL");
        curve.add("color", WhiteboardUtils.convertColorIntToJson(color));
        curve.addProperty("stride", 3);
        return curve.toString();
    }

    private Whiteboard createMockWhiteboard(String channelId, List<Stroke> strokes) {
        return new Whiteboard(channelId, strokes);
    }

    private Stroke createMockStroke(float[] points, int color, boolean isErase) {
        return new Stroke(UUID.randomUUID(), points, color, 3, isErase ? BlendMode.BLENDMODE_ERASE : BlendMode.BLENDMODE_NORMAL);
    }

    private Headers createHeadersWithNextUrl(String nextUrl) {
        return new Headers.Builder().add("Link", "<" + nextUrl + ">; rel=\"next\"").build();
    }

    private class TestSubscriber extends Subscriber<Whiteboard> {
        Throwable error;
        Whiteboard whiteboard;

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            this.error = e;
        }

        @Override
        public void onNext(Whiteboard whiteboard) {
            this.whiteboard = whiteboard;
        }
    }
}
