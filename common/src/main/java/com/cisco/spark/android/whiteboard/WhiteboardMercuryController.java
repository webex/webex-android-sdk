package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistrationBinding;
import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistrationBindingItems;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.RegistrationRequestResource;
import com.cisco.spark.android.whiteboard.realtime.RealtimeMessage;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;

public class WhiteboardMercuryController {

    private MercuryClient primaryMercury;
    private MercuryClient whiteboardMercury;

    private final WhiteboardEncryptor whiteboardEncryptor;
    private final SchedulerProvider schedulerProvider;
    private final WhiteboardService whiteboardService;
    private final WhiteboardCache whiteboardCache;
    private final ApiClientProvider apiClientProvider;
    private final LocusDataCache locusDataCache;
    private final EventBus bus;
    private final Gson gson;
    private final JsonParser jsonParser;

    private Executor singleThreadedExecutor;

    private boolean initializingRealtime;
    private String currentChannelId;
    private boolean usePrimaryMercury;

    private List<PendingRTMessage> pendingMessages = new ArrayList<>();
    private List<String> registeredBoardIds = new ArrayList<>();

    private final Object pendingMessagesLock = new Object();
    private final Object currentLock = new Object();

    public WhiteboardMercuryController(WhiteboardEncryptor whiteboardEncryptor, SchedulerProvider schedulerProvider,
                                       WhiteboardService whiteboardService, WhiteboardCache whiteboardCache, ApiClientProvider apiClientProvider,
                                       LocusDataCache locusDataCache, EventBus bus, Gson gson) {

        this.whiteboardEncryptor = whiteboardEncryptor;
        this.schedulerProvider = schedulerProvider;
        this.whiteboardService = whiteboardService;
        this.whiteboardCache = whiteboardCache;
        this.apiClientProvider = apiClientProvider;
        this.locusDataCache = locusDataCache;
        this.gson = gson;
        this.bus = bus;
        this.jsonParser = new JsonParser();

        singleThreadedExecutor = Executors.newSingleThreadExecutor();
    }

    private void setCurrentChannelId(String channelId) {
        synchronized (currentLock) {
            currentChannelId = channelId;
        }
    }

    public void realtimeMessage(final JsonObject msg, final Channel channel) {

        if (whiteboardMercury == null) {
            if (channel != null) {
                synchronized (pendingMessagesLock) {
                    pendingMessages.add(new PendingRTMessage(msg, channel));
                }
            }
            return;
        }

        Observable.just(msg)
                  .subscribeOn(schedulerProvider.from(singleThreadedExecutor))
                  .map(boardId -> {

                      Uri keyUrl = channel.getDefaultEncryptionKeyUrl();
                      String encryptedBlob = whiteboardEncryptor.encrypt(gson.toJson(msg), keyUrl);
                      if (encryptedBlob == null) {
                          return null;
                      }

                      RealtimeMessage realtimeMessage = new RealtimeMessage(channel.getChannelId(), encryptedBlob, keyUrl);
                      return gson.toJson(realtimeMessage);
                  })
                  .subscribe(s -> {
                      if (s != null) {
                          whiteboardMercury.send(s);
                      }
                  }, Ln::e);

    }

    public void inCallMercuryInit(Channel channel) {
        if (channel != null && whiteboardMercury == null) {
            if (channel.isPrivateChannel() && locusDataCache.isInCall()) {
                initMercury(channel.getChannelId(), false);
            }
        }
    }

    public void mercuryEvent(WhiteboardActivityEvent event) {

        String data = whiteboardEncryptor.decryptContent(event.getPayload(), event.getEncryptionKeyUrl());
        if (data == null) {
            Ln.e("Unable to decrypt whiteboard realtime data");
            return;
        }
        JsonElement messageData = jsonParser.parse(data);
        if (messageData == null || !messageData.isJsonObject()) {
            Ln.e("Unable to parse whiteboard realtime data");
            return;
        }
        bus.post(new WhiteboardRealtimeEvent(messageData.getAsJsonObject(), event.getChannelId()));
    }

