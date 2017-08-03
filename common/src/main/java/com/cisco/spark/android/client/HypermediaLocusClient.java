package com.cisco.spark.android.client;

import com.cisco.spark.android.locus.model.CalliopeAgent;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.MediaShare;
import com.cisco.spark.android.locus.requests.AlertLocusRequest;
import com.cisco.spark.android.locus.requests.CreateAclRequest;
import com.cisco.spark.android.locus.requests.DeclineLocusRequest;
import com.cisco.spark.android.locus.requests.FloorShareRequest;
import com.cisco.spark.android.locus.requests.JoinLocusRequest;
import com.cisco.spark.android.locus.requests.LeaveLocusRequest;
import com.cisco.spark.android.locus.requests.LocusHoldRequest;
import com.cisco.spark.android.locus.requests.LocusResumeRequest;
import com.cisco.spark.android.locus.requests.MediaCreationRequest;
import com.cisco.spark.android.locus.requests.MergeLociRequest;
import com.cisco.spark.android.locus.requests.MigrateRequest;
import com.cisco.spark.android.locus.requests.ModifyMediaRequest;
import com.cisco.spark.android.locus.requests.SendDtmfRequest;
import com.cisco.spark.android.locus.requests.UpdateLocusRequest;
import com.cisco.spark.android.locus.responses.CreateAclResponse;
import com.cisco.spark.android.locus.responses.DeleteIntentResponse;
import com.cisco.spark.android.locus.responses.JoinLocusResponse;
import com.cisco.spark.android.locus.responses.LeaveLocusResponse;
import com.cisco.spark.android.locus.responses.LocusParticipantResponse;
import com.cisco.spark.android.locus.responses.LocusResponse;
import com.cisco.spark.android.locus.responses.MediaCreationResponse;
import com.cisco.spark.android.locus.responses.MediaDeletionResponse;
import com.cisco.spark.android.locus.responses.MergeLociResponse;
import com.cisco.spark.android.locus.responses.ModifyMediaResponse;
import com.cisco.spark.android.locus.responses.UpdateLocusResponse;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Url;


public interface HypermediaLocusClient {
    @GET
    Call<Locus> getLocus(@Url String url, @Query("sync_debug") String value);

    @POST
    Call<JoinLocusResponse> joinLocus(@Url String url, @Body JoinLocusRequest request);

    @POST
    Call<MergeLociResponse> mergeLoci(@Url String url, @Body MergeLociRequest mergeLociRequest);

    @PUT
    Call<UpdateLocusResponse> updateLocus(@Url String url, @Body UpdateLocusRequest updateLocusRequest);

    @PUT
    Call<LeaveLocusResponse> leaveLocus(@Url String url, @Body LeaveLocusRequest leaveLocusRequest);

    @PUT
    Call<LocusResponse> declineLocus(@Url String url, @Body DeclineLocusRequest request);

    @PUT
    Call<LocusResponse> alertLocus(@Url String url, @Body AlertLocusRequest request);

    @PUT
    Call<ModifyMediaResponse> modifyMedia(@Url String url, @Body ModifyMediaRequest request);

    @POST
    Call<MediaCreationResponse> createMedia(@Url String url, @Body MediaCreationRequest request);

    @DELETE
    Call<MediaDeletionResponse> deleteMedia(@Url String url);

    @PUT
    Call<MediaShare> updateFloor(@Url String url, @Body FloorShareRequest request);

    @PATCH
    Call<ModifyMediaResponse> modifyParticipantControls(@Url String url, @Body JsonObject request);

    @PATCH
    Call<LocusResponse> modifyLocusControls(@Url String url, @Body JsonObject request);

    @PUT
    Call<LeaveLocusResponse> expelParticipant(@Url String url, @Body JsonObject request);

    @GET
    List<CalliopeAgent> getCalliopeAgents(@Url String url);

    @POST
    Call<Void> sendDtmf(@Url String url, @Body SendDtmfRequest sendDtmfRequest);

    @POST
    Call<Void> migrateLocus(@Url String url, @Body MigrateRequest migrateRequest);

    @PUT
    Call<LocusParticipantResponse> holdLocus(@Url String url, @Body LocusHoldRequest request);

    @PUT
    Call<LocusParticipantResponse> resumeLocus(@Url String url, @Body LocusResumeRequest request);

    @GET
    Call<Void> keepAlive(@Url String url);

    @POST
    Call<CreateAclResponse> createAcl(@Url String url, @Body CreateAclRequest createAclRequest);

    @DELETE
    Call<DeleteIntentResponse> deleteIntent(@Url String url);
}
