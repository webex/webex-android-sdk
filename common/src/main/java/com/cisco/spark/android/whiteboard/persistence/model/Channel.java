package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.crypto.scr.SecureContentReference;

import java.util.Map;
import java.util.UUID;

public class Channel implements Comparable<Channel> {
    private String channelId;
    private Uri channelUrl;
    private String parentId;
    private String type;
    private UUID creatorId;
    private Long createdTime;
    private Map<String, String> properties;

    // This is the whiteboard ACL URL
    private String aclUrl;
    private String kmsMessage;
    private Uri defaultEncryptionKeyUrl;
    private Uri kmsResourceUrl;
    private Long contentLastUpdatedTime;
    private Uri hiddenSpaceUrl;
    private Uri conversationUrl;
    private ChannelImage image;

    // This is the conversation ACL URL
    private Uri aclUrlLink;
    private Long imageUpdateInterval;

    public Channel(String conversationId) {
        this();
        this.aclUrl = conversationId;
    }

    public Channel() {
        this.type = "whiteboard";
    }

    public boolean isUsingOldEncryption() { //the board is in a room BUT was created not using the acl TODO remove when transitioned
        return conversationUrl != null && aclUrlLink == null;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelUrl() {
        return channelUrl.toString();
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setChannelUrl(Uri channelUrl) {
        this.channelUrl = channelUrl;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Uri getDefaultEncryptionKeyUrl() {
        return defaultEncryptionKeyUrl;
    }

    public void setDefaultEncryptionKeyUrl(Uri defaultEncryptionKeyUrl) {
        this.defaultEncryptionKeyUrl = defaultEncryptionKeyUrl;
    }

    public void setKmsResourceUrl(Uri kmsResourceUrl) {
        this.kmsResourceUrl = kmsResourceUrl;
    }

    public Uri getKmsResourceUrl() {
        return kmsResourceUrl;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

    public void setKmsMessage(String kmsMessage) {
        this.kmsMessage = kmsMessage;
    }

    public String getAclUrl() {
        return aclUrl;
    }

    public void setAclUrl(String aclUrl) {
        this.aclUrl = aclUrl;
    }

    public String getBoardAclId() {
        return aclUrl.substring(aclUrl.lastIndexOf('/') + 1);
    }

    public String getType() {
        return type;
    }

    public Long getContentLastUpdatedTime() {
        return contentLastUpdatedTime;
    }

    public void setContentLastUpdatedTime(Long contentLastUpdatedTime) {
        this.contentLastUpdatedTime = contentLastUpdatedTime;
    }

    public Uri getHiddenSpaceUrl() {
        return hiddenSpaceUrl;
    }

    public void setHiddenSpaceUrl(Uri hiddenSpaceUrl) {
        this.hiddenSpaceUrl = hiddenSpaceUrl;
    }

    public ChannelImage getImage() {
        return image;
    }

    public void setImage(ChannelImage image) {
        this.image = image;
    }

    public Uri getAclUrlLink() {
        return aclUrlLink;
    }

    public void setAclUrlLink(Uri aclUrlLink) {
        this.aclUrlLink = aclUrlLink;
    }

    public long getImageUpdateInterval() {
        return imageUpdateInterval;
    }

    public void setImageUpdateInterval(long imageUpdateInterval) {
        this.imageUpdateInterval = imageUpdateInterval;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public int compareTo(@NonNull Channel another) {
        return getContentLastUpdatedTime().compareTo(another.getContentLastUpdatedTime());
    }

    public boolean isPrivateChannel() {
        return aclUrlLink == null && conversationUrl == null;
    }

    public SecureContentReference getAvailableSnapShotSecureContentReference() {
        SecureContentReference secureContentReference = null;
        if (image != null) {
            if (image.getThumbnail() != null && image.getThumbnail().getSecureContentReference() != null) {
                return image.getThumbnail().getSecureContentReference();
            }
            return image.getSecureContentReference();
        }
        return null;
    }

    //ouuuh reflection. Used to PATCH a channel locally
    public void merge(Channel other) {
        this.channelId = other.channelId == null ? this.channelId : other.channelId;
        this.channelUrl = other.channelUrl == null ? this.channelUrl : other.channelUrl;
        this.aclUrl = other.aclUrl == null ? this.aclUrl : other.aclUrl;
        this.type = other.type == null ? this.type : other.type;
        this.creatorId = other.creatorId == null ? this.creatorId : other.creatorId;
        this.properties = other.properties == null ? this.properties : other.properties;
        //this.kmsMessage = other.kmsMessage == null ? this.kmsMessage : other.kmsMessage;
        //this.defaultEncryptionKeyUrl = other.defaultEncryptionKeyUrl == null ? this.defaultEncryptionKeyUrl : other.defaultEncryptionKeyUrl;
        this.kmsResourceUrl = other.kmsResourceUrl == null ? this.kmsResourceUrl : other.kmsResourceUrl;
        this.contentLastUpdatedTime = other.contentLastUpdatedTime == null ? this.contentLastUpdatedTime : other.contentLastUpdatedTime;
        this.hiddenSpaceUrl = other.hiddenSpaceUrl == null ? this.hiddenSpaceUrl : other.hiddenSpaceUrl;
        this.conversationUrl = other.conversationUrl == null ? this.conversationUrl : other.conversationUrl;
        this.image = other.image == null ? this.image : other.image;
        this.aclUrlLink = other.aclUrlLink == null ? this.aclUrlLink : other.aclUrlLink;
        this.imageUpdateInterval = other.imageUpdateInterval == null ? this.imageUpdateInterval : other.imageUpdateInterval;
    }

    // Auto-generated

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Channel channel = (Channel) o;

        if (channelId != null ? !channelId.equals(channel.channelId) : channel.channelId != null) {
            return false;
        }
        if (channelUrl != null ? !channelUrl.equals(channel.channelUrl) : channel.channelUrl != null) {
            return false;
        }
        if (parentId != null ? !parentId.equals(channel.parentId) : channel.parentId != null) {
            return false;
        }
        if (type != null ? !type.equals(channel.type) : channel.type != null) {
            return false;
        }
        if (creatorId != null ? !creatorId.equals(channel.creatorId) : channel.creatorId != null) {
            return false;
        }
        if (createdTime != null ? !createdTime.equals(channel.createdTime) : channel.createdTime != null) {
            return false;
        }
        if (properties != null ? !properties.equals(channel.properties) : channel.properties != null) {
            return false;
        }
        if (aclUrl != null ? !aclUrl.equals(channel.aclUrl) : channel.aclUrl != null) {
            return false;
        }
        if (kmsMessage != null ? !kmsMessage.equals(channel.kmsMessage) : channel.kmsMessage != null) {
            return false;
        }
        if (defaultEncryptionKeyUrl != null ? !defaultEncryptionKeyUrl.equals(channel.defaultEncryptionKeyUrl) :
                    channel.defaultEncryptionKeyUrl != null) {
            return false;
        }
        if (kmsResourceUrl != null ? !kmsResourceUrl.equals(channel.kmsResourceUrl) : channel.kmsResourceUrl != null) {
            return false;
        }
        if (contentLastUpdatedTime != null ? !contentLastUpdatedTime.equals(channel.contentLastUpdatedTime) :
                    channel.contentLastUpdatedTime != null) {
            return false;
        }
        if (hiddenSpaceUrl != null ? !hiddenSpaceUrl.equals(channel.hiddenSpaceUrl) : channel.hiddenSpaceUrl != null) {
            return false;
        }
        if (conversationUrl != null ? !conversationUrl.equals(channel.conversationUrl) :
                    channel.conversationUrl != null) {
            return false;
        }
        if (image != null ? !image.equals(channel.image) : channel.image != null) {
            return false;
        }
        if (aclUrlLink != null ? !aclUrlLink.equals(channel.aclUrlLink) : channel.aclUrlLink != null) {
            return false;
        }
        return imageUpdateInterval != null ? imageUpdateInterval.equals(channel.imageUpdateInterval) :
                       channel.imageUpdateInterval == null;

    }

    @Override
    public int hashCode() {
        int result = channelId != null ? channelId.hashCode() : 0;
        result = 31 * result + (channelUrl != null ? channelUrl.hashCode() : 0);
        result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (creatorId != null ? creatorId.hashCode() : 0);
        result = 31 * result + (createdTime != null ? createdTime.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (aclUrl != null ? aclUrl.hashCode() : 0);
        result = 31 * result + (kmsMessage != null ? kmsMessage.hashCode() : 0);
        result = 31 * result + (defaultEncryptionKeyUrl != null ? defaultEncryptionKeyUrl.hashCode() : 0);
        result = 31 * result + (kmsResourceUrl != null ? kmsResourceUrl.hashCode() : 0);
        result = 31 * result + (contentLastUpdatedTime != null ? contentLastUpdatedTime.hashCode() : 0);
        result = 31 * result + (hiddenSpaceUrl != null ? hiddenSpaceUrl.hashCode() : 0);
        result = 31 * result + (conversationUrl != null ? conversationUrl.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (aclUrlLink != null ? aclUrlLink.hashCode() : 0);
        result = 31 * result + (imageUpdateInterval != null ? imageUpdateInterval.hashCode() : 0);
        return result;
    }
}
