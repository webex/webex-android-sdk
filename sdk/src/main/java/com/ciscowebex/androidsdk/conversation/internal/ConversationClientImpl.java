package com.ciscowebex.androidsdk.conversation.internal;


import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.conversation.Activity;
import com.ciscowebex.androidsdk.conversation.ConversationClient;
import com.ciscowebex.androidsdk.message.internal.WebexId;
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

public class ConversationClientImpl implements ConversationClient {

    private Authenticator _authenticator;
    private ReadreceiptsServiceInt _service;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://conv-a.wbx2.com/conversation/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public ConversationClientImpl(Authenticator authenticator) {

        _authenticator = authenticator;
        _service = retrofit.create(ReadreceiptsServiceInt.class);
    }

    public void sendReadReceipt(@NonNull String personId, @NonNull String messageId,@NonNull String spaceId,@NonNull CompletionHandler<ResponseBody> handler) {

        Activity activity = Activity.acknowledge(personId,messageId,spaceId);
        ServiceBuilder.async(_authenticator,handler, s -> _service.sendReadReciept(s,activity),new ObjectCallback<>(handler));
    }

    public void getSpaceInfo(@NonNull String spaceId, @NonNull CompletionHandler<Activity> handler)  {

        String _roomId = WebexId.from(spaceId).getId();
        ServiceBuilder.async(_authenticator,handler, s-> _service.getSpaceInfo(s,_roomId, "all",0 ),new ObjectCallback<>(handler));
    }

    private interface ReadreceiptsServiceInt {

        @GET("conversations/{roomId}")
        Call<Activity> getSpaceInfo(@Header("Authorization") String authorization,
                                    @Path("roomId") String roomId,
                                    @Query("participantAckFilter") String participantAckFilter,
                                    @Query("activitiesLimit") int activitiesLimit);

        @POST("activities?personRefresh=true")
        Call<ResponseBody> sendReadReciept(@Header("Authorization") String authorization, @Body Activity parameters);
    }
}