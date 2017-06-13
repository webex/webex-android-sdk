package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.client.ConversationClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.FeatureToggle;
import com.cisco.spark.android.wdm.Features;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Response;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import static com.cisco.spark.android.wdm.Features.ANDROID_WHITEBOARD_WITH_ACL;
import static junit.framework.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16, manifest = Config.NONE)
public class WhiteboardCreatorTest {

    @Mock EncryptedConversationProcessor conversationProcessor;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;
    @Mock DeviceRegistration deviceRegistration;
    @Mock ApiClientProvider apiClientProvider;
    @Mock WhiteboardService whiteboardService;
    @Mock SchedulerProvider schedulerProvider;
    @Mock KeyManager keyManager;
    @Mock SdkClient sdkClient;
    @Mock Injector injector;
    @Mock Context context;
    @Mock EventBus bus;

    private Features features;
    private TestScheduler testScheduler;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);

        testScheduler = new TestScheduler();

        // Always need to do this in unit tests
        when(schedulerProvider.from(any(Executor.class))).thenReturn(Schedulers.immediate());
        when(schedulerProvider.computation()).thenReturn(testScheduler);

        features = new Features();
        when(deviceRegistration.getFeatures()).thenReturn(features);

        AuthenticatedUser mockUser = mock(AuthenticatedUser.class);
        when(authenticatedUserProvider.getAuthenticatedUserOrNull()).thenReturn(mockUser);
        when(mockUser.getUserId()).thenReturn("myId");
    }

    @Test
    public void testPrivateCreateNoKeysStops() {

        when(sdkClient.supportsPrivateBoards()).thenReturn(true);
        when(keyManager.getCachedUnboundKey()).thenReturn(null);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(null, null, null));

        // Make sure it never makes it past the return
        verify(whiteboardService, never()).getPrivateStore();

        // Make sure it didn't make it into the other cases by accident either
        verify(whiteboardService, never()).getRemoteStore();
    }

    @Test
    public void testPrivateWithKeys() {

        KeyObject canHazYourKeyz = mock(KeyObject.class);
        Uri keyUrl = Uri.parse("http://kmskmskms.com/KEYYZZZZZ");
        when(canHazYourKeyz.getKeyUrl()).thenReturn(keyUrl);

        String kmsMessage = "I AM KMS";
        WhiteboardStore store = mock(WhiteboardStore.class);

        when(sdkClient.supportsPrivateBoards()).thenReturn(true);
        when(keyManager.getCachedUnboundKey()).thenReturn(canHazYourKeyz);
        when(conversationProcessor.createNewResource(any(List.class), any(List.class))).thenReturn(kmsMessage);
        when(whiteboardService.getPrivateStore()).thenReturn(store);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(null, null, null));

        // Make sure it never makes it past the return
        verify(whiteboardService, times(1)).getPrivateStore();
        verify(whiteboardService, never()).getRemoteStore();

        ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
        verify(store, times(1)).createChannel(captor.capture());

        Channel channel = captor.getValue();

        assertNotNull(channel.getKmsMessage());
        assertEquals(keyUrl, channel.getDefaultEncryptionKeyUrl());
    }

    @Test
    public void testConversationIdWithKeyUrl() {

        WhiteboardStore store = mock(WhiteboardStore.class);
        when(whiteboardService.getRemoteStore()).thenReturn(store);
        features.setDeveloperFeature(new FeatureToggle(ANDROID_WHITEBOARD_WITH_ACL, "false", true));

        String conversationId = "http://conv-conv.com/C15CO-C15CO-C15CO";
        Uri keyUrl = Uri.parse("http://kms-kms.com/C15CO-C15CO-C15CO");
        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(conversationId, keyUrl, null);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(createData);

        ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
        verify(store, times(1)).createChannel(captor.capture());

        Channel channel = captor.getValue();

        assertEquals(conversationId, channel.getAclUrl());
        assertEquals(keyUrl, channel.getDefaultEncryptionKeyUrl());
    }

    @Test
    public void testConversationIdNoKeyUrl() throws IOException {

        // This uses the flow for getting the key URL from the api rather than the DB because static

        WhiteboardStore store = mock(WhiteboardStore.class);
        when(whiteboardService.getRemoteStore()).thenReturn(store);
        features.setDeveloperFeature(new FeatureToggle(ANDROID_WHITEBOARD_WITH_ACL, "false", true));

        Conversation conv = mock(Conversation.class);

        // Mock the HTTP lookup
        ConversationClient convClient = mock(ConversationClient.class);
        Call<Conversation> mockCall = mock(Call.class);
        Response<Conversation> response = Response.success(conv);

        when(apiClientProvider.getConversationClient()).thenReturn(convClient);
        when(convClient.getConversation(any(String.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);

        Uri keyUrl = Uri.parse("http://i-am-a-conv.key");

        // This is more interesting
        when(conv.getDefaultActivityEncryptionKeyUrl()).thenReturn(keyUrl);

        String conversationId = "http://conv-conv.com/C15CO-C15CO-C15CO";
        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(conversationId, null, null);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(createData);

        ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
        verify(store, times(1)).createChannel(captor.capture());

        Channel channel = captor.getValue();

        assertEquals(conversationId, channel.getAclUrl());
        assertEquals(keyUrl, channel.getDefaultEncryptionKeyUrl());
    }

    @Test
    public void testAclUrlNoKeys() {

        WhiteboardStore store = mock(WhiteboardStore.class);
        when(whiteboardService.getRemoteStore()).thenReturn(store);
        features.setDeveloperFeature(new FeatureToggle(ANDROID_WHITEBOARD_WITH_ACL, "true", true));

        Uri aclUrl = Uri.parse("http://acl-acl.com/C15CO-C15CO-C15CO");
        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(null, null, aclUrl);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(createData);

        when(keyManager.getCachedUnboundKey()).thenReturn(null);

        verify(whiteboardService, never()).getRemoteStore();
    }

    @Test
    public void testAclUrlWithKeysContinue() {

        WhiteboardStore store = mock(WhiteboardStore.class);
        when(whiteboardService.getRemoteStore()).thenReturn(store);
        features.setDeveloperFeature(new FeatureToggle(ANDROID_WHITEBOARD_WITH_ACL, "true", true));

        String conversationId = "http://conv-id.com/HELOO";
        Uri aclUrl = Uri.parse("http://acl-acl.com/C15CO-C15CO-C15CO");
        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(conversationId, null, aclUrl);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();

        Uri keyUrl = Uri.parse("http://this-is-a-key");

        KeyObject mockKey = mock(KeyObject.class);
        when(mockKey.getKeyUrl()).thenReturn(keyUrl);
        when(keyManager.getCachedUnboundKey()).thenReturn(mockKey);

        Uri kmsUrl = Uri.parse("http://parent-kms-url/Object");
        when(whiteboardService.getParentkmsResourceObjectUrl()).thenReturn(kmsUrl);

        String kmsMessage = "I AM KMS";
        when(conversationProcessor.createNewResource(any(List.class), any(List.class))).thenReturn(kmsMessage);

        whiteboardCreator.createBoard(createData);
        verify(whiteboardService, times(1)).getRemoteStore();

        ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
        verify(store, times(1)).createChannel(captor.capture());

        Channel channel = captor.getValue();

        assertEquals(aclUrl, channel.getAclUrlLink());
        assertEquals(keyUrl, channel.getDefaultEncryptionKeyUrl());
        assertEquals(kmsMessage, channel.getKmsMessage());
    }

    @Test
    public void testKeyTimeout() throws NoSuchFieldException, IllegalAccessException {

        when(sdkClient.supportsPrivateBoards()).thenReturn(true);
        when(keyManager.getCachedUnboundKey()).thenReturn(null);

        WhiteboardCreator whiteboardCreator = createWhiteboardCreator();
        whiteboardCreator.createBoard(new WhiteboardCreator.CreateData(null, null, null));

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
        return new WhiteboardCreator(deviceRegistration, apiClientProvider, whiteboardService, schedulerProvider, keyManager, sdkClient,
                                     bus, conversationProcessor, injector, context, authenticatedUserProvider) {

            @Override
            public ConversationResolver getConversationResolver(String conversationId) {
                // FIXME Work out what on earth to do with this
                return null;
            }
        };
    }
}
