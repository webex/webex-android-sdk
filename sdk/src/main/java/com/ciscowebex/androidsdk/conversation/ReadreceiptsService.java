package com.ciscowebex.androidsdk.conversation;


import android.support.annotation.NonNull;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;

import java.io.UnsupportedEncodingException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class ReadreceiptsService {


    private Authenticator _authenticator;

    private ReadreceiptsService.ReadreceiptsServiceInt _service;


    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://conv-a.wbx2.com/conversation/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public ReadreceiptsService(Authenticator authenticator) {

        _authenticator = authenticator;
        _service = retrofit.create(ReadreceiptsService.ReadreceiptsServiceInt.class);
    }



    public void sendReadReceipt(@NonNull String personId, @NonNull String messageId,@NonNull String roomId,@NonNull CompletionHandler<ResponseBody> handler) throws UnsupportedEncodingException {

        Activity activity =   Activity.acknowledge(personId,messageId,roomId);
        ServiceBuilder.async(_authenticator,handler, s -> _service.sendReadReciept(s,activity),new ObjectCallback<>(handler));
    }

    public void getSpaceInfo(@NonNull String roomId, @NonNull CompletionHandler<Activity> handler) throws UnsupportedEncodingException {

        String _roomId = Utils.getUUID(roomId);
        ServiceBuilder.async(_authenticator,handler, s-> _service.getSpaceInfo(s,_roomId, "all",0 ),new ObjectCallback<>(handler));

    }

    private interface ReadreceiptsServiceInt {

        @GET("conversations/{roomId}")
        Call<Activity> getSpaceInfo(@Header("Authorization") String authorization,
                                    @Path("roomId") String roomId,
                                    @Query("participantAckFilter") String participantAckFilter,
                                    @Query("activitiesLimit") int activitiesLimit);

        @POST("activities?personRefresh=true")
        Call<ResponseBody> sendReadReciept (@Header("Authorization") String authorization, @Body Activity parameters);


    }
}
