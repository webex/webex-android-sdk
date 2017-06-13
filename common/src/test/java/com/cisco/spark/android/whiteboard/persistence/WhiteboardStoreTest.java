package com.cisco.spark.android.whiteboard.persistence;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Content;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16, manifest = Config.NONE)
public class WhiteboardStoreTest {

    @Mock ApiClientProvider apiClientProvider;
    @Mock WhiteboardService whiteboardService;
    @Mock SchedulerProvider schedulerProvider;
    @Mock SdkClient sdkClient;
    @Mock Injector injector;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetContentBatch() {

        AbsRemoteWhiteboardStore whiteboardStore = createRemoteWhiteboardStore();

        // Null
        List<List<Content>> contentBatch = whiteboardStore.getContentBatch(null);
        assertNotNull(contentBatch);
        assertEquals(0, contentBatch.size());

        // Empty
        contentBatch = whiteboardStore.getContentBatch(new ArrayList<>());
        assertNotNull(contentBatch);
        assertEquals(0, contentBatch.size());

        testFullPartialBatches(whiteboardStore, 0, true);
        testFullPartialBatches(whiteboardStore, 1, true);
        testFullPartialBatches(whiteboardStore, 100, true);

        testFullPartialBatches(whiteboardStore, 0, false);
        testFullPartialBatches(whiteboardStore, 1, false);
        testFullPartialBatches(whiteboardStore, 100, false);
    }

    private void testFullPartialBatches(AbsRemoteWhiteboardStore whiteboardStore, int numFull, boolean includePartial) {

        final int fullSize = 150;
        final int partialSize = 40;
        int numPartial = includePartial ? 1 : 0;

        List<List<Content>> contentBatch;
        int total = numFull * fullSize + numPartial * partialSize;

        List<Content> input = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            input.add(new Content(Content.CONTENT_TYPE, "FAKE_DEVICE", "http://fake.url", "ey1234"));
        }

        contentBatch = whiteboardStore.getContentBatch(input);
        assertEquals(numFull + numPartial, contentBatch.size());
        int i;
        for (i = 0; i < numFull; i++) {
            assertEquals(fullSize, contentBatch.get(i).size());
        }
        if (includePartial) {
            assertEquals(partialSize, contentBatch.get(i).size());
        }
    }

    private AbsRemoteWhiteboardStore createRemoteWhiteboardStore() {
        return new RemoteWhiteboardStore(whiteboardService, apiClientProvider, injector, schedulerProvider);
    }
}
