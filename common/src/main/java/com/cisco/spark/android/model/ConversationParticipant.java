package com.cisco.spark.android.model;

import android.database.Cursor;
import android.text.TextUtils;

import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.util.NameUtils;

import static com.cisco.spark.android.sync.ActorRecord.ActorKey;
import static com.cisco.spark.android.sync.ConversationContract.vw_Participant;

//TODO this doesn't really belong in .model
public class ConversationParticipant {
    private String conversationId;
    private Person person;
    private String orgId;
    private boolean squaredEntitled;
    private String lastAckActivityId;
    private long lastAcActivityTime;
    private boolean moderator;

    public ConversationParticipant(Cursor cursor) {
        this.conversationId = cursor.getString(vw_Participant.CONVERSATION_ID.ordinal());
        this.orgId = cursor.getString(vw_Participant.ORG_ID.ordinal());
        this.squaredEntitled = cursor.getInt(vw_Participant.ENTITLEMENT_SQUARED.ordinal()) != 0;
        this.lastAckActivityId = cursor.getString(vw_Participant.LASTACK_ACTIVITY_ID.ordinal());
        this.lastAcActivityTime = cursor.getLong(vw_Participant.LASTACK_ACTIVITY_TIME.ordinal());
        this.moderator = cursor.getInt(vw_Participant.IS_MODERATOR.ordinal()) != 0;

        String actorUuid = cursor.getString(vw_Participant.ACTOR_UUID.ordinal());
        String displayName = cursor.getString(vw_Participant.DISPLAY_NAME.ordinal());
        String email = cursor.getString(vw_Participant.EMAIL.ordinal());
        person = new Person(email, displayName);
        person.setUuid(actorUuid);
    }

    public String getConversationId() {
        return conversationId;
    }

    public ActorKey getActorKey() {
        return new ActorKey(getActorUuid());
    }

    public String getActorUuid() {
        return person.getUuid();
    }

    public String getDisplayName() {
        return person.getDisplayName();
    }

    public String getEmail() {
        return person.getEmail();
    }

    public String getOrgId() {
        return orgId;
    }

    public String getOrg() {
        return "@" + NameUtils.getDomainFromEmail(getEmail());
    }

    public boolean isSquaredEntitled() {
        return squaredEntitled;
    }

    public String getLastAckActivityId() {
        return lastAckActivityId;
    }

    public long getLastAcActivityTime() {
        return lastAcActivityTime;
    }

    public boolean isModerator() {
        return moderator;
    }

    public boolean isExternal(AuthenticatedUser authenticatedUser) {
        return authenticatedUser.isExternal(orgId);
    }

    public String getShortNameOrEmail() {
        return (TextUtils.isEmpty(getDisplayName()) ? getEmail() : NameUtils.getShortName(getDisplayName()));
    }

    public String getUuidOrEmail() {
        return person.getUuid() == null ? person.getUuid() : person.getEmail();
    }

    public Person getPerson() {
        return person;
    }
}
