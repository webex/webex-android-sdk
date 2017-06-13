package com.cisco.spark.android.lyra;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.callcontrol.events.CallControlBindingEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.mercury.events.LyraActivityEvent;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.RoomSystemMetricsBuilder;
import com.cisco.spark.android.metrics.value.SpaceBindingMetricsValues;
import com.cisco.spark.android.model.AudioState;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.room.RoomLeftEvent;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.room.RoomUpdatedEvent;
import com.cisco.spark.android.room.model.RoomState;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.cisco.spark.android.sync.ConversationContentProviderOperation.clearBindingState;
import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateBindingState;

public class LyraService implements Component {
    private static final int CONVERSATION_BINDED_STATE = 1;

    private EventBus bus;
    private ApiClientProvider apiClientProvider;
    private transient EncryptedConversationProcessor conversationProcessor;
    private transient ContentResolver contentResolver;
    private RoomService roomService;
    private Executor singleThreadedExecutor = Executors.newSingleThreadExecutor();
    private BindingResponses bindingResponses;
    private boolean isGetBindingState;
    private RoomState roomState;
    private BindingBackend bindingBackend;
    private AudioState audioState;
    private Provider<Batch> batchProvider;
    private Context context;
    private DeviceRegistration deviceRegistration;
    private Injector injector;
    private ApiTokenProvider apiTokenProvider;
    private LocusDataCache locusDataCache;
    private final SchedulerProvider schedulerProvider;
    private MetricsReporter metricsReporter;

    private boolean isImplicitBinding;
    private boolean hasImplicitBound;

    public LyraService(EventBus eventBus, ApiClientProvider apiClientProvider,
                       EncryptedConversationProcessor conversationProcessor, ContentResolver contentResolver,
                       RoomService roomService, BindingBackend bindingBackend, DeviceRegistration deviceRegistration,
                       Context context, Injector injector, ApiTokenProvider apiTokenProvider,
                       LocusDataCache locusDataCache, Provider<Batch> batchProvider, SchedulerProvider schedulerProvider, MetricsReporter metricsReporter) {
        this.bus = eventBus;
        this.apiClientProvider = apiClientProvider;
        this.conversationProcessor = conversationProcessor;
        this.contentResolver = contentResolver;
        this.roomService = roomService;
        this.bindingBackend = bindingBackend;
        this.batchProvider = batchProvider;
        this.context = context;
        this.deviceRegistration = deviceRegistration;
        this.injector = injector;
        this.apiClientProvider = apiClientProvider;
        this.apiTokenProvider = apiTokenProvider;
        this.locusDataCache = locusDataCache;
        this.schedulerProvider = schedulerProvider;
        this.metricsReporter = metricsReporter;
    }

    public AudioState getAudioState() {
        return audioState;
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

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }

    public void bind(final Uri conversationUrl, final String roomIdentity, final String conversationId) {
        if ((conversationUrl == null) || (roomIdentity == null) || (conversationId == null)) {
            Ln.w("bind fail for null parameter");
            return;
        }

        Observable.just(roomIdentity)
                .subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return getAddKmsMessage(roomIdentity, conversationId);
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        bindingBackend.bind(roomIdentity, new BindingRequest(conversationUrl, s), new BindingCallback() {
                            @Override
                            public void onSuccess() {
                                onBindingCallback(LyraBindingEvent.LYRA_BINDING, true);
                            }

                            @Override
                            public void onError() {
                                onBindingCallback(LyraBindingEvent.LYRA_BINDING, false);
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Ln.e(throwable);
                    }
                });
    }

    public void unbind(final Uri conversationUrl, final String roomIdentity, final String conversationId) {
        if ((conversationUrl == null) || (roomIdentity == null) || (conversationId == null)) {
            Ln.w("unbind fail for null parameter");
            return;
        }
        Observable.just(roomIdentity)
                .subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return getDeleteKmsMessage(roomIdentity, conversationId);
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        bindingBackend.unbind(roomIdentity, getBindingUrlId(conversationUrl), s, new BindingCallback() {
                            @Override
                            public void onSuccess() {
                                onBindingCallback(LyraBindingEvent.LYRA_UNBINDING, true);
                            }

                            @Override
                            public void onError() {
                                onBindingCallback(LyraBindingEvent.LYRA_UNBINDING, false);
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Ln.e(throwable);
                    }
                });
    }

