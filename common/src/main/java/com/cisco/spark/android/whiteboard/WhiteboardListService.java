package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.acl.AclLinkRequest;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.WhiteboardDeleteEvent;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import retrofit2.Response;
import rx.Observable;

public class WhiteboardListService implements Component {

    protected final EventBus bus;
    protected final Lazy<WhiteboardService> whiteboardService;
    protected final KeyManager keyManager;
    protected final SchedulerProvider schedulerProvider;
    protected final ApiClientProvider apiClientProvider;
    protected final EncryptedConversationProcessor conversationProcessor;
    protected final Injector injector;
    protected final Context context;
    protected final MetricsReporter metricsReporter;

    private WhiteboardSavingInConversationDelegate whiteboardSavingDelegate;

    protected int numberOfRemoteBoards;
    protected int numberOfLocalBoards;

    public WhiteboardListService(EventBus bus, Lazy<WhiteboardService> whiteboardService, KeyManager keyManager,
                                 SchedulerProvider schedulerProvider, ApiClientProvider apiClientProvider,
                                 EncryptedConversationProcessor conversationProcessor, Injector injector, Context context,
                                 MetricsReporter metricsReporter) {
        this.bus = bus;
        this.whiteboardService = whiteboardService;
        this.keyManager = keyManager;
        this.schedulerProvider = schedulerProvider;
        this.apiClientProvider = apiClientProvider;
        this.conversationProcessor = conversationProcessor;
        this.injector = injector;
        this.context = context;
        this.metricsReporter = metricsReporter;
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public int getNumberOfLocalBoards() {
        return numberOfLocalBoards;
    }

    public int getNumberOfRemoteBoards() {
        return numberOfRemoteBoards;
    }

    public void saveWhiteboardsInBoundConversation(List<Channel> boards) {
        whiteboardSavingDelegate = new WhiteboardSavingInConversationDelegate(whiteboardService.get(), schedulerProvider,
                apiClientProvider, conversationProcessor, injector, context, bus);
        //All boards must have at least: boardId and kmsResourceUrl
        for (final Channel board : boards) {
            whiteboardSavingDelegate.saveBoardInConversation(board, whiteboardService.get().getAclId(), whiteboardService.get().getRemoteStore(), new AbsRemoteWhiteboardStore.ClientCallback() {
                @Override
                public void onSuccess(Channel channel) {
                    whiteboardService.get().getPrivateStore().removeFromCache(board.getChannelId());
                    numberOfLocalBoards--;
                    numberOfRemoteBoards++;
                    bus.post(new WhiteboardChannelSavedEvent(board, true));
                }

                @Override
                public void onFailure(String errorMessage) {
                    bus.post(new WhiteboardChannelSavedEvent(board, false, errorMessage));
                }
            });
        }
    }

    public synchronized void loadBoardList(int channelsLimit) {
        Uri aclUrlLink = whiteboardService.get().getAclUrlLink();
        String aclUrl = aclUrlLink != null ? aclUrlLink.toString() : null;
        whiteboardService.get().getCurrentWhiteboardStore().loadWhiteboardList(aclUrl, channelsLimit, (items, link, isLocalStore, isFirstPage) -> whiteboardsLoaded(items, link, isLocalStore, isFirstPage));
    }

    public void loadNextPageBoards(String conversationId, String url) {
        if (conversationId == null) {
            whiteboardService.get().getPrivateStore().loadNextPageWhiteboards(conversationId, url, (items, link, isLocalStore, isFirstPage) -> whiteboardsLoaded(items, link, isLocalStore, isFirstPage));
        } else {
            whiteboardService.get().getRemoteStore().loadNextPageWhiteboards(conversationId, url, (items, link, isLocalStore, isFirstPage) -> whiteboardsLoaded(items, link, isLocalStore, isFirstPage));
        }
    }

    protected void whiteboardsLoaded(List<Channel> items, final String link, final boolean isLocalStore,
                                     final boolean isFirstPage) {
        if (isLocalStore) {
            numberOfLocalBoards = items.size();
        } else {
            numberOfRemoteBoards = items.size();
        }

        WhiteboardListReadyEvent event = new WhiteboardListReadyEvent(items, link, isLocalStore, isFirstPage);
        bus.post(event);
    }

    public interface WhiteboardListCallback {
        void loadWhiteboardsComplete(List<Channel> items, final String link, final boolean isLocalStore,
                                final boolean isFirstPage);
    }

    public void deleteWhiteboard(List<Channel> channels, String conversationId) {
        if (channels == null) {
            throw new IllegalArgumentException("channels shouldn't be null");
        }

        Observable.just(conversationId)
                .subscribeOn(schedulerProvider.computation())
                .subscribe(id -> {
                    List<Channel> deletedChannels = new ArrayList<>();
                     for (Channel c: channels) {
                         /* Enable this when server is ready
                         try {
                             Response lockRes = apiClientProvider.getWhiteboardPersistenceClient().lockChannel(c.getChannelId()).execute();
                             if (!lockRes.isSuccessful()) {
                                 sendWhiteboardDeleteMetrics("Lock_Failed", lockRes.isSuccessful(), lockRes.code(), lockRes.message());
                                 continue;
                             }
                             sendWhiteboardDeleteMetrics(null, lockRes.isSuccessful(), lockRes.code(), null);
                         } catch (IOException e) {
                             Ln.e(e, "lock channel failed");
                         }*/

                         String kmsMessage = conversationProcessor.removeParticipantUsingKmsMessagingApi(new KmsResourceObject(c.getKmsResourceUrl()), conversationId);
                         AclLinkRequest req = new AclLinkRequest(AclLinkRequest.AclLinkOperation.DELETE, c.getAclUrlLink(), kmsMessage);
                         try {
                             Response delRes = apiClientProvider.getAclClient().addOrRemoveAclLink(c.getBoardAclId(), req).execute();
                             if (!delRes.isSuccessful()) {
                                 sendWhiteboardDeleteMetrics("Delete_Failed", delRes.isSuccessful(), delRes.code(), delRes.message());
                                 continue;
                             }
                             whiteboardService.get().getCurrentWhiteboardStore().removeFromCache(c.getChannelId());
                             sendWhiteboardDeleteMetrics(null, delRes.isSuccessful(), delRes.code(), null);
                             deletedChannels.add(c);
                         } catch (IOException e) {
                             Ln.e(e, "remove channel failed");
                         }
                     }
                     bus.post(new WhiteboardDeleteEvent(channels.size() == deletedChannels.size(), channels, deletedChannels));
                 });
    }

    private void sendWhiteboardDeleteMetrics(String errorType, boolean isSuccessful, int httpStatusCode, String errorMsg) {
        MetricsReportRequest request = metricsReporter.newWhiteboardServiceMetricsBuilder()
                .reportWhiteboardDelete(errorType, isSuccessful, httpStatusCode, errorMsg)
                .build();
        metricsReporter.enqueueMetricsReport(request);
    }
}
