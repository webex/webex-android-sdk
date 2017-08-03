package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.Features;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16, manifest = Config.NONE)
public class WhiteboardCreatorTest {

    private static final WhiteboardService.WhiteboardContext PRIVATE_CONTEXT = new WhiteboardService.WhiteboardContext(null, null, null, null);

    @Mock WhiteboardPersistenceClient whiteboardPersistenceClient;
    @Mock EncryptedConversationProcessor conversationProcessor;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;
    @Mock DeviceRegistration deviceRegistration;
    @Mock ApiClientProvider apiClientProvider;
    @Mock WhiteboardService whiteboardService;
    @Mock SchedulerProvider schedulerProvider;
    @Mock WhiteboardCache whiteboardCache;
    @Mock KeyManager keyManager;
    @Mock SdkClient sdkClient;
    @Mock Injector injector;
    @Mock Context context;
    @Mock EventBus bus;
    private Gson gson;

    private TestScheduler testScheduler;

    public WhiteboardCreatorTest() {
        gson = new Gson();
    }

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);

        testScheduler = new TestScheduler();

        // Always need to do this in unit tests
        when(schedulerProvider.from(any(Executor.class))).thenReturn(Schedulers.immediate());
        when(schedulerProvider.computation()).thenReturn(testScheduler);

        Features features = new Features();
        when(deviceRegistration.getFeatures()).thenReturn(features);

        when(apiClientProvider.getWhiteboardPersistenceClient()).thenReturn(whiteboardPersistenceClient);

        AuthenticatedUser mockUser = mock(AuthenticatedUser.class);
        when(authenticatedUserProvider.getAuthenticatedUserOrNull()).thenReturn(mockUser);
        when(mockUser.getUserId()).thenReturn("myId");
    }

    @Test
    public void testPrivateCreateNoKeysStops() {

        when(sdkClient.supportsPrivateBoards()).thenReturn(true);
        when(keyManager.getCachedUnboundKey()).thenReturn(null);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(PRIVATE_CONTEXT));

        // Make sure it never makes it past the return
        verify(apiClientProvider, never()).getWhiteboardPersistenceClient();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPrivateWithKeys() throws InterruptedException {

        System.out.println("testPrivateWithKeys");

        KeyObject canHazYourKeyz = mock(KeyObject.class);
        Uri keyUrl = Uri.parse("http://kmskmskms.com/KEYYZZZZZ");
        when(canHazYourKeyz.getKeyUrl()).thenReturn(keyUrl);

        String kmsMessage = "I AM KMS";
        WhiteboardStore store = mock(WhiteboardStore.class);

        when(sdkClient.supportsPrivateBoards()).thenReturn(true);
        when(keyManager.getCachedUnboundKey()).thenReturn(canHazYourKeyz);
        when(conversationProcessor.createNewResource(any(List.class), any(List.class))).thenReturn(kmsMessage);
        when(whiteboardService.getPrivateStore()).thenReturn(store);

        Channel createdChannel = mock(Channel.class);

        Call<Channel> call = mock(Call.class);
        when(whiteboardPersistenceClient.createPrivateChannel(anyBoolean(), anyBoolean(), any())).thenReturn(call);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(PRIVATE_CONTEXT));

        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);

        // Make sure it never makes it past the return
        verify(whiteboardPersistenceClient, times(1)).createPrivateChannel(anyBoolean(), anyBoolean(), channelCaptor.capture());
        verify(whiteboardPersistenceClient, never()).createChannel(anyBoolean(), anyBoolean(), any());

        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(call, times(1)).enqueue(callbackCaptor.capture());

        Response response = Response.success(createdChannel);
        Callback value = callbackCaptor.getValue();
        value.onResponse(call, response);

        Channel channel = channelCaptor.getValue();

        assertNotNull(channel.getKmsMessage());
        assertEquals(keyUrl, channel.getDefaultEncryptionKeyUrl());
    }

    @Test
    public void testAclUrlNoKeys() {

        WhiteboardStore store = mock(WhiteboardStore.class);
        when(whiteboardService.getRemoteStore()).thenReturn(store);

        Uri aclUrl = Uri.parse("http://acl-acl.com/C15CO-C15CO-C15CO");

        WhiteboardService.WhiteboardContext convContext = new WhiteboardService.WhiteboardContext(aclUrl, null, null, null);
        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(convContext);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(createData);

        when(keyManager.getCachedUnboundKey()).thenReturn(null);

        verify(whiteboardService, never()).getRemoteStore();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAclUrlWithKeysContinue() {

        System.out.println("Test with keys continue");

        WhiteboardStore store = mock(WhiteboardStore.class);
        when(whiteboardService.getRemoteStore()).thenReturn(store);

        Uri aclUrl = Uri.parse("http://acl-acl.com/C15CO-C15CO-C15CO");
        WhiteboardService.WhiteboardContext convContext = new WhiteboardService.WhiteboardContext(aclUrl, null, null, null);
        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(convContext);

        Uri keyUrl = Uri.parse("http://this-is-a-key");

        KeyObject mockKey = mock(KeyObject.class);
        when(mockKey.getKeyUrl()).thenReturn(keyUrl);
        when(keyManager.getCachedUnboundKey()).thenReturn(mockKey);

        Uri kmsUrl = Uri.parse("http://parent-kms-url/Object");
        when(whiteboardService.getParentkmsResourceObjectUrl()).thenReturn(kmsUrl);

        String kmsMessage = "I AM KMS";
        when(conversationProcessor.createNewResource(any(List.class), any(List.class))).thenReturn(kmsMessage);

        Call<Channel> call = mock(Call.class);
        when(whiteboardPersistenceClient.createChannel(anyBoolean(), anyBoolean(), any())).thenReturn(call);

        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(createData);

        // Make sure it never makes it past the return
        verify(whiteboardPersistenceClient, times(1)).createChannel(anyBoolean(), anyBoolean(), channelCaptor.capture());
        verify(whiteboardPersistenceClient, never()).createPrivateChannel(anyBoolean(), anyBoolean(), any());

        Channel channel = channelCaptor.getValue();

        System.out.println(channel.hashCode());

        assertEquals(aclUrl, channel.getAclUrlLink());
        assertEquals(keyUrl, channel.getDefaultEncryptionKeyUrl());
        assertEquals(kmsMessage, channel.getKmsMessage());
    }

    @Test
    public void testKeyTimeout() throws NoSuchFieldException, IllegalAccessException {

        when(sdkClient.supportsPrivateBoards()).thenReturn(true);
        when(keyManager.getCachedUnboundKey()).thenReturn(null);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(PRIVATE_CONTEXT));

        // Make sure it never makes it past the return
        verify(whiteboardService, never()).getPrivateStore();

        // Make sure it didn't make it into the other cases by accident either
        verify(whiteboardService, never()).getRemoteStore();

        // The timer should have started and there should be a create request
        assertTrue("An ongoing request mandates createData != null", whiteboardCreator.createRequesting());
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);

        assertFalse("Create data should be null after the timer completes", whiteboardCreator.createRequesting());

    }

    private WhiteboardCreator createWhiteboardCreator() {
        return new WhiteboardCreator(apiClientProvider, whiteboardService, schedulerProvider,
                                     whiteboardCache, keyManager, sdkClient,
                                     bus, conversationProcessor, authenticatedUserProvider, gson);
    }
}
