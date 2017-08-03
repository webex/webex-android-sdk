package com.cisco.spark.android.locus.model;


import android.net.Uri;

import com.cisco.spark.android.ui.conversation.ConversationResolver;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserRecentSession {
    private static final String CONVERSATION_URL = "conversationUrl";
    private static final String CALLBACK_ADDRESS = "callbackAddress";
    private static final String OUTGOING_DIRECTION = "OUTGOING";
    private static final String MISSED_DISPOSITION = "MISSED";

    private Uri url;
    private String sessionId;
    private Date startTime;
    private Date endTime;
    private long durationSeconds;
    private long joinedDurationSeconds;
    private String direction;
    private String disposition;
    private int participantCount;
    private LocusParticipantInfo other;
    private Map<String, String> links = new HashMap<>();
    private String conversationDisplayName;
    private transient ConversationResolver conversationResolver;


    private int callCount = 1;
    private Date callGroupStartTime;
    private Date callGroupEndTime;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getJoinedDurationSeconds() {
        return joinedDurationSeconds;
    }

    public void setJoinedDurationSeconds(long joinedDurationSeconds) {
        this.joinedDurationSeconds = joinedDurationSeconds;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSecs) {
        this.durationSeconds = durationSeconds;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public LocusParticipantInfo getOther() {
        return other;
    }

    public void setOther(LocusParticipantInfo other) {
        this.other = other;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public String getConversationDisplayName() {
        return conversationDisplayName;
    }

    public void setConversationDisplayName(String conversationDisplayName) {
        this.conversationDisplayName = conversationDisplayName;
    }

    public ConversationResolver getConversationResolver() {
        return conversationResolver;
    }

    public void setConversationResolver(ConversationResolver conversationResolver) {
        this.conversationResolver = conversationResolver;
    }

    public String getConversationUrl() {
        return links.get(CONVERSATION_URL);
    }

    public void setConversationUrl(String conversationUrl) {
        links.put(CONVERSATION_URL, conversationUrl);
    }

    public String getCallbackAddress() {
        return links.get(CALLBACK_ADDRESS);
    }

    public void setCallbackAddress(String callbackAddress) {
        links.put(CALLBACK_ADDRESS, callbackAddress);
    }

    public boolean isOutgoingCall() {
        return OUTGOING_DIRECTION.equals(direction);
    }

    public boolean isMissedCall() {
        return MISSED_DISPOSITION.equals(disposition);
    }

    public int getCallCount() {
        return callCount;
    }

    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    public Date getCallGroupStartTime() {
        return callGroupStartTime;
    }

    public void setCallGroupStartTime(Date callGroupStartTime) {
        this.callGroupStartTime = callGroupStartTime;
    }

    public Date getCallGroupEndTime() {
        return callGroupEndTime;
    }

    public void setCallGroupEndTime(Date callGroupEndTime) {
        this.callGroupEndTime = callGroupEndTime;
    }
}
