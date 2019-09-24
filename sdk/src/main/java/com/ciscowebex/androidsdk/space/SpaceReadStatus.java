package com.ciscowebex.androidsdk.space;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.cisco.spark.android.model.SpaceProperty;
import com.cisco.spark.android.model.conversation.Conversation;
import com.cisco.spark.android.model.conversation.ConversationTag;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Set;
import java.util.function.Function;

/**
 * Details about the data of the last activity in the space,
 * and the date of the users last presence in the space.
 * <p>
 * For spaces where lastActivityDate > lastSeenDate the room can be considered to be "unread".
 *
 * @since 2.2.0
 */
public class SpaceReadStatus {

    @SerializedName("id")
    private String _id;

    @SerializedName("tags")
    private Space.SpaceType _type;

    @SerializedName(value = "lastReadableActivityDate", alternate = {"lastRelevantActivityDate"})
    private Date _lastActivityDate;

    @SerializedName("lastSeenActivityDate")
    private Date _lastSeenActivityDate;

    protected SpaceReadStatus(Conversation conversation) {
        _id =  new WebexId(WebexId.Type.ROOM_ID, conversation.getId()).toHydraId();
        _type = conversation.isOneOnOne() ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
        _lastSeenActivityDate = conversation.getLastSeenActivityDate();
        _lastActivityDate = conversation.getLastReadableActivityDate();
        if (_lastActivityDate == null) {
            _lastActivityDate = conversation.getLastRelevantActivityTimestamp();
        }
    }

    /**
     * The identifier of this space.
     *
     * @return the identifier of this space.
     * @since 2.2.0
     */
    public String getId() {
        return _id;
    }

    /**
     * The type of this space.
     *
     * @return the type of this space.
     * @since 2.2.0
     */
    public Space.SpaceType getType() {
        return _type;
    }

    /**
     * The published date of the last readable message.
     *
     * @return the published date of the last readable message.
     * @since 2.2.0
     */
    public Date getLastActivityDate() {
        return _lastActivityDate;
    }

    /**
     * The published date of the last message that login user seen.
     *
     * @return the published date of the last message that login user seen.
     * @since 2.2.0
     */
    public Date getLastSeenActivityDate() {
        return _lastSeenActivityDate;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