    private void onBindingCallback(@LyraBindingEvent.Events int event, boolean result) {
        switch (event) {
            case LyraBindingEvent.LYRA_BINDING:
                if (isImplicitBinding) {
                    if (result) {
                        hasImplicitBound = true;
                    } else {
                        clearImplicitBindingFlag();
                    }
                    reportImplicitBindingMetrics(SpaceBindingMetricsValues.BindingMetricValue.BIND, result);
                } else {
                    postLyraBindingEvent(LyraBindingEvent.LYRA_BINDING, result);
                }
                break;
            case LyraBindingEvent.LYRA_UNBINDING:
                if (isImplicitBinding) {
                    clearImplicitBindingFlag();
                    reportImplicitBindingMetrics(SpaceBindingMetricsValues.BindingMetricValue.UNBIND, result);
                } else {
                    postLyraBindingEvent(LyraBindingEvent.LYRA_BINDING, result);
                }
                break;
        }
    }


    public void updateBindings(final String roomIdentity) {
        if (isGetBindingState || (roomIdentity == null)) {
            Ln.w("updateBindings fail for null parameter or updating");
            return;
        }
        isGetBindingState = true;
        bindingBackend.updateBindings(roomIdentity, new UpdateBindingCallback() {
            @Override
            public void onSuccess(BindingResponses bindingResponses) {
                LyraService.this.bindingResponses = bindingResponses;
                postLyraBindingEvent(LyraBindingEvent.LYRA_GETSTATE, true);
                isGetBindingState = false;
            }

            @Override
            public void onError() {
                LyraService.this.bindingResponses = null;
                postLyraBindingEvent(LyraBindingEvent.LYRA_GETSTATE, false);
                isGetBindingState = false;
            }
        });
    }

    private  String getAddKmsMessage(String roomIdentity, String conversationId) {
        Set<String> userIds = new HashSet<>();
        userIds.add(roomIdentity);
        KmsResourceObject kro = ConversationContentProviderQueries.getKmsResourceObject(
                contentResolver, conversationId);
        return conversationProcessor.authorizeNewParticipantsUsingKmsMessagingApi(kro, userIds);
    }
    private  String getDeleteKmsMessage(String roomIdentity, String conversationId) {
        KmsResourceObject kro = ConversationContentProviderQueries.getKmsResourceObject(
                contentResolver, conversationId);
        return conversationProcessor.removeParticipantUsingKmsMessagingApi(kro, roomIdentity);
    }

    private void postLyraBindingEvent(@LyraBindingEvent.Events int event, boolean result) {
        LyraBindingEvent lyraBindingEvent = new LyraBindingEvent(event, result);
        bus.post(lyraBindingEvent);
    }

    private void postLyraAudioEvent(@LyraAudioEvent.Events int event) {
        LyraAudioEvent lyraAudioEvent = new LyraAudioEvent(event, audioState);
        bus.post(lyraAudioEvent);
    }

    public Uri getBindingUrl(Uri conversationUrl) {
        if ((bindingResponses == null) || (conversationUrl == null)) {
            Ln.w("No binding result or null conversationUrl");
            return null;
        }

        for (BindingResponse bindingResponse : bindingResponses.getItems()) {
            if (bindingResponse.getConversationUrl().equals(conversationUrl)) {
                return bindingResponse.getBindingUrl();
            }
        }

        return null;
    }

    private String getBindingUrlId(Uri conversationUrl) {

        Uri bindingUrl = getBindingUrl(conversationUrl);

        if (bindingUrl != null) {
            String bindingString = bindingUrl.toString();
            return bindingString != null ? bindingString.substring(bindingString.lastIndexOf('/') + 1) : null;
        }

        return null;
    }

    public void getBindingState(final String roomIdentity) {
        if (TextUtils.isEmpty(roomIdentity)) {
            Ln.w("getBindingState fail for null parameter");
            return;
        }
        bindingBackend.updateBindings(roomIdentity, new UpdateBindingCallback() {
            @Override
            public void onSuccess(BindingResponses bindingResponses) {
                final LyraBindingEvent lyraBindingEvent = new LyraBindingEvent(LyraBindingEvent.LYRA_GETSTATE, true);
                if (bindingResponses != null && bindingResponses.getItems().size() > 0) {
                    for (BindingResponse bindingResponse : bindingResponses.getItems()) {
                        final String conversationUrl = bindingResponse.getConversationUrl().toString();
                        if (!TextUtils.isEmpty(conversationUrl)) {
                            lyraBindingEvent.setConversationUrl(conversationUrl);
                        }
                        Thread thread = new Thread() {
                            public void run() {
                                if (!TextUtils.isEmpty(conversationUrl)) {
                                    String conversationId = conversationUrl.substring(conversationUrl.lastIndexOf('/') + 1);
                                    updateBindConversationState(conversationId, CONVERSATION_BINDED_STATE);
                                }
                                bus.post(lyraBindingEvent);
                            }
                        };
                        thread.start();
                    }
                } else {
                    clearBindStateInBackground();
                    bus.post(lyraBindingEvent);
                }
            }

            @Override
            public void onError() {
                Ln.w("getBindingState fail");
            }
        });
    }

