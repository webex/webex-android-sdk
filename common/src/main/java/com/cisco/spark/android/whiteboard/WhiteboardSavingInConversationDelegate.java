package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class WhiteboardSavingInConversationDelegate implements Component {

    private final WhiteboardService whiteboardService;
    private final SchedulerProvider schedulerProvider;
    private final ApiClientProvider apiClientProvider;
    private final EncryptedConversationProcessor conversationProcessor;
    private final Injector injector;
    private final Context context;
    private final EventBus bus;

    private Executor singleThreadedExecutor = Executors.newSingleThreadExecutor();


    public WhiteboardSavingInConversationDelegate(WhiteboardService whiteboardService,
                                                  SchedulerProvider schedulerProvider,
                                                  ApiClientProvider apiClientProvider,
                                                  EncryptedConversationProcessor conversationProcessor,
                                                  Injector injector,
                                                  Context context,
                                                  EventBus bus) {

        this.whiteboardService = whiteboardService;
        this.schedulerProvider = schedulerProvider;
        this.apiClientProvider = apiClientProvider;
        this.conversationProcessor = conversationProcessor;
        this.injector = injector;
        this.context = context;
        this.bus = bus;
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }
    }

    @Override
    public void stop() {
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }
    }

    private ConversationResolver getConversationResolver(String conversationId) {
        return ConversationContentProviderQueries.getConversationResolverById(context.getContentResolver(),
                                                                              conversationId, injector);
    }

    private Conversation getConversation(String conversationId) {
        if (Strings.isEmpty(conversationId)) {
            return null;
        }

        Conversation conversation = null;
        ConversationResolver resolver = getConversationResolver(conversationId);

        if (resolver == null) {
            try {
                Response<Conversation> response = apiClientProvider.getConversationClient().getConversation(conversationId).execute();
                if (response.isSuccessful()) {
                    conversation = response.body();
                } else {
                    Ln.w("Failed getting conversation " + LoggingUtils.toString(response));
                }
            } catch (IOException e) {
                Ln.w(e);
            }
        } else {
            conversation = new Conversation(conversationId);
            conversation.setAclUrl(resolver.getAclUrl());
            conversation.setKmsResourceObjectUrl(Uri.parse(resolver.getKmsResourceObjectUrl()));
        }

        return conversation;
    }

    public void saveBoardInConversation(final Channel channel, final String conversationId, final WhiteboardStore store,
                                        final AbsRemoteWhiteboardStore.ClientCallback clientCallback) {
        Observable.just(conversationId).subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                .map(new Func1<String, Conversation>() {
                    @Override
                    public Conversation call(String s) {
                        return getConversation(conversationId);
                    }
                }).subscribe(new Action1<Conversation>() {
            @Override
            public void call(Conversation conversation) {

                List<String> convKroUrl = new ArrayList<>();
                if (conversation.getKmsResourceObject() != null
                        && conversation.getKmsResourceObject().getUri() != null) {
                    String kmsObjectUrl = conversation.getKmsResourceObject().getUri().toString();
                    convKroUrl = Collections.singletonList(kmsObjectUrl);
                } else {
                    clientCallback.onFailure("Couldn't save board in conversation, KRO mising");
                    return;
                }

                KmsResourceObject boardKro = new KmsResourceObject(channel.getKmsResourceUrl());

                Channel patchedChannel = new Channel();
                patchedChannel.setChannelId(channel.getChannelId());
                patchedChannel.setKmsMessage(conversationProcessor.authorizeNewParticipantsUsingKmsMessagingApi(boardKro, convKroUrl));
                patchedChannel.setAclUrlLink(Uri.parse(conversation.getAclUrl()));

                store.patchChannel(patchedChannel, clientCallback);

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Ln.e(throwable);
                clientCallback.onFailure(throwable.getMessage());
            }
        });
    }

    public void removeSelfFromBoardAcl(final Channel channel, final String authenticatedUserId) {
        //FIXME the acl service should come from device registration
        if (Strings.isEmpty(channel.getAclUrl())) {
            Ln.w("This board is using deprecated pre-acl encryption, cannot delete");
            return;
        }
        String aclService = channel.getAclUrl().substring(0, channel.getAclUrl().lastIndexOf("acls/"));
        Uri aclServiceUrl = Uri.parse(aclService);
        whiteboardService.getDeviceRegistration().whitelist(aclServiceUrl);

        KmsResourceObject boardKro = new KmsResourceObject(channel.getKmsResourceUrl());
        String removeSelfKmsMessage = conversationProcessor.removeParticipantUsingKmsMessagingApi(boardKro, authenticatedUserId);
        WhiteboardKmsMessage kmsMessage = new WhiteboardKmsMessage(removeSelfKmsMessage);

        Call call = apiClientProvider.getAclClient(aclServiceUrl).removeUserFromAcl(channel.getBoardAclId(), authenticatedUserId, kmsMessage);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Ln.v("Self userid removed from board acl");
                    bus.post(new SelfRemovedFromAclEvent(true, channel.getChannelId()));
                } else {
                    Ln.e("Failed removing self from the board acl " + LoggingUtils.toString(response));
                    bus.post(new SelfRemovedFromAclEvent(false, channel.getChannelId()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Ln.e("Failed removing self from the board acl " + t.getMessage());
                bus.post(new SelfRemovedFromAclEvent(false, channel.getChannelId()));
            }
        });

    }
}
