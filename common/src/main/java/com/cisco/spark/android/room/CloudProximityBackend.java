package com.cisco.spark.android.room;

import android.net.Uri;

import com.cisco.spark.android.client.LyraClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.lyra.LyraSpaceOccupantPass;
import com.cisco.spark.android.lyra.LyraSpaceOccupantRequest;
import com.cisco.spark.android.lyra.SpaceResponseCallback;
import com.cisco.spark.android.lyra.model.AdvertisementByToken;
import com.cisco.spark.android.lyra.model.Link;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.ObserverAdapter;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;

public class CloudProximityBackend implements ProximityBackend {

    private static final int RESPONSE_STATUS_ANNOUNCE_INVALID_TOKEN = 422;

    private Subscription pairedSubscription;
    private ApiClientProvider apiClientProvider;
    private DeviceRegistration deviceRegistration;

    public CloudProximityBackend(ApiClientProvider apiClientProvider, DeviceRegistration deviceRegistration) {
        this.apiClientProvider = apiClientProvider;
        this.deviceRegistration = deviceRegistration;
    }

    @Override
    public void announceProximity(String token, final AnnounceCallback callback) {

        if (apiClientProvider != null) {

            AnnounceProximityRequest announceProximityRequest = new AnnounceProximityRequest(token, deviceRegistration.getUrl());

            try {
                Ln.d("RoomService, announce proximity with token: %s", token);
                Response<AnnounceProximityResponse> response = apiClientProvider.getRoomServiceClient().announceProximity(announceProximityRequest).execute();

                if (!response.isSuccessful()) {
                    if (response.code() == RESPONSE_STATUS_ANNOUNCE_INVALID_TOKEN) {
                        Ln.i("RoomService, detected invalid token - ignore");
                    } else {
                        Ln.w("RoomService, AnnounceProximity failed type: %s", response.errorBody().string());
                    }
                    callback.onError(String.valueOf(response.code()));
                } else {
                    if (pairedSubscription != null) {
                        Ln.i("RoomService, new valid token clear timeout timer");
                        pairedSubscription.unsubscribe();
                    }

                    AnnounceProximityResponse proximityResponse = response.body();

                    // This part actually looks like business logic, move it to RoomService again.
                    // Not the most important part right now  - and it might go away - but good catch.
                    Ln.d("AnnounceProximityResponse nextToken in %d seconds", proximityResponse.getSecondsToNextTokenEmit());
                    Ln.d("AnnounceProximityResponse tokenValid in %d seconds", proximityResponse.getMaxTokenValidityInSeconds());

                    // Set timer to detect if we haven't received token by given max. token validity value
                    Ln.i("RoomService, set new token timeout timer to %d seconds", proximityResponse.getMaxTokenValidityInSeconds());
                    pairedSubscription = Observable.timer(proximityResponse.getMaxTokenValidityInSeconds(), TimeUnit.SECONDS).subscribe(new ObserverAdapter<Long>() {
                        @Override
                        public void onCompleted() {
                            callback.onTimeout();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            super.onError(throwable);
                            Ln.e(throwable, "RoomService, token timeout timer failed");
                        }
                    });

                    callback.onSuccess(proximityResponse.getSecondsToNextTokenEmit(), proximityResponse.getMaxTokenValidityInSeconds());
                }
            } catch (IOException e) {
                Ln.e(e.getMessage());
            }
        }
    }

    @Override
    public void requestProximityStatus(Uri url, final ProximityStatusCallback callback) {
        String base64DeviceUrl = CryptoUtils.base64EncodedString(url);
        Ln.i("RoomService, requestProximityStatus");
        apiClientProvider.getRoomServiceClient().requestProximityStatus(base64DeviceUrl).enqueue(
                new Callback<FindRoomByDeviceResponse>() {
                    @Override
                    public void onResponse(Call<FindRoomByDeviceResponse> call, Response<FindRoomByDeviceResponse> response) {
                        if (response.isSuccessful()) {
                            callback.newProximityStatus(response.body());
                        } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                            Ln.w("RoomService, requestProximityStatus, Room does not exist");
                        } else {
                            Ln.w("RoomService, requestProximityStatus, response code: %d", response.code());
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<FindRoomByDeviceResponse> call, Throwable t) {
                        Ln.e(t, "RoomService, requestProximityStatus, onFailure");
                    }
                }
        );
    }

