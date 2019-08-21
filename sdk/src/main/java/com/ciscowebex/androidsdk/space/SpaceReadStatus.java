package com.ciscowebex.androidsdk.space;

import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Set;

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
    private Set<String> _type;

    @SerializedName(value = "lastReadableActivityDate", alternate = {"lastRelevantActivityDate"})
    private Date _lastActivityDate;

    @SerializedName("lastSeenActivityDate")
    private Date _lastSeenActivityDate;

    /**
     * The identifier of this space.
     *
     * @return the identifier of this space.
     * @since 2.2.0
     */
    public String getId() {
        return new WebexId(WebexId.Type.ROOM_ID, _id).toHydraId();
    }

    /**
     * The type of this space.
     *
     * @return the type of this space.
     * @since 2.2.0
     */
    public Space.SpaceType getType() {
        return _type.contains("ONE_ON_ONE") ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
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