    public void initMercury(final String channelId, final boolean isLoadboard) {

        if (channelId == null) {
            Ln.e("Not registering because no channel ID");
            return;
        }

        synchronized (currentLock) {
            if (channelId.equals(currentChannelId) && whiteboardMercury != null && whiteboardMercury.isRunning()) {
                Ln.w("Received request to open another mercury connection for %s, ignoring", channelId);
                return;
            } else {
                stopMercury();
            }
        }

        Ln.i("Initialising mercury for channel %s %s", channelId, LoggingUtils.getCaller());
        if (whiteboardMercury == null && !initializingRealtime) {

            initializingRealtime = true;
            whiteboardService.boardStart();

            if (primaryMercury.getMercuryConnectionServiceClusterUrl() == null) {
                Ln.d("No connection service cluster url in primary mercury, using new mercury connection for whiteboard");
                registerWhiteboard(channelId, isLoadboard);
            } else {
                // Shared mercury
                Ln.d("Using primary shared mercury connection for whiteboard");
                addSharedBoardMercury(channelId);
            }
        } else if (whiteboardMercury != null) {
            if (whiteboardMercury.shouldStart() && !whiteboardMercury.isRunning()) {
                whiteboardMercury.start();
            }
            if (isLoadboard) {
                whiteboardService.loadBoardFromMercury(channelId);
            }
        }
    }

    public void clearPendingMessages() {
        synchronized (pendingMessagesLock) {
            pendingMessages.clear();
        }
    }

    private void bindingFailed(Response<DeviceRegistration> response) {
        String message;
        try {
            message = response.errorBody().string();
            Ln.w(message);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            initializingRealtime = false;
            setCurrentChannelId(null);
        }

        whiteboardService.boardReady();
    }

    public synchronized void stopMercury() {
        whiteboardCache.markCurrentRealtimeBoardStale();

        // Don't stop primary mercury connection
        if (whiteboardMercury == null) {
            Ln.w("Stopping whiteboard mercury connection");
            return;
        }

        if (whiteboardMercury != primaryMercury) {
            whiteboardMercury.stop();
        } else {
            removeSharedBoardMercury(currentChannelId);
        }
        whiteboardMercury = null;

        setCurrentChannelId(null);
    }

    public void setPrimaryMercury(MercuryClient primaryMercury) {
        this.primaryMercury = primaryMercury;
    }

    private void registerWhiteboard(String channelId, boolean isLoadboard) {
        String bindingId = sanitizeBinding(channelId);
        RegistrationRequestResource rrm = new RegistrationRequestResource(Collections.singletonList(bindingId));
        Call<DeviceRegistration> binding = apiClientProvider.getWhiteboardPersistenceClient().registerChannelId(rrm);
        binding.enqueue(new retrofit2.Callback<DeviceRegistration>() {
            @Override
            public void onResponse(Call<DeviceRegistration> call, retrofit2.Response<DeviceRegistration> response) {
                if (response.isSuccessful()) {
                    DeviceRegistration boardRegistration = response.body();
                    bindingSuccess(boardRegistration, channelId, isLoadboard);
                } else {
                    bindingFailed(response);
                    whiteboardService.boardConnectMercuryError(WhiteboardConstants.MERCURY_CONNECTION_FAILURE);
                }
            }

            @Override
            public void onFailure(Call<DeviceRegistration> call, Throwable t) {
                Ln.e("Failed to create secondary mercury connection");
                // handle execution failures like no internet connectivity
                //TODO Retrofit 2 failure
                initializingRealtime = false;
                whiteboardService.boardReady();
                whiteboardService.boardConnectMercuryError(WhiteboardConstants.MERCURY_CONNECTION_FAILURE);
            }
        });
    }

    private void startWhiteboardMercury(Uri webSocketUrl, String channelId) {
        Ln.d("Using new mercury connection for whiteboard");
        whiteboardMercury = whiteboardService.createWhiteboardMercuryClient();
        whiteboardMercury.setUriOverride(webSocketUrl);
        whiteboardMercury.start();
        Ln.d("Opened secondary mercury connection");
        setCurrentChannelId(channelId);
        initializingRealtime = false;
    }

    private void bindingSuccess(DeviceRegistration boardRegistration, String channelId, boolean isLoadboard) {

       Ln.d("Using new mercury connection for whiteboard");
       startWhiteboardMercury(boardRegistration.getWebSocketUrl(), channelId);

       if (isLoadboard) {
           whiteboardService.loadBoardFromMercury(channelId);
       } else {
           whiteboardService.boardReady();
       }

       synchronized (pendingMessagesLock) {
           for (PendingRTMessage pendingRTMessage : pendingMessages) {
               realtimeMessage(pendingRTMessage.getMessage(), pendingRTMessage.getChannel());
           }
           pendingMessages.clear();
       }
    }

