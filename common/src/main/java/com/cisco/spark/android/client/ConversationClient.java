package com.cisco.spark.android.client;

import com.cisco.spark.android.locus.responses.LocusUrlResponse;
import com.cisco.spark.android.mercury.TypingEvent;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.BulkActivitiesRequest;
import com.cisco.spark.android.model.BulkActivitiesResponse;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.DeskFeedbackRequest;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.RetentionPolicy;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.model.UrlResponse;
import com.cisco.spark.android.model.UserPatchRequest;
import com.cisco.spark.android.status.HealthCheckResponse;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;

public interface ConversationClient {

    @GET("conversations?personRefresh=true&uuidEntryFormat=true&lastViewableActivityOnly=true")
    Observable<ResponseBody> getShells(@Query("conversationsLimit") long conversationsLimit, @Query("participantsLimit") long participantsLimit, @Query("sinceDate") long sinceTime, @Query("ackFilter") String ackFilter);

    @GET("conversations?personRefresh=true&uuidEntryFormat=true&ackFilter=all")
    Observable<ResponseBody> getConversationsSince(@Query("sinceDate") long since, @Query("conversationsLimit") long conversationsLimit, @Query("activitiesLimit") long activitiesLimit, @Query("participantsLimit") long participantsLimit);

    @GET("teams?includeTeamMembers=false")
    Call<ItemCollection<Team>> getTeamsWithUnjoinedTeamConversationsSince(@Query("includeNonJoinedTeamConversationsSinceDate") long since);

    @GET("conversations/left")
    Call<ItemCollection<Conversation>> getConversationsLeftSince(@Query("sinceDate") long since, @Query("conversationsLimit") long limit);

    @GET("conversations/{id}?includeParticipants=false&ackFilter=all")
    Call<Conversation> getConversation(@Path("id") String id);

    @GET("conversations/{id}?personRefresh=true&uuidEntryFormat=true&ackFilter=all")
    Call<Conversation> getMostRecentConversationActivitiesSince(@Path("id") String id, @Query("sinceDate") long since, @Query("activitiesLimit") long activitiesLimit, @Query("includeParticipants") String includeParticipants);

    @GET("conversations/{id}?includeParticipants=true&ackFilter=all")
    Call<Conversation> getConversationAndParticipants(@Path("id") String id);

    @GET("activities?personRefresh=true")
    Call<ItemCollection<Activity>> getConversationActivitiesBefore(@Query("conversationId") String id, @Query("maxDate") long before, @Query("limit") long activitiesLimit);

    @GET("activities?personRefresh=true&lastActivityFirst=false")
    Call<ItemCollection<Activity>> getConversationActivitiesImmediatelyAfter(@Query("conversationId") String id, @Query("sinceDate") long after, @Query("limit") long activitiesLimit);

    @GET("activities?personRefresh=true")
    Call<ItemCollection<Activity>> getConversationActivitiesAround(@Query("conversationId") String id, @Query("midDate") long middleActivityPublishTime, @Query("limit") long activitiesLimit);

    @POST("conversations?compact=true")
    Call<Conversation> postConversation(@Body Conversation conversation);

    @GET("teams/{id}")
    Call<Team> getTeam(@Path("id") String teamId);

    @GET("teams")
    Call<ItemCollection<Team>> getTeams();

    @POST("teams")
    Call<Team> postTeam(@Body Conversation conversation);

    @POST("teams/{teamId}/conversations/{id}/participants")
    Call<Conversation> joinTeamConversation(@Path("teamId") String teamId, @Path("id") String conversationId);

    @POST("teams/{id}/conversations?compact=true")
    Call<Conversation> postTeamConversation(@Path("id") String teamId, @Body Conversation conversation);

    @POST("activities")
    Call<Activity> postActivity(@Body Activity post);

    @POST("content?async=false")
    Call<Activity> postContent(@Body Activity post, @Query("transcode") boolean transcode);

    @POST("status/typing")
    Call<Void> postTyping(@Body TypingEvent typingEvent);

    @GET("users/deskSupportUrl")
    Call<UrlResponse> getDeskSupport(@Query("languageCode") String languageCode);

    @POST("users/deskFeedbackUrl")
    Call<UrlResponse> getDeskFeedback(@Body DeskFeedbackRequest feedbackRequest);

    @GET("conversations/user/{participant}")
    Call<Conversation> getOneOnOneConversation(@Path("participant") String userId);

    // Used by tests to create unencrypted convs
    @Deprecated
    @PUT("conversations/user/{participant}?compact=true")
    Call<Conversation> putConversation(@Path("participant") String userId);

    @PUT("conversations/{conversationId}/space")
    Call<HashMap<String, String>> putConversationSpace(@Path("conversationId") String conversationId);

    @PUT("conversations/{conversationId}/space/hidden")
    Call<HashMap<String, String>> putConversationSpaceHidden(@Path("conversationId") String conversationId);

    @POST("metrics")
    Call<Void> recordMetrics(@Body MetricsReportRequest metricsRequest);

    @PATCH("users/user")
    Call<Void> updateUser(@Body UserPatchRequest request);

    @PUT("conversations/{id}/locus")
    Call<LocusUrlResponse> getOrCreatePermanentLocus(@Path("id") String id);

    @GET("logToken")
    Call<HashMap<String, String>> getDynamicLoggingToken(@Query("clientIdentifier") String clientPin);

    @GET("mentions")
    Call<ItemCollection<Activity>> getUserMentions(@Query("sinceDate") long since, @Query("limit") int limit);

    @POST("bulk_activities_fetch")
    Call<BulkActivitiesResponse> getActivities(@Body BulkActivitiesRequest request);

    @GET
    Call<Activity> getActivity(@Url String url);

    @GET
    @Streaming
    Call<ResponseBody> downloadFile(@Url String url);

    @GET
    @Streaming
    Call<ResponseBody> downloadFileIfModified(@Url String url, @Header("If-Modified-Since") String sinceDate);

    @GET
    Call<RetentionPolicy> getRetentionPolicy(@Url String url);
    @GET("retention/self")
    Call<RetentionPolicy> getRetentionPolicyForOneOnOneConversation();

    @GET("retention/organization/{orgId}")
    Call<RetentionPolicy> getRetentionPolicyForGroupConversation(@Path("orgId") String orgId);

    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();

}
