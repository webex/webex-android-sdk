package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.BoardRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.RegistrationRequestResource;
import com.cisco.spark.android.whiteboard.realtime.RealtimeMessage;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
import rx.functions.Action1;
import rx.functions.Func1;

public class WhiteboardMercuryController {

    private MercuryClient primaryMercury;
    private MercuryClient whiteboardMercury;

    private final WhiteboardEncryptor whiteboardEncryptor;
    private final SchedulerProvider schedulerProvider;
    private final WhiteboardService whiteboardService;
    private final ApiClientProvider apiClientProvider;
    private final LocusDataCache locusDataCache;
    private final EventBus bus;
    private final Gson gson;

    private Executor singleThreadedExecutor;

    private boolean initializingRealtime;
    private String currentChannelId;
    private boolean usePrimaryMercury;

    private List<PendingRTMessage> pendingMessages = new ArrayList<>();
    private final Object pendingMessagesLock = new Object();

    public WhiteboardMercuryController(WhiteboardEncryptor whiteboardEncryptor, SchedulerProvider schedulerProvider,
                                       WhiteboardService whiteboardService, ApiClientProvider apiClientProvider,
                                       LocusDataCache locusDataCache, EventBus bus, Gson gson) {

        this.whiteboardEncryptor = whiteboardEncryptor;
        this.schedulerProvider = schedulerProvider;
        this.whiteboardService = whiteboardService;
        this.apiClientProvider = apiClientProvider;
        this.locusDataCache = locusDataCache;
        this.gson = gson;
        this.bus = bus;

        singleThreadedExecutor = Executors.newSingleThreadExecutor();
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
                  .map(new Func1<JsonObject, String>() {
                      @Override
                      public String call(JsonObject boardId) {

                          Uri keyUrl = channel.getDefaultEncryptionKeyUrl();
                          String encryptedBlob = whiteboardEncryptor.encrypt(gson.toJson(msg.get("data")), keyUrl);
                          if (encryptedBlob == null) {
                              return null;
                          }

                          RealtimeMessage realtimeMessage = new RealtimeMessage(channel.getChannelId(), encryptedBlob, keyUrl);
                          return gson.toJson(realtimeMessage);
                      }
                  })
                  .subscribe(new Action1<String>() {
                      @Override
                      public void call(String s) {
                          if (s != null) {
                              whiteboardMercury.send(s);
                          }
                      }
                  }, new Action1<Throwable>() {
                      @Override
                      public void call(Throwable throwable) {
                          Ln.e(throwable);
                      }
                  });

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

        if (!TextUtils.isEmpty(data)) {
            bus.post(new WhiteboardRealtimeEvent(data));
        }
    }


    public void initMercury(final String channelId, final boolean isLoadboard) {

        Ln.d("Initialising mercury for channel %s", channelId);
        if (whiteboardMercury == null && !initializingRealtime) {
            if (channelId == null) {
                Ln.e("Not registering because no channel ID");
                return;
            }

            initializingRealtime = true;
            whiteboardService.boardStart();

            if (primaryMercury.getMercuryConnectionServiceClusterUrl() == null) {
                Ln.d("No connection service cluster url in primary mercury, using new mercury connection for whiteboard");
                registerWhiteboard(channelId, isLoadboard);
            } else {
                // Shared mercury
                Ln.d("Using primary shared mercury connection for whiteboard");
                replaceSharedWhiteboardMercury(channelId);
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
        }
        whiteboardService.boardReady();
    }

    public synchronized void stopMercury() {
        // Don't stop primary mercury connection
        if (whiteboardMercury == null)
            return;

        if (whiteboardMercury != primaryMercury) {
            whiteboardMercury.stop();
        } else {
            removeSharedBoardMercury(currentChannelId);
            currentChannelId = null;
        }
        whiteboardMercury = null;
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

                    Ln.d("Using new mercury connection for whiteboard");
                    startWhiteboardMercury(boardRegistration.getWebSocketUrl());

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
                } else {
                    bindingFailed(response);
                }
            }

            @Override
            public void onFailure(Call<DeviceRegistration> call, Throwable t) {
                Ln.e("Failed to create secondary mercury connection");
                // handle execution failures like no internet connectivity
                //TODO Retrofit 2 failure
                initializingRealtime = false;
                whiteboardService.boardReady();
            }
        });
    }

    private void startWhiteboardMercury(Uri webSocketUrl) {
        whiteboardMercury = whiteboardService.createWhiteboardMercuryClient();
        whiteboardMercury.setUriOverride(webSocketUrl);
        whiteboardMercury.start();
        Ln.d("Opened secondary mercury connection");
        initializingRealtime = false;
    }

    private void replaceSharedWhiteboardMercury(String channelId) {
        currentChannelId = channelId;
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
                        startWhiteboardMercury(replaceBoardRegistration.getWebSocketUrl());
                    }

                    initializingRealtime = false;
                } else {
                    Ln.e("Board registration replace is not successful");
                    initializingRealtime = false;
                }
                whiteboardService.boardReady();
            }

            @Override
            public void onFailure(Call<BoardRegistration> call, Throwable t) {
                Ln.e("Failed to replace shared mercury connection");
                initializingRealtime = false;
                whiteboardService.boardReady();
            }
        });
    }

    public void removeSharedBoardMercury(String channelId) {
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