    private void updateBindConversationState(String conversationId, int bindState) {
        Ln.i("update conversation bind state, conversationId = " + conversationId + ", value = " + bindState);
        try {
            Batch batch = batchProvider.get();
            batch.add(clearBindingState());
            batch.add(updateBindingState(conversationId, bindState));
            batch.apply();
        } catch (Exception ex) {
            Ln.e(ex, "Failed to set 'bind state' field to '%b', for bindState: %s", bindState, conversationId);
        }
    }

    private void clearBindConversationState() {
        try {
            Batch batch = batchProvider.get();
            batch.add(clearBindingState());
            batch.apply();
        } catch (Exception ex) {
            Ln.e(ex, "Failed to clear bind state");
        }
    }

    public void clearBindStateInBackground() {
        Ln.i("Clear conversation bind state");
        Thread thread = new Thread() {
            public void run() {
                clearBindConversationState();
            }
        };
        thread.start();
    }

    public void onEventMainThread(RoomUpdatedEvent event) {
        if (event.getRoomState() == RoomUpdatedEvent.RoomState.PAIRED) {
            if (roomState == null) {
                roomState = roomService.getRoomState();
                postLyraBindingEvent(LyraBindingEvent.LYRA_ROOM_UPDATE, true);
                queryAudioState(roomState.getRoomIdentity().toString());
                return;
            }

            RoomState roomState = roomService.getRoomState();

            if (!this.roomState.getRoomIdentity().equals(roomState.getRoomIdentity()) ||  !this.roomState.getRoomName().equals(roomState.getRoomName())) {
                this.roomState = roomState;
                postLyraBindingEvent(LyraBindingEvent.LYRA_ROOM_UPDATE, true);
                queryAudioState(roomState.getRoomIdentity().toString());
            }
        }
    }

    public void onEvent(RoomUpdatedEvent event) {
        if (event.getRoomState() == RoomUpdatedEvent.RoomState.PAIRED) {
            RoomState roomState = roomService.getRoomState();
            if (roomState != null) {
                updateBindings(roomState.getRoomIdentity().toString());
            }
        }
    }

