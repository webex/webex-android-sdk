package com.cisco.spark.android.model;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.LocationManager;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Conversation extends ActivityObject {
    private ItemCollection<Person> participants = new ItemCollection<>();
    private ItemCollection<Activity> activities = new ItemCollection<>();
    private List<ConversationTag> tags = new ArrayList<>();
    private Uri locusUrl;
    private String summary;
    private String creatorUUID;
    private Date lastRelevantActivityDate;
    private Team team;
    private Boolean isTeamGuestMember;
    private Content avatar;
    private Uri avatarEncryptionKeyUrl;
    private int shareCount;
    private Uri aclUrl;

    /**
     * defaultEncryptionKeyUrl is used to encrypt outgoing messages
     */
    private Uri defaultActivityEncryptionKeyUrl;
    private transient boolean isEncrypted;

    /**
     * The key for encryptionKeyUrl is used to encrypt the conversation's title (displayName), if
     * present.
     */
    private Uri encryptionKeyUrl;
    private Uri kmsResourceObjectUrl;
    private String encryptedKmsMessage;

    private Date lastReadableActivityDate;
    private Date lastSeenActivityDate;

    private Uri retentionUrl;
    private CustodianOrgInfo custodianOrg;

    public Uri getDefaultActivityEncryptionKeyUrl() {
        return defaultActivityEncryptionKeyUrl;
    }

    public void setDefaultActivityEncryptionKeyUrl(Uri defaultActivityEncryptionKeyUrl) {
        this.defaultActivityEncryptionKeyUrl = defaultActivityEncryptionKeyUrl;
    }

    public Conversation() {
        super(ObjectType.conversation);
    }

    public Conversation(String id) {
        this();
        setId(id);
    }

    public ItemCollection<Person> getParticipants() {
        return participants;
    }

    public void setParticipants(ItemCollection<Person> participants) {
        this.participants = participants;
    }

    public ItemCollection<Activity> getActivities() {
        return activities;
    }

    public void setActivities(ItemCollection<Activity> activities) {
        this.activities = activities;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isMuted() {
        return tags.contains(ConversationTag.MUTED);
    }

    public boolean isFavorite() {
        return tags.contains(ConversationTag.FAVORITE);
    }

    public boolean isHidden() {
        return tags.contains(ConversationTag.HIDDEN);
    }

    public boolean isArchived() {
        return tags.contains(ConversationTag.ARCHIVED);
    }

    public boolean isLocked() {
        return tags.contains(ConversationTag.LOCKED);
    }

    public boolean isTeam() {
        return tags.contains(ConversationTag.TEAM);
    }

    public boolean isTeamGuestMember() {
        return (isTeamGuestMember == null) ? false : isTeamGuestMember;
    }

    public boolean isOpen() {
        return tags.contains(ConversationTag.OPEN);
    }

    public boolean isJoined() {
        return !tags.contains(ConversationTag.NOT_JOINED);
    }

    public void setIsOpen(boolean isOpen) {
        setTag(ConversationTag.OPEN, isOpen);
    }

    public void setIsTeam(boolean isTeam) {
        setTag(ConversationTag.TEAM, isTeam);
    }

    public void setIsMuted(boolean isMuted) {
        setTag(ConversationTag.MUTED, isMuted);
    }

    public Boolean isMessageNotificationsOn() {
        return getNotificationTagTristate(ConversationTag.MESSAGE_NOTIFICATIONS_ON, ConversationTag.MESSAGE_NOTIFICATIONS_OFF);
    }

    public Boolean isMentionNotificationsOn() {
        return getNotificationTagTristate(ConversationTag.MENTION_NOTIFICATIONS_ON, ConversationTag.MENTION_NOTIFICATIONS_OFF);
    }



    @Nullable
    private Boolean getNotificationTagTristate(ConversationTag notificationOnTag, ConversationTag notificationOffTag) {
        final boolean onSet = tags.contains(notificationOnTag);
        final boolean offSet = tags.contains(notificationOffTag);

        if (onSet) {
            return true;
        } else if (offSet) {
            return false;
        } else {
            return null;
        }
    }

    public void setTag(ConversationTag tag, boolean shouldExist) {
        if (shouldExist) {
            if (!tags.contains(tag))
                tags.add(tag);
        } else {
            tags.remove(tag);
        }
    }

    public List<ConversationTag> getTags() {
        return tags;
    }

    public boolean isOneOnOne() {
        return tags.contains(ConversationTag.ONE_ON_ONE);
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public Uri getLocusUrl() {
        return locusUrl;
    }

    public void setLocusUrl(Uri locusUrl) {
        this.locusUrl = locusUrl;
    }

    public String getCreatorUUID() {
        return creatorUUID;
    }

    public Uri getTitleEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public Date getLastRelevantActivityTimestamp() {
        return lastRelevantActivityDate;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Uri getAvatarEncryptionKeyUrl() {
        return avatarEncryptionKeyUrl;
    }

    public Content getAvatar() {
        return avatar;
    }

    @Override
    public ActivityObject getSkinnyObject() {
        Conversation object = new Conversation();
        object.setId(this.getId());
        object.setUri(this.getUrl());
        object.setObjectType(this.getObjectType());
        return object;
    }

    public KmsResourceObject getKmsResourceObject() {
        if (kmsResourceObjectUrl == null)
            return null;

        return new KmsResourceObject(kmsResourceObjectUrl);
    }

    public void setKmsResourceObjectUrl(Uri kmsResourceObjectUrl) {
        this.kmsResourceObjectUrl = kmsResourceObjectUrl;
    }

    public Date getLastReadableActivityDate() {
        return lastReadableActivityDate;
    }

    public Date getLastSeenActivityDate() {
        return lastSeenActivityDate;
    }

    public void setLastReadableActivityDate(Date lastReadableActivityDate) {
        this.lastReadableActivityDate = lastReadableActivityDate;
    }

    public void setLastRelevantActivityDate(Date lastRelevantActivityDate) {
        this.lastRelevantActivityDate = lastRelevantActivityDate;
    }

    public void setLastSeenActivityDate(Date lastSeenActivityDate) {
        this.lastSeenActivityDate = lastSeenActivityDate;
    }

    public CustodianOrgInfo getCustodianOrg() {
        return custodianOrg;
    }

    public void setCustodianOrg(CustodianOrgInfo custodianOrg) {
        this.custodianOrg = custodianOrg;
    }

    public Uri getRetentionUrl() {
        return retentionUrl;
    }

    public void setRetentionUrl(Uri retentionUrl) {
        this.retentionUrl = retentionUrl;
    }

    public void updateWithHeadersFrom(Conversation conversation) {
        if (conversation == null)
            return;

        if (getLastReadableActivityDate() == null) {
            setLastReadableActivityDate(conversation.getLastReadableActivityDate());
        } else if (conversation.getLastReadableActivityDate() != null && getLastReadableActivityDate().before(conversation.getLastReadableActivityDate())) {
            setLastReadableActivityDate(conversation.getLastReadableActivityDate());
        }
    }

    public Uri getAclUrl() {
        return aclUrl;
    }

    public void setAclUrl(Uri aclUrl) {
        this.aclUrl = aclUrl;
    }

    public static class Builder {
        private Conversation conversation;
        private Person creator;
        private String kmsMessage;
        private boolean built = false;
        private boolean isOneOnOne;

        public Builder() {
            conversation = new Conversation();
        }

        public Builder setTitle(String title) {
            conversation.setDisplayName(title);
            return this;
        }

        public Builder setSummary(String summary) {
            conversation.setSummary(summary);
            return this;
        }

        public Builder setEncryptionKeyUrl(Uri keyUrl) {
            conversation.setDefaultActivityEncryptionKeyUrl(keyUrl);
            return this;
        }

        public Builder setIsEncrypted(boolean isEncrypted) {
            conversation.setIsEncrypted(isEncrypted);
            return this;
        }

        public Builder setTeamShellWithTeamId(String teamId) {
            Team team = new Team();
            team.setId(teamId);
            conversation.setTeam(team);
            return this;
        }

        public Builder addActivity(Activity activity) {
            activity.setSource(ConversationContract.ActivityEntry.Source.LOCAL);
            conversation.getActivities().addItem(activity);
            return this;
        }

        public Builder setCreator(Person creator) {
            this.creator = creator;
            return this;
        }

        public Builder addParticipant(Person person) {
            return addActivity(Activity.add(null, person, null));
        }

        public Builder addMessage(String message, LocationManager locationManager, Uri encryptionKeyUrl) {
            return addActivity(Activity.post(null, new Comment(message), null, null, locationManager, encryptionKeyUrl));
        }

        public Conversation build() {
            if (built) {
                throw new UnsupportedOperationException("A conversation was already built with this builder.");
            }
            if (creator == null) {
                throw new UnsupportedOperationException("Creator is required to build a new conversation.");
            }
            for (Activity activity : conversation.getActivities().getItems()) {
                activity.setActor(creator);
            }

            Activity createActivity = Activity.create(creator);

            if (!TextUtils.isEmpty(kmsMessage))
                conversation.setEncryptedKmsMessage(kmsMessage);

            if (isOneOnOne)
                conversation.tags.add(ConversationTag.ONE_ON_ONE);

            conversation.getActivities().getItems().add(0, Activity.add(creator, creator, null));
            conversation.getActivities().getItems().add(0, createActivity);

            built = true;
            return conversation;
        }

        public Builder setClientTempId(String clientTempId) {
            conversation.setClientTempId(clientTempId);
            return this;
        }

        public void setEncryptedKmsMessage(String kmsMessage) {
            this.kmsMessage = kmsMessage;
        }

        public void setIsOneOnOne(boolean isOneOnOne) {
            this.isOneOnOne = isOneOnOne;
        }
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
            encryptionKeyUrl = key.getKeyUrl();
        }
        if (Strings.notEmpty(getSummary())) {
            setSummary(CryptoUtils.encryptToJwe(key, getSummary()));
            encryptionKeyUrl = key.getKeyUrl();
        }
        if (avatar != null) {
            avatar.encrypt(key);
            avatarEncryptionKeyUrl = key.getKeyUrl();
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (!TextUtils.isEmpty(getDisplayName())) {
            String plainText = CryptoUtils.decryptFromJwe(key, getDisplayName());
            if (!TextUtils.isEmpty(plainText)) {
                setDisplayName(plainText);
                encryptionKeyUrl = key.getKeyUrl();
            }
        }
        if (!TextUtils.isEmpty(getSummary())) {
            String plainText = CryptoUtils.decryptFromJwe(key, getSummary());
            if (!TextUtils.isEmpty(plainText)) {
                setSummary(plainText);
                encryptionKeyUrl = key.getKeyUrl();
            }
        }
        if (avatar != null) {
            avatar.decrypt(key);
            avatarEncryptionKeyUrl = key.getKeyUrl();
        }
    }

    @Override
    public String toString() {
        if (isOneOnOne())
            return getId() + " (1-1)";
        return getId() + " (group)";
    }
}
