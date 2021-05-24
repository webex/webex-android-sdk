/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import com.ciscowebex.androidsdk.internal.crypto.KmsResourceObject;
import com.google.gson.annotations.SerializedName;
import me.helloworld.utils.Checker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConversationModel extends ObjectModel {

    public enum Tag {
        MUTED,
        ONE_ON_ONE,
        FAVORITE,
        HIDDEN,
        LOCKED,
        TEAM,   // If a conversation has the TEAM tag it is the primary conversation for a team. This
        //   means that the participant list is the list of members of the team, and the
        //   conversation is the 'general' team conversation
        OPEN,   // If a conversation has the OPEN tag it is a conversation that is open to all members
        //   of a team that it is associated with, but is not the primary conversation
        NOT_JOINED,  // This means the conversation is a team room that the self user hasn't joined
        ARCHIVED,
        MESSAGE_NOTIFICATIONS_ON,
        MESSAGE_NOTIFICATIONS_OFF,
        MENTION_NOTIFICATIONS_ON,
        MENTION_NOTIFICATIONS_OFF,
        ANNOUNCEMENT;

        public static Tag safeValueOf(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                // no such thing
                return null;
            }
        }
    }

    private ItemsModel<PersonModel> participants = new ItemsModel<>();
    private ItemsModel<ActivityModel> activities = new ItemsModel<>();
    @SerializedName("tags")
    private List<String> tagNames = new ArrayList<>();
    private String locusUrl;
    private String summary;
    private String creatorUUID;
    private Date lastRelevantActivityDate;
    private TeamModel team;
    private Boolean isTeamGuestMember;
    private ContentModel avatar;
    private String avatarEncryptionKeyUrl;
    private int shareCount;
    private String aclUrl;

    private String defaultActivityEncryptionKeyUrl;
    private transient boolean isEncrypted;
    private String encryptionKeyUrl;
    private String kmsResourceObjectUrl;
    private String encryptedKmsMessage;

    private Date lastReadableActivityDate;
    private Date lastSeenActivityDate;
    private Date lastJoinedDate;

    private String retentionUrl;
    private OrganizationModel custodianOrg;
    private ItemsModel<OrganizationPolicyModel> organizationPolicies = new ItemsModel<>();
    private transient boolean hasTags = true;

    public String getDefaultActivityEncryptionKeyUrl() {
        return defaultActivityEncryptionKeyUrl;
    }

    public void setDefaultActivityEncryptionKeyUrl(String defaultActivityEncryptionKeyUrl) {
        this.defaultActivityEncryptionKeyUrl = defaultActivityEncryptionKeyUrl;
    }

    public ConversationModel() {
        super(ObjectModel.Type.conversation);
    }

    public ConversationModel(String id) {
        this();
        setId(id);
    }

    public ItemsModel<PersonModel> getParticipants() {
        return participants;
    }

    public void setParticipants(ItemsModel<PersonModel> participants) {
        this.participants = participants;
    }

    public ItemsModel<ActivityModel> getActivities() {
        return activities;
    }

    public void setActivities(ItemsModel<ActivityModel> activities) {
        this.activities = activities;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isMuted() {
        return containsTag(ConversationModel.Tag.MUTED);
    }

    public boolean isFavorite() {
        return containsTag(ConversationModel.Tag.FAVORITE);
    }

    public boolean isHidden() {
        return containsTag(ConversationModel.Tag.HIDDEN);
    }

    public boolean isArchived() {
        return containsTag(ConversationModel.Tag.ARCHIVED);
    }

    public boolean isLocked() {
        return containsTag(ConversationModel.Tag.LOCKED);
    }

    public boolean isInAnnouncementMode() {
        return containsTag(ConversationModel.Tag.ANNOUNCEMENT);
    }

    public boolean isTeam() {
        return containsTag(ConversationModel.Tag.TEAM);
    }

    public boolean isTeamGuestMember() {
        return (isTeamGuestMember == null) ? false : isTeamGuestMember;
    }

    public boolean isOpen() {
        return containsTag(ConversationModel.Tag.OPEN);
    }

    public boolean isJoined() {
        return !containsTag(ConversationModel.Tag.NOT_JOINED);
    }

    public void setIsOpen(boolean isOpen) {
        setTag(ConversationModel.Tag.OPEN, isOpen);
    }

    public void setIsTeam(boolean isTeam) {
        setTag(ConversationModel.Tag.TEAM, isTeam);
    }

    public void setIsMuted(boolean isMuted) {
        setTag(ConversationModel.Tag.MUTED, isMuted);
    }

    public Boolean isMessageNotificationsOn() {
        return getNotificationTagTristate(ConversationModel.Tag.MESSAGE_NOTIFICATIONS_ON, ConversationModel.Tag.MESSAGE_NOTIFICATIONS_OFF);
    }

    public Boolean isMentionNotificationsOn() {
        return getNotificationTagTristate(ConversationModel.Tag.MENTION_NOTIFICATIONS_ON, ConversationModel.Tag.MENTION_NOTIFICATIONS_OFF);
    }


    @Nullable
    private Boolean getNotificationTagTristate(ConversationModel.Tag notificationOnTag, ConversationModel.Tag notificationOffTag) {
        final boolean onSet = containsTag(notificationOnTag);
        final boolean offSet = containsTag(notificationOffTag);

        if (onSet) {
            return true;
        } else if (offSet) {
            return false;
        } else {
            return null;
        }
    }

    public boolean containsTag(ConversationModel.Tag tag) {
        return tagNames.contains(tag.name());
    }

    public void setTag(ConversationModel.Tag tag, boolean shouldExist) {
        if (shouldExist) {
            if (!tagNames.contains(tag.name()))
                tagNames.add(tag.name());
        } else {
            tagNames.remove(tag.name());
        }
    }

    public List<ConversationModel.Tag> getTags() {
        if (tagNames == null)
            return null;

        List<ConversationModel.Tag> ret = new ArrayList<>();
        for (String name : tagNames) {
            ConversationModel.Tag tag = ConversationModel.Tag.safeValueOf(name);
            if (tag != null)
                ret.add(tag);
        }
        return ret;
    }

    public boolean isOneOnOne() {
        return containsTag(ConversationModel.Tag.ONE_ON_ONE);
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public String getLocusUrl() {
        return locusUrl;
    }

    public void setLocusUrl(String locusUrl) {
        this.locusUrl = locusUrl;
    }

    public String getCreatorUUID() {
        return creatorUUID;
    }

    public String getTitleEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public Date getLastRelevantActivityTimestamp() {
        return lastRelevantActivityDate;
    }

    public TeamModel getTeam() {
        return team;
    }

    public void setTeam(TeamModel team) {
        this.team = team;
    }

    public String getAvatarEncryptionKeyUrl() {
        return avatarEncryptionKeyUrl;
    }

    public ContentModel getAvatar() {
        return avatar;
    }

    public KmsResourceObject getKmsResourceObject() {
        if (kmsResourceObjectUrl == null) {
            return null;
        }
        return new KmsResourceObject(kmsResourceObjectUrl);
    }

    public void setKmsResourceObjectUrl(String kmsResourceObjectUrl) {
        this.kmsResourceObjectUrl = kmsResourceObjectUrl;
    }

    public Date getLastReadableActivityDate() {
        return lastReadableActivityDate;
    }

    public Date getLastSeenActivityDate() {
        return lastSeenActivityDate;
    }

    public Date getLastJoinedDate() {
        return lastJoinedDate;
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

    public void setLastJoinedDate(Date lastJoinedDate) {
        this.lastJoinedDate = lastJoinedDate;
    }

    public OrganizationModel getCustodianOrg() {
        return custodianOrg;
    }

    public void setCustodianOrg(OrganizationModel custodianOrg) {
        this.custodianOrg = custodianOrg;
    }

    public String getRetentionUrl() {
        return retentionUrl;
    }

    public void setRetentionUrl(String retentionUrl) {
        this.retentionUrl = retentionUrl;
    }

    public ItemsModel<OrganizationPolicyModel> getOrganizationPolicies() {
        return organizationPolicies;
    }

    public void setOrganizationPolicies(ItemsModel<OrganizationPolicyModel> organizationPolicies) {
        this.organizationPolicies = organizationPolicies;
    }

    public void updateWithHeadersFrom(ConversationModel conversation) {
        if (conversation == null) {
            return;
        }
        if (getLastReadableActivityDate() == null) {
            setLastReadableActivityDate(conversation.getLastReadableActivityDate());
        } else if (conversation.getLastReadableActivityDate() != null && getLastReadableActivityDate().before(conversation.getLastReadableActivityDate())) {
            setLastReadableActivityDate(conversation.getLastReadableActivityDate());
        }
    }

    public String getAclUrl() {
        return aclUrl;
    }

    public void setAclUrl(String aclUrl) {
        this.aclUrl = aclUrl;
    }

    public void setHasTags(boolean hasTags) {
        this.hasTags = hasTags;
    }

    public boolean hasTags() {
        return hasTags;
    }

    @Override
    public void encrypt(KeyObject key){
        if (key == null) {
            return;
        }
        super.encrypt(key);
        if (!Checker.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
            encryptionKeyUrl = key.getKeyUrl();
        }
        if (!Checker.isEmpty(getSummary())) {
            setSummary(CryptoUtils.encryptToJwe(key, getSummary()));
            encryptionKeyUrl = key.getKeyUrl();
        }
        if (avatar != null) {
            avatar.encrypt(key);
            avatarEncryptionKeyUrl = key.getKeyUrl();
        }
    }

    @Override
    public void decrypt(KeyObject key) {
        if (key == null) {
            return;
        }
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
        if (isOneOnOne()) {
            return getId() + " (1-1)";
        }
        return getId() + " (group)";
    }

}