    private void queryAudioState(final String roomId) {
        apiClientProvider.getLyraClient().getAudioState(roomId).enqueue(new Callback<AudioStateResponse>() {
            @Override
            public void onResponse(Call<AudioStateResponse> call, Response<AudioStateResponse> response) {
                audioState = new AudioState(roomId);
                if (response.isSuccessful() && response.body() != null) {
                    audioState.setIsMuted(response.body().getMicrophones().isMuted());
                    audioState.setVolumeLevel(response.body().getVolume().getLevel());
                    audioState.setVolumeStep(response.body().getVolume().getStep());
                    audioState.setMaxVolumeLevel(response.body().getVolume().getMax());
                    audioState.setMinVolumeLevel(response.body().getVolume().getMin());
                    postLyraAudioEvent(LyraAudioEvent.LYRA_AUDIO_UPDATE);
                }
            }

            @Override
            public void onFailure(Call<AudioStateResponse> call, Throwable t) {
                Ln.e(t, "Failed to get audio state in LyraService.");
            }
        });
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(RoomLeftEvent event) {
        roomState = null;
        postLyraBindingEvent(LyraBindingEvent.LYRA_ROOM_LEFT, true);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LyraActivityEvent event) {
        if (roomState != null) {
            queryAudioState(roomState.getRoomIdentity().toString());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CallControlBindingEvent event) {
        Ln.d("LyraService.onEvent(CallControlBindingEvent)");
        Locus locus = event.getLocus();
        if (!isValidLocusForBind(locus))
            return;
        switch (event.getType()) {
            case BIND:
                implicitBindForCall(locus.getConversationUrl());
                break;
            case UNBIND:
                if (shouldImplicitUnbind(event))
                    implicitUnbindForCall(locus.getConversationUrl());
            break;
        }
    }

    public void implicitBindForCall(final String conversationUrl) {
        final Uri conversationUri = Uri.parse(conversationUrl);
        RoomState roomState = roomService.getRoomState();
        String roomIdentity = roomState != null ? String.valueOf(roomState.getRoomIdentity()) : null;
        bindingBackend.updateBindings(roomIdentity, new UpdateBindingCallback() {
            @Override
            public void onSuccess(BindingResponses bindingResponses) {
                LyraService.this.bindingResponses = bindingResponses;
                    if (bindingResponses == null)
                        return;

                    if (isValidateForBind(conversationUri)) {
                        isImplicitBinding = true;
                        bind(conversationUri,  getRoomIdentity(), getConversationIdFromConversationUrl(conversationUrl));
                    }
            }

            @Override
            public void onError() {
                Ln.w("getBindingState fail");
            }
        });
    }

    public void implicitUnbindForCall(final  String conversationUrl) {
        final Uri conversationUri = Uri.parse(conversationUrl);
        RoomState roomState = roomService.getRoomState();
        String roomIdentity = roomState != null ? String.valueOf(roomState.getRoomIdentity()) : null;
        bindingBackend.updateBindings(roomIdentity, new UpdateBindingCallback() {
            @Override
            public void onSuccess(BindingResponses bindingResponses) {
                LyraService.this.bindingResponses = bindingResponses;
                if (bindingResponses == null || bindingResponses.getItems().size() == 0 || !hasBoundWithRoom(conversationUri)) {
                    clearImplicitBindingFlag();
                    return;
                }
                if (isImplicitBinding && hasImplicitBound) {
                    unbind(conversationUri, getRoomIdentity(), getConversationIdFromConversationUrl(conversationUrl));
                }
            }

            @Override
            public void onError() {
                Ln.w("getBindingState fail");
            }
        });
    }

    public boolean getAvailableForBinding() {
        if (bindingResponses == null) {
            return false;
        }
        return bindingResponses.getAvailableForBinding();
    }

    public BindingResponses getBindingResponses() {
        return bindingResponses;
    }

    public Boolean isValidateForBind(Uri conversationUri) {
        if (!roomService.isInRoom())
            return false;
        if (!deviceRegistration.getFeatures().isRoomBindingEnabled())
            return false;
        if (!getAvailableForBinding())
            return false;
        if (hasBoundWithRoom(conversationUri))
            return false;
        return true;
    }

    public boolean hasBoundWithRoom(Uri conversationUrl) {
        return getBindingUrl(conversationUrl) != null ? true : false;
    }

    public boolean hasDevicesBoundWithRoom() {
        for (BindingResponse bindingResponse : bindingResponses.getItems()) {
            if (bindingResponse.getBindingUrl() != null) {
                return true;
            }
        }
        return false;
    }

    public String getRoomIdentity() {
        RoomState roomState = roomService.getRoomState();
        if (roomState != null) {
            return  roomState.getRoomIdentity() != null ? roomState.getRoomIdentity().toString() : null;
        }
        return null;
    }

    private String getConversationIdFromConversationUrl(String conversationUrl) {
        String conversationId = conversationUrl != null ? conversationUrl.substring(conversationUrl.lastIndexOf('/') + 1) : null;
        return  conversationId;
    }

    private boolean hasModerator(String conversationId) {
        int moderatorCount = ConversationContentProviderQueries.getConversationModeratorCount(contentResolver, conversationId);
        return moderatorCount > 0;
    }

    private boolean isValidLocusForBind(Locus locus) {
        if (locus == null)
            return false;
        if (locus.getConversationUrl() == null)
            return false;
// Temporarily remove this condition for server side locus bug, will add it back after the bug are fixed
//https://sqbu-github.cisco.com/WebExSquared/Locus-Issues/issues/85
//        if (locusData.getLocus().isSelfScreenShareWithoutCall())
//            return false;

        return true;
    }

    private boolean shouldImplicitUnbind(CallControlBindingEvent event) {
        if (!isImplicitBinding || !hasImplicitBound) {
            return false;
        }
        if (!roomService.isInRoom()) {
            clearImplicitBindingFlag();
            return false;
        }
        for (LocusParticipant participant : event.getLeftParticipants()) {
            if (participant.getPerson().getId().equals(getRoomIdentity())) {
                return true;
            }
        }
        return false;
    }

    private void clearImplicitBindingFlag() {
        if (isImplicitBinding) {
            isImplicitBinding = false;
            hasImplicitBound = false;
        }
    }

    private void reportImplicitBindingMetrics(String lyraBindingType, boolean isSuccess) {
        RoomSystemMetricsBuilder roomSystemMetricsBuilder = new RoomSystemMetricsBuilder(metricsReporter.getEnvironment());
        SpaceBindingMetricsValues.BindingMetricValue bindingMetricValue = new SpaceBindingMetricsValues.BindingMetricValue(lyraBindingType, isSuccess);
        metricsReporter.enqueueMetricsReport(roomSystemMetricsBuilder
                .addSpaceImplicitBinding(bindingMetricValue)
                .build());
    }

}
