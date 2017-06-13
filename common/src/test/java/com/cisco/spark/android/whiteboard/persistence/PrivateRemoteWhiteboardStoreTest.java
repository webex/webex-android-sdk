package com.cisco.spark.android.whiteboard.persistence;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Json;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16, manifest = "../AndroidManifest.xml")
public class PrivateRemoteWhiteboardStoreTest {

    @Mock WhiteboardService whiteboardService;
    @Mock ApiClientProvider apiClientProvider;
    @Mock SchedulerProvider schedulerProvider;
    @Mock WhiteboardPersistenceClient boardClient;
    @Mock WhiteboardListCache cache;
    @Mock Clock clock;
    @Mock EventBus bus;

    private Injector injector;

    public PrivateRemoteWhiteboardStoreTest() {

        MockitoAnnotations.initMocks(this);

        when(apiClientProvider.getWhiteboardPersistenceClient()).thenReturn(boardClient);

        final ObjectGraph objectGraph = ObjectGraph.create(new MockModule());
        injector = new TestInjector(objectGraph);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateChannel() {

        AbsRemoteWhiteboardStore store = new PrivateRemoteWhiteboardStore(whiteboardService, apiClientProvider, injector,
                                                                          schedulerProvider);
        boolean result = store.createChannel(null);
        assertFalse("Create channel with null channel info should return false", result);

        // Make it fail
        final Call<Channel> call = mock(Call.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Callback arg = (Callback) invocation.getArguments()[0];
                arg.onFailure(call, new NullPointerException());
                return null;
            }
        }).when(call).enqueue(any(Callback.class));

        when(boardClient.createPrivateChannel(any(Channel.class))).thenReturn(call);

        result = store.createChannel(new Channel());
        assertFalse("A channel should not call through to the board service without a keyUrl", result); // FIXME We should check this
    }

    // --------------------------------------------------------------------------------
    // Test utilites for dagger
    // --------------------------------------------------------------------------------

    private class TestInjector implements Injector {

        private ObjectGraph objectGraph;

        public TestInjector(ObjectGraph objectGraph) {
            this.objectGraph = objectGraph;
        }

        @Override
        public void inject(Object object) {
            objectGraph.inject(object);
        }

        @Override
        public ObjectGraph getObjectGraph() {
            return objectGraph;
        }
    }

    @SuppressWarnings("unused")
    @Module(
        library = true,
        complete = false,
        injects = {
            AbsRemoteWhiteboardStore.class,
            PrivateRemoteWhiteboardStore.class
        }
    )
    public class MockModule {

        @Provides
        @Singleton
        WhiteboardListCache provideWhiteboardListCache() {
            return cache;
        }

        @Provides
        @Singleton
        Clock provideClock() {
            return clock;
        }

        @Provides
        @Singleton
        EventBus provideBus() {
            return bus;
        }

        @Provides
        Gson provideGson() {
            return Json.buildGson();
        }

        @Provides
        @Singleton
        WhiteboardCache provideWhiteboardCache(EventBus eventBus, Gson gson) {
            return new WhiteboardCache(eventBus, gson);
        }

    }
}
