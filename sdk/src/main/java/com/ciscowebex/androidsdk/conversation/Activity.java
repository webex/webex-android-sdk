package com.ciscowebex.androidsdk.conversation;

import java.io.UnsupportedEncodingException;

public class Activity extends ActivityObject {

    private String verb;
    private Actor actor;
    private ActivityObject object;
    private ActivityObject target;
    private Participants participants;


    Activity(String verb, String objectType,Actor actor,ActivityObject object,ActivityObject target) {
        this.verb = verb;
        this.objectType = objectType;
        this.object = object;
        this.target = target;
        this.actor = actor;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public ActivityObject getObject() {
        return object;
    }

    public void setObject(ActivityObject object) {
        this.object = object;
    }

    public ActivityObject getTarget() {
        return target;
    }

    public void setTarget(ActivityObject target) {
        this.target = target;
    }

    public Participants getParticipants() {
        return participants;
    }

    public void setParticipants(Participants participants) {
        this.participants = participants;
    }

    public static Activity acknowledge(String personId, String messageId, String roomId) throws UnsupportedEncodingException {

        return new Activity("acknowledge", "activity",new Actor(Utils.getUUID(personId)),new ActivityObject("activity", Utils.getUUID(messageId))
         , new ActivityObject("conversation",Utils.getUUID(roomId)));
    }

}