    private void replaceSharedWhiteboardMercury(String channelId) {

        setCurrentChannelId(channelId);

        BoardRegistration replaceBoardRegistration = new BoardRegistration(null,
                primaryMercury.getMercuryConnectionServiceClusterUrl(),
                primaryMercury.getPrimaryMercuryWebSocketUrl(), BoardRegistration.Action.REPLACE);

        Call<BoardRegistration> boardReplaceRegistrationCall =
                apiClientProvider.getWhiteboardPersistenceClient().sharedMercuryBoardRegistration(channelId, replaceBoardRegistration);

        boardReplaceRegistrationCall.enqueue(new retrofit2.Callback<BoardRegistration>() {
            @Override
            public void onResponse(Call<BoardRegistration> call, Response<BoardRegistration> response) {
                if (response.isSuccessful()) {
                    BoardRegistration replaceBoardRegistration = response.body();

                    if (replaceBoardRegistration.getIsSharedWebSocket()) {
                        usePrimaryMercury = replaceBoardRegistration.getIsSharedWebSocket();
                        whiteboardMercury = primaryMercury;
                    } else {
                        startWhiteboardMercury(replaceBoardRegistration.getWebSocketUrl(), channelId);
                    }

                    initializingRealtime = false;
                } else {
                    Ln.e("Board registration replace is not successful");
                    initializingRealtime = false;
                    whiteboardService.boardConnectMercuryError(WhiteboardConstants.SHARED_MERCURY_REPLACE_FAILURE);
                }
                whiteboardService.boardReady();
            }

            @Override
            public void onFailure(Call<BoardRegistration> call, Throwable t) {
                Ln.e("Failed to replace shared mercury connection");
                initializingRealtime = false;
                whiteboardService.boardReady();
                whiteboardService.boardConnectMercuryError(WhiteboardConstants.SHARED_MERCURY_REPLACE_FAILURE);
            }
        });
    }

    private void addSharedBoardMercury(String channelId) {
        Ln.i("Start to add shared mercury for channel: %s", channelId);
        setCurrentChannelId(channelId);

        BoardRegistration addBoardRegistration = new BoardRegistration(null,
                primaryMercury.getMercuryConnectionServiceClusterUrl(),
                primaryMercury.getPrimaryMercuryWebSocketUrl(), BoardRegistration.Action.ADD);

        Call<BoardRegistration> boardAddRegistrationCall =
                apiClientProvider.getWhiteboardPersistenceClient().sharedMercuryBoardRegistration(channelId, addBoardRegistration);

        boardAddRegistrationCall.enqueue(new retrofit2.Callback<BoardRegistration>() {
            @Override
            public void onResponse(Call<BoardRegistration> call, Response<BoardRegistration> response) {
                if (response.isSuccessful()) {
                    BoardRegistration addBoardRegistration = response.body();

                    if (addBoardRegistration.getIsSharedWebSocket()) {
                        usePrimaryMercury = addBoardRegistration.getIsSharedWebSocket();
                        whiteboardMercury = primaryMercury;
                        registeredBoardIds.add(channelId);
                    } else {
                        startWhiteboardMercury(addBoardRegistration.getWebSocketUrl(), channelId);
                    }

                    initializingRealtime = false;
                    whiteboardService.boardReady();
                } else if (response.code() == 400) {
                    Ln.w("Add shared mercury failed for channel: %s, 400 error, may caused by registered more than two channels", channelId);
                    Observable.just(1)
                            .subscribeOn(schedulerProvider.newThread())
                            .map(m -> getRegisteredBoardBindingsSync()) // get bindings
                            .map(bindingItems -> removeRegisteredBindingsSync(bindingItems)) // compare & remove
                            .subscribe(s -> {
                                if (s.equals(WhiteboardConstants.SUCCESS)) {
                                    addSharedBoardMercury(channelId);
                                } else {
                                    Ln.e("Board registration get bindings/remove is not successful: %s", s);
                                    initializingRealtime = false;
                                    whiteboardService.boardConnectMercuryError(s);
                                }
                            });
                } else {
                    Ln.e("Board registration add is not successful");
                    initializingRealtime = false;
                    whiteboardService.boardConnectMercuryError(WhiteboardConstants.SHARED_MERCURY_ADD_FAILURE);
                }
            }

            @Override
            public void onFailure(Call<BoardRegistration> call, Throwable t) {
                Ln.e("Failed to add shared mercury connection");
                initializingRealtime = false;
                whiteboardService.boardReady();
                whiteboardService.boardConnectMercuryError(WhiteboardConstants.SHARED_MERCURY_ADD_FAILURE);
            }
        });
    }

