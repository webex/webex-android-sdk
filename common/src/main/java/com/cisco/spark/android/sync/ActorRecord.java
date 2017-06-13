package com.cisco.spark.android.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.model.Actor;
import com.cisco.spark.android.model.ParticipantTag;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.presence.PresenceStatus;
import com.cisco.spark.android.sync.ConversationContract.ActorEntry;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.Strings;

import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

public class ActorRecord implements Comparable<ActorRecord>, Parcelable, Actor {
    private ActorKey key;
    private String email;

    private String displayName;
    private String orgId;
    private String type;    // One of the types defined in Person (BOT/ROBOT/PERSON)
    private PresenceStatus presenceStatus;
    private Date presenceLastActive;
    private Date presenceExpiration;
    private EnumSet<ParticipantTag> tags;
    private boolean areTagsValid = true;
    private long rawContactId = -1;

    //TODO give this the ConversationResolver treatment
    public ActorRecord(ActorKey key,
                       String email,
                       String displayName,
                       boolean isSquaredEntitled,
                       String orgId,
                       boolean isNotFound,
                       PresenceStatus status,
                       Date presenceLastActive,
                       Date presenceExpiration,
                       long rawContactId,
                       String type) {
        this.key = key;
        this.displayName = displayName;
        this.orgId = orgId;
        this.presenceStatus = status;
        this.presenceLastActive = presenceLastActive;
        this.presenceExpiration = presenceExpiration;
        this.rawContactId = rawContactId;
        this.type = type;

        if (email != null)
            this.email = email.toLowerCase(Locale.US);
        tags = EnumSet.noneOf(ParticipantTag.class);
        if (!isSquaredEntitled) {
            tags.add(ParticipantTag.ENTITLEMENT_NO_SQUARED);
            tags.add(ParticipantTag.SIDE_BOARDED);
        }

        if (isNotFound) {
            tags.add(ParticipantTag.CI_NOTFOUND);
        }
    }

    public ActorRecord(ActorKey key, String email, String displayName, String type, boolean isSquaredEntitled, String orgId, boolean isNotFound) {
        this(key, email, displayName, isSquaredEntitled, orgId, isNotFound, null, null, null, -1, type);
    }

    public ActorRecord(Cursor cursor) {
        this(
                new ActorKey(cursor.getString(cursor.getColumnIndex(ActorEntry.ACTOR_UUID.name()))),
                cursor.getString(cursor.getColumnIndex(ActorEntry.EMAIL.name())),
                cursor.getString(cursor.getColumnIndex(ActorEntry.DISPLAY_NAME.name())),
                !"0".equals(cursor.getString(cursor.getColumnIndex(ActorEntry.ENTITLEMENT_SQUARED.name()))),
                cursor.getString(cursor.getColumnIndex(ActorEntry.ORG_ID.name())),

                !"0".equals(cursor.getString(cursor.getColumnIndex(ActorEntry.CI_NOTFOUND.name()))),
                PresenceStatus.values()[cursor.getInt(cursor.getColumnIndex(ActorEntry.PRESENCE_STATUS.name()))],
                new Date(cursor.getLong(cursor.getColumnIndex(ActorEntry.PRESENCE_LAST_ACTIVE.name()))),
                new Date(cursor.getLong(cursor.getColumnIndex(ActorEntry.PRESENCE_EXPIRATION_DATE.name()))),
                getRawContactId(cursor),
                cursor.getColumnIndex(ActorEntry.TYPE.name()) == -1 ? null : cursor.getString(cursor.getColumnIndex(ActorEntry.TYPE.name()))
        );
    }

    private static long getRawContactId(Cursor cursor) {
        int rawContactIdIndex = cursor.getColumnIndex(ActorEntry.RAW_CONTACT_ID.name());
        if (rawContactIdIndex == -1)
            return -1;

        return cursor.isNull(rawContactIdIndex)
                ? -1
                : cursor.getLong(rawContactIdIndex);
    }

    public ActorRecord(Person person) {
        key = person.getKey();
        email = person.getEmail();
        displayName = person.getDisplayName();
        tags = EnumSet.noneOf(ParticipantTag.class);
        for (ParticipantTag tag : person.getTags()) {
            if (tag != null) {
                tags.add(tag);
            }
        }
        orgId = person.getOrgId();
        type = person.getType();
    }

    public static ActorRecord newInstance(Person actor) {
        return new ActorRecord(actor.getKey(), actor.getEmail(), actor.getDisplayName(), actor.getType(), actor.isSquaredEntitled(), actor.getOrgId(), actor.isNotFoundInCI());
    }

