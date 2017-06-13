package com.cisco.spark.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.sync.ActorRecord;

import java.util.ArrayList;
import java.util.List;

public class Person extends ActivityObject implements Parcelable, Actor {
    public static final String BOT = "BOT";
    public static final String ROBOT = "ROBOT";
    public static final String PERSON = "PERSON";
    public static final String LYRA_SPACE = "LYRA_SPACE";

    private List<ParticipantTag> tags = new ArrayList<>();
    private String orgId;
    private String entryUUID;
    private ParticipantRoomProperties roomProperties;
    private boolean isLocalContact;
    private String emailAddress;
    private String type;

    public Person() {
        super(ObjectType.person);
    }

    public Person(String emailOrUuid) {
        this();
        setId(emailOrUuid);
        if (emailOrUuid.contains("@"))
            emailAddress = emailOrUuid;
        else
            entryUUID = emailOrUuid;
    }

    public Person(ActorRecord.ActorKey actorKey) {
        this();
        setUuid(actorKey.getUuid());
    }

    public Person(String email, String displayName) {
        this(email);
        setDisplayName(displayName);
    }

    public Person(AuthenticatedUser user) {
        this(user.getEmail(), user.getDisplayName());
        if (user.getKey() != null)
            setUuid(user.getKey().getUuid());
    }

    public Person(ActorRecord actor) {
        this(actor.getEmail(), actor.getDisplayName());
        if (actor.getKey() != null)
            setUuid(actor.getKey().getUuid());
    }

    public Person(User user) {
        this(user.getEmail(), user.getDisplayName());
        setUuid(user.getUuid());
    }

    public boolean isAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null)
            return false;

        if (TextUtils.isEmpty(getEmail()))
            return getKey() != null && getKey().isAuthenticatedUser(authenticatedUser);

        return authenticatedUser.getEmail().equals(getEmail())
                || authenticatedUser.getEmail().equals(getId());
    }

    @Override
    public String getDisplayName() {
        String displayName = super.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = getEmail();
        }
        return displayName;
    }

    public List<ParticipantTag> getTags() {
        return tags;
    }

    public boolean isSquaredEntitled() {
        return !tags.contains(ParticipantTag.ENTITLEMENT_NO_SQUARED);
    }

    public boolean isSideboarded() {
        return tags.contains(ParticipantTag.SIDE_BOARDED);
    }

    public boolean isNotFoundInCI() {
        return tags.contains(ParticipantTag.CI_NOTFOUND);
    }

    public boolean isInvited() {
        return !isSquaredEntitled() || isSideboarded();
    }

    public void setIsLocalContact(boolean isLocalContact) {
        this.isLocalContact = isLocalContact;
    }

    public boolean isLocalContact() {
        return isLocalContact;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getUuid() {
        return entryUUID;
    }

    @Override
    public String getEmail() {
        return emailAddress;
    }

    @Override
    public String getId() {
        if (TextUtils.isEmpty(getUuid()))
            return super.getId();
        return getUuid();
    }

    public ParticipantRoomProperties getRoomProperties() {
        return roomProperties;
    }

    public void setUuid(String entryUUID) {
        this.entryUUID = entryUUID;
        if (!TextUtils.isEmpty(entryUUID))
            setId(entryUUID);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ActorRecord.ActorKey getKey() {
        if (TextUtils.isEmpty(getUuid())) {
            if (TextUtils.equals(getEmail(), getId()))
                return null;
            return new ActorRecord.ActorKey(getId());
        }
        return new ActorRecord.ActorKey(getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Person))
            return false;

        Person that = (Person) o;

        if (entryUUID != null && entryUUID.equals(that.entryUUID))
            return true;

        if (getEmail() != null && getEmail().equals(that.getEmail()))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        // Defer to equals since we can match on either uuid _or_ email
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    Person(Parcel src) {
        emailAddress = src.readString();
        entryUUID = src.readString();
        orgId = src.readString();
        setId(src.readString());
        setDisplayName(src.readString());
        setType(src.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(emailAddress);
        dest.writeString(entryUUID);
        dest.writeString(orgId);
        dest.writeString(getId());
        dest.writeString(getDisplayName());
        dest.writeString(getType());
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Person createFromParcel(Parcel source) {
            return new Person(source);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
}
