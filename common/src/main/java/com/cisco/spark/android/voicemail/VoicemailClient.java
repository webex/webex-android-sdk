package com.cisco.spark.android.voicemail;

import com.cisco.spark.android.voicemail.model.VoicemailInfo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

public interface VoicemailClient {
    @GET("vmInfo")
    Observable<VoicemailInfo> getVoicemailInfo();

    //> FOR TESTING ONLY: USER POSTING MUST BE A MACHINE ACCOUNT WITH "vm_admin" SCOPE ACCESS
    @POST("vmInfo/orgs/{orgUUID}/users/{userUUID}")
    Call<VoicemailInfo> setVoicemailInfo(@Path("orgUUID") String orgUUID, @Path("userUUID") String userUUID, @Body VoicemailInfo voicemailInfo);
}