    public static ActorRecord newInstance(AuthenticatedUser user) {
        return new ActorRecord(user.getKey(), user.getEmail(), user.getDisplayName(), Person.PERSON, true, user.getOrgId(), false);
    }

    public ActorKey getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEmail() {
        return email == null ? null : email.toLowerCase(Locale.getDefault());
    }

    public void setEmail(String email) {
        if (email != null) {
            email = email.toLowerCase(Locale.US);
        }
        this.email = email;
    }

    public String getDisplayName() {
        if (displayName == null) {
            displayName = getEmail();
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isSquaredEntitled() {
        return tags == null || !this.tags.contains(ParticipantTag.ENTITLEMENT_NO_SQUARED);
    }

    public boolean isSideboarded() {
        return tags != null && tags.contains(ParticipantTag.SIDE_BOARDED);
    }

    public boolean isInvited() {
        return !isSquaredEntitled() || isSideboarded();
    }

    public boolean isExternal(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null)
            return false;
        return authenticatedUser.isExternal(orgId);
    }

    public boolean isNotFoundInCI() {
        return tags.contains(ParticipantTag.CI_NOTFOUND);
    }

    public PresenceStatus getPresenceStatus() {
        return presenceStatus;
    }

    public void setPresenceStatus(PresenceStatus value) {
        this.presenceStatus = value;
    }

    public Date getPresenceLastActive() {
        return presenceLastActive;
    }

    public void setPresenceLastActive(Date value) {
        this.presenceLastActive = value;
    }

    public Date getPresenceExpiration() {
        return presenceExpiration;
    }

    public void setPresenceExpiration(Date value) {
        this.presenceExpiration = value;
    }

    public long getRawContactId() {
        return rawContactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActorRecord that = (ActorRecord) o;

        return (key != null && key.equals(that.key))
                || (email != null && TextUtils.equals(email, that.email));
    }

    @Override
    public int hashCode() {
        // ActorRecords always have an email address, if not then we should return 0
        return email == null ? 0 : email.hashCode();
    }

    public ContentValues buildContentValues() {
        ContentValues values = new ContentValues();
        values.put(ActorEntry.ACTOR_UUID.name(), getUuidOrEmail());
        values.put(ActorEntry.EMAIL.name(), getEmail());
        if (getType() != null) {
            values.put(ActorEntry.TYPE.name(), getType());
        }

        if (!isSparse()) {
            values.put(ActorEntry.DISPLAY_NAME.name(), getDisplayName());
        }

        if (this.hasValidTags()) {
            values.put(ActorEntry.ENTITLEMENT_SQUARED.name(), isSquaredEntitled() ? 1 : 0);
            values.put(ActorEntry.CI_NOTFOUND.name(), isNotFoundInCI() ? 1 : 0);
        }
        if (getOrgId() != null) {
            values.put(ActorEntry.ORG_ID.name(), getOrgId());
        }

        if (presenceStatus != null) {
            values.put(ActorEntry.PRESENCE_STATUS.name(), presenceStatus.ordinal());
        }

        if (presenceLastActive != null) {
            values.put(ActorEntry.PRESENCE_LAST_ACTIVE.name(), presenceLastActive.getTime());
        }

        if (presenceExpiration != null) {
            values.put(ActorEntry.PRESENCE_EXPIRATION_DATE.name(), presenceExpiration.getTime());
        }

        return values;
    }

    public String getUuidOrEmail() {
        if (key != null && !TextUtils.isEmpty(key.getUuid()))
            return key.getUuid();
        return email;
    }

    public void addInsertUpdateContentProviderOperation(Batch batch) {
        ContentValues values = buildContentValues();

        //Insert then update. Insert will fail silently if actor already exists.
        ContentProviderOperation op = ContentProviderOperation.newInsert(ActorEntry.CONTENT_URI)
                .withValues(values)
                .build();

        batch.add(op);

        if (!Strings.isEmailAddress(getKey().uuid))
            values.put(ActorEntry.ACTOR_UUID.name(), getKey().uuid);

        if (TextUtils.isEmpty(values.getAsString(ActorEntry.EMAIL.name())))
            values.remove(ActorEntry.EMAIL.name());

        if (TextUtils.isEmpty(values.getAsString(ActorEntry.ACTOR_UUID.name())))
            values.remove(ActorEntry.ACTOR_UUID.name());

        op = ContentProviderOperation.newUpdate(ActorEntry.CONTENT_URI)
                .withValues(values)
                .withSelection(ActorEntry.ACTOR_UUID + "=? OR " + ActorEntry.ACTOR_UUID + "=?", new String[]{email == null ? getUuidOrEmail() : email, getUuidOrEmail()})
                .build();

        batch.add(op);
    }

    public boolean hasValidTags() {
        return areTagsValid;
    }

    public EnumSet<ParticipantTag> getTags() {
        if (tags == null)
            return EnumSet.noneOf(ParticipantTag.class);
        return tags;
    }

    public boolean isAuthenticatedUser(@Nullable AuthenticatedUser user) {
        if (key != null)
            return key.isAuthenticatedUser(user);
        if (email != null && user != null)
            return email.equals(user.getEmail());
        return false;
    }

    public void setTags(EnumSet<ParticipantTag> tags) {
        this.tags = tags;
    }

    public void setTagValidity(boolean tagValidity) {
        this.areTagsValid = tagValidity;
    }

    @Override
    public int compareTo(ActorRecord another) {
        return getSortId().compareTo(another.getSortId());
    }

    private String getSortId() {
        if (!TextUtils.isEmpty(email))
            return email;

        if (!TextUtils.isEmpty(displayName))
            return displayName;

        if (key != null && key.getUuid() != null)
            return key.getUuid();

        return "";
    }

    public boolean isSparse() {
        return getEmail() != null && getEmail().equals(getDisplayName());
    }

    public String getOrg() {
        return "@" + NameUtils.getDomainFromEmail(getEmail());
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setRawContactId(long rawContactId) {
        this.rawContactId = rawContactId;
    }

    public static class ActorKey implements Parcelable {
        @NonNull
        private String uuid;

        public ActorKey(@NonNull String uuid) {
            this.uuid = uuid.toLowerCase(Locale.US);
        }

        public ActorKey(Parcel in) {
            this.uuid = in.readString().toLowerCase(Locale.US);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActorKey actorKey = (ActorKey) o;

            return TextUtils.equals(actorKey.uuid, uuid);
        }

        @Override
        public int hashCode() {
            return uuid == null ? 0 : uuid.hashCode();
        }

        @Override
        public String toString() {
            return getUuid();
        }

        public boolean isAuthenticatedUser(AuthenticatedUser authenticatedUser) {
            return authenticatedUser != null && equals(authenticatedUser.getKey());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(uuid);
        }

        public static final Parcelable.Creator<ActorKey> CREATOR
                = new Parcelable.Creator<ActorKey>() {
            public ActorKey createFromParcel(Parcel in) {
                return new ActorKey(in);
            }

            public ActorKey[] newArray(int size) {
                return new ActorKey[size];
            }
        };

        @NonNull
        public String getUuid() {
            return uuid;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.key, 0);
        dest.writeString(this.email);
        dest.writeString(this.displayName);
        dest.writeString(this.orgId);
        dest.writeSerializable(this.tags);
        dest.writeByte(areTagsValid ? (byte) 1 : (byte) 0);
        dest.writeString(this.type);
        dest.writeString(this.presenceStatus == null ? "" : this.presenceStatus.toString());
        dest.writeLong(this.presenceExpiration == null ? -1 : this.presenceExpiration.getTime());
        dest.writeLong(this.presenceLastActive == null ? -1 : this.presenceLastActive.getTime());
    }

    @SuppressWarnings("unchecked")
    protected ActorRecord(Parcel in) {
        this.key = in.readParcelable(ActorKey.class.getClassLoader());
        this.email = in.readString();
        this.displayName = in.readString();
        this.orgId = in.readString();
        this.tags = (EnumSet<ParticipantTag>) in.readSerializable();
        this.areTagsValid = in.readByte() != 0;
        this.type = in.readString();
        String tmpStatus = in.readString();
        this.presenceStatus = tmpStatus.isEmpty() ? null : PresenceStatus.fromString(tmpStatus);
        long tmpDate = in.readLong();
        this.presenceExpiration = tmpDate == -1 ? null : new Date(tmpDate);
        tmpDate = in.readLong();
        this.presenceLastActive = tmpDate == -1 ? null : new Date(tmpDate);
    }

    public static final Parcelable.Creator<ActorRecord> CREATOR = new Parcelable.Creator<ActorRecord>() {
        public ActorRecord createFromParcel(Parcel source) {
            return new ActorRecord(source);
        }

        public ActorRecord[] newArray(int size) {
            return new ActorRecord[size];
        }
    };
}