    private void removeSharedBoardMercury(String channelId) {
        Ln.i("Remove shared board mercury for channel: %s", channelId);
        registeredBoardIds.remove(channelId);

        String bindingId = sanitizeBinding(channelId);
        BoardRegistration removeBoardRegistration = new BoardRegistration(bindingId,
                primaryMercury.getMercuryConnectionServiceClusterUrl(),
                primaryMercury.getPrimaryMercuryWebSocketUrl(), BoardRegistration.Action.REMOVE);
        Call<BoardRegistration> boardRemoveRegistrationCall =
                apiClientProvider.getWhiteboardPersistenceClient()
                        .sharedMercuryBoardRegistration(currentChannelId, removeBoardRegistration);

        boardRemoveRegistrationCall.enqueue(new retrofit2.Callback<BoardRegistration>() {
            @Override
            public void onResponse(Call<BoardRegistration> call, Response<BoardRegistration> response) {
                if (response.isSuccessful()) {
                    BoardRegistration removeBoardRegistration = response.body();
                    usePrimaryMercury = removeBoardRegistration.getIsSharedWebSocket();
                } else {
                    Ln.e("Board registration remove is not successful");
                }
            }

            @Override
            public void onFailure(Call<BoardRegistration> call, Throwable t) {
                Ln.e("Board registration remove failed");
            }
        });
    }

    @Nullable
    private BoardRegistrationBindingItems getRegisteredBoardBindingsSync() {
        try {
            Response<BoardRegistrationBindingItems> response =
                    apiClientProvider.getWhiteboardPersistenceClient().getBoardRegistrationBindings(primaryMercury.getPrimaryMercuryWebSocketUrl()).execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException e) {
            Ln.e("GetRegisteredBoardBindingsSync failed!");
        }
        return null;
    }

    private String removeRegisteredBindingsSync(BoardRegistrationBindingItems bindingItems) {
        if (bindingItems == null) {
            return WhiteboardConstants.SHARED_MERCURY_GET_REGISTRATION_BINDINGS_FAILURE;
        } else {
            Ln.d("Local registered boardIds counts: %s; remote registered boardIds counts: %s", registeredBoardIds.size(), bindingItems.getItems().size());
            for (BoardRegistrationBinding item : bindingItems.getItems()) {
                String[] parts = item.getChannelUrl().toString().split("/");
                String boardId = parts[parts.length - 1];
                if (!registeredBoardIds.contains(boardId)) {
                    Ln.d("Remove unlive registered board: %s", boardId);
                    String bindingId = sanitizeBinding(boardId);
                    BoardRegistration removeBoardRegistration = new BoardRegistration(bindingId,
                            primaryMercury.getMercuryConnectionServiceClusterUrl(),
                            primaryMercury.getPrimaryMercuryWebSocketUrl(), BoardRegistration.Action.REMOVE);
                    try {
                        Response<BoardRegistration> response =
                                apiClientProvider.getWhiteboardPersistenceClient().sharedMercuryBoardRegistration(boardId, removeBoardRegistration).execute();
                        if (response.isSuccessful()) {
                            return WhiteboardConstants.SUCCESS;
                        } else {
                            Ln.e("Remove registered bindings failed for channel: %s, non 200", boardId);
                            return WhiteboardConstants.SHARED_MERCURY_REMOVE_FAILURE;
                        }
                    } catch (IOException e) {
                        Ln.e("Remove registered bindings failed for channel: %s, %s", boardId, e.getMessage());
                        return WhiteboardConstants.SHARED_MERCURY_REMOVE_FAILURE;
                    }
                }
            }
            return WhiteboardConstants.SUCCESS;
        }
    }

    @NonNull
    private String sanitizeBinding(String channelID) {
        return "board." + channelID.replace('-', '.').replace('_', '#');
    }

    /**
     * The logic for this will be implemented when it exists in the board service. For now always use the secondary
     * connection.
     */
    public boolean usePrimaryMercury() {
        return usePrimaryMercury;
    }
}