    public void announceLyraProximity(String token, final LyraAnnounceCallback callback) {

        if (apiClientProvider != null) {

            try {
                Ln.d("LyraService, announce proximity with space identify %s", token);
                Response<AdvertisementByToken> response = apiClientProvider.getLyraProximityServiceClient().announceProximity(token).execute();

                if (!response.isSuccessful()) {
                    if (response.code() == RESPONSE_STATUS_ANNOUNCE_INVALID_TOKEN) {
                        Ln.w("LyraService, detected invalid token - ignore");
                    } else {
                        Ln.w("LyraService, AnnounceProximity failed type: %s", response.errorBody().string());
                    }
                    callback.onError(String.valueOf(response.code()));
                } else {

                    AdvertisementByToken proximityResponse = response.body();
                    long maxTokenValidityInSeconds = proximityResponse.getToken().getRemainingValidityInSeconds();
                    long secondsToNextTokenEmit = proximityResponse.getToken().getDurationInMillis() / 1000;

                    Ln.d("AnnounceProximityResponse nextToken in %d seconds", secondsToNextTokenEmit);
                    Ln.d("AnnounceProximityResponse tokenValid in %d seconds", maxTokenValidityInSeconds);

                    callback.onSuccess(proximityResponse.getLinks().getAddMeToSpace(),
                            proximityResponse.getAdvertiser().getId().toString(),
                            secondsToNextTokenEmit,
                            maxTokenValidityInSeconds,
                            proximityResponse.getProof());
                }
            } catch (IOException e) {
                Ln.e(e.getMessage());
            }
        }
    }

    @Override
    public void requestAddToSpace(Link spaceLink, Uri deviceUrl, String proof, final SpaceResponseCallback callback) {
        Ln.i("LyraService, requestAddToSpace");

        LyraClient lyraClient = apiClientProvider.getLyraClient();

        if (spaceLink.getMethod() == Link.Method.PUT) {
            LyraSpaceOccupantPass pass = new LyraSpaceOccupantPass(LyraSpaceOccupantPass.Type.PROOF, proof);
            LyraSpaceOccupantRequest request = new LyraSpaceOccupantRequest(pass, deviceUrl.toString());
            lyraClient.addMeToSpace(spaceLink.getHref(), request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response);
                    } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                        Ln.w("LyraService, requestAddToSpace, Room does not exist");
                    } else {
                        Ln.w("LyraService, requestAddToSpace, response code: %d", response.code());
                        callback.onError(response);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Ln.e(t, "LyraService, requestAddToSpace, onFailure");
                }
            });
        }
    }

    public void requestLyraProximityStatusBySpaceUrl(String spaceUrl, final LyraSpaceCallback callback) {
        Ln.i("LyraService, requestLyraProximityStatusBySpaceUrl");

        LyraClient lyraClient = apiClientProvider.getLyraClient();

        lyraClient.getSpaceStatus(spaceUrl).enqueue(new Callback<LyraSpaceResponse>() {
            @Override
            public void onResponse(Call<LyraSpaceResponse> call, Response<LyraSpaceResponse> response) {
                if (response.isSuccessful()) {
                    callback.newLyraSpaceStatus(response.body());
                } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                    Ln.w("LyraService, requestLyraProximityStatusBySpaceUrl, Room does not exist");
                } else {
                    Ln.w("LyraService, requestLyraProximityStatusBySpaceUrl, responsecode : %d", response.code());
                    callback.onError(response.body());
                }
            }

            @Override
            public void onFailure(Call<LyraSpaceResponse> call, Throwable t) {
                Ln.e(t, "LyraService, requestLyraProximityStatusBySpaceUrl, onFailure");
            }
        });
    }

    @Override
    public void leaveSpace(String spaceId, String deviceUrl, final SpaceResponseCallback callback) {
        LyraClient lyraClient = apiClientProvider.getLyraClient();
        final LyraSpaceOccupantRequest request = new LyraSpaceOccupantRequest(deviceUrl);
        lyraClient.leaveRoom(spaceId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response);
                } else {
                    Ln.w("LyraService, leaveSpace, response code: %d", response.code());
                    callback.onError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Ln.e(t, "LyraService, leaveSpace, onFailure");
            }
        });
    }

    @Override
    public void requestLyraProximityStatus(Uri deviceUrl, final ProximityStatusCallback callback) {
        LyraClient lyraClient = apiClientProvider.getLyraClient();
        String base64DeviceUrl = CryptoUtils.base64EncodedString(deviceUrl);
        lyraClient.getLyraProximityStatus(base64DeviceUrl).enqueue(new Callback<FindRoomByDeviceResponse>() {
            @Override
            public void onResponse(Call<FindRoomByDeviceResponse> call, Response<FindRoomByDeviceResponse> response) {
                if (response.isSuccessful()) {
                    callback.newProximityStatus(response.body());
                } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                    Ln.w("LyraService, requestLyraProximityStatus, Room does not exist");
                } else {
                    Ln.w("LyraService, requestLyraProximityStatus, response: %d", response.code());
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<FindRoomByDeviceResponse> call, Throwable t) {
                Ln.e(t, "LyraService, requestLyraProximityStatus, onFailure");
            }
        });
    }
}
