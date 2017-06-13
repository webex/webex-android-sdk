package com.cisco.spark.android.whiteboard;


import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

import de.greenrobot.event.EventBus;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16, manifest = Config.NONE)
public class WhiteboardShareDelegateTest {

    @Mock EncryptedConversationProcessor conversationProcessor;
    @Mock CallControlService callControlService;
    @Mock DeviceRegistration deviceRegistration;
    @Mock ApiClientProvider apiClientProvider;
    @Mock WhiteboardService whiteboardService;
    @Mock LocusDataCache locusDataCache;
    @Mock LocusService locusService;
    @Mock KeyManager keyManager;
    @Mock EventBus eventBus;

    @Before
    public void reset() {
        // Clear everything between tests
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStartStop() {

        WhiteboardShareDelegate shareDelegate = createShareDelegate();

        when(eventBus.isRegistered(any())).thenReturn(false);

        shareDelegate.start();
        verify(eventBus, times(1)).register(shareDelegate);

        when(eventBus.isRegistered(any())).thenReturn(true);

        shareDelegate.stop();
        verify(eventBus, times(1)).unregister(shareDelegate);
    }

    @Test
    public void testNullShare() {

        WhiteboardShareDelegate shareDelegate = createShareDelegate();

        // presumably this shouldn't just log
        shareDelegate.share(null, null);
        verify(callControlService, never()).shareWhiteboard(anyString());
    }

    @Test
    public void testNoCallShare() {

        when(locusDataCache.isInCall()).thenReturn(false);
        WhiteboardShareDelegate shareDelegate = createShareDelegate();

        String validChannelId = UUID.randomUUID().toString();
        String validAclId = UUID.randomUUID().toString();

        Channel validChannel = mock(Channel.class);
        when(validChannel.getChannelId()).thenReturn(validChannelId);

        shareDelegate.share(validChannel, validAclId);
        verify(callControlService, never()).shareWhiteboard(anyString());
    }

    public void testAdHocShare() {

        WhiteboardShareDelegate shareDelegate = createShareDelegate();

        // Set up the call
        setupCall(null);

        // Nulls
        shareDelegate.share(null, null);
        verify(callControlService, never()).shareWhiteboard(anyString()); // The locus doesn't have a conversation

        String validChannelId = UUID.randomUUID().toString();
        String validAclId = UUID.randomUUID().toString();

        Channel validChannel = mock(Channel.class);
        when(validChannel.getChannelId()).thenReturn(validChannelId);

        shareDelegate.share(validChannel, validAclId);
        verify(callControlService, never()).shareWhiteboard(anyString()); // The locus doesn't have a conversation
    }

    @Test
    public void testConversationShare() {

        WhiteboardShareDelegate shareDelegate = createShareDelegate();

        // Set up the call
        String conversationID = UUID.randomUUID().toString();
        setupCall(conversationID);

        // Nulls
        shareDelegate.share(null, null);
        verify(callControlService, never()).shareWhiteboard(anyString()); // The locus doesn't have a conversation

        String validChannelId = UUID.randomUUID().toString();

        Channel validChannel = mock(Channel.class);
        when(validChannel.getChannelId()).thenReturn(validChannelId);

        // Valid
        shareDelegate.share(validChannel, conversationID);
        verify(callControlService, never()).shareWhiteboard(anyString()); // The locus doesn't have a conversation

    }

    private void setupCall(String conversationId) {

        LocusKey key = LocusKey.fromString("http://fake.locus.key");
        Locus mockLocus = mock(Locus.class);
        LocusData mockLocusData = mock(LocusData.class);
        when(mockLocusData.getLocus()).thenReturn(mockLocus);
        when(mockLocus.getConversationUrl()).thenReturn(conversationId);

        when(locusDataCache.isInCall()).thenReturn(true);
        when(locusDataCache.getActiveLocus()).thenReturn(key);
        when(locusDataCache.getLocusData(key)).thenReturn(mockLocusData);
    }

    private WhiteboardShareDelegate createShareDelegate() {
        return new WhiteboardShareDelegate(conversationProcessor, locusDataCache, callControlService, deviceRegistration, apiClientProvider,
                                           whiteboardService, locusService, keyManager, eventBus);
    }
}
