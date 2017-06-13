package com.cisco.spark.android.room;

import android.net.Uri;

import com.cisco.spark.android.core.ApiClientProvider;
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
                    callback.onError();
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
                e.printStackTrace();
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
                            try {
                                Ln.i("RoomService, requestProximityStatus, response: %d - %s", response.code(), response.errorBody().string());
                            } catch (IOException e) {
                                onFailure(call, e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<FindRoomByDeviceResponse> call, Throwable t) {
                        Ln.i(t, "RoomService, requestProximityStatus, onFailure");
                    }
                }
        );
    }
}
