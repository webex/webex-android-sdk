/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import me.helloworld.utils.Checker;

import java.util.Comparator;

public class ActivityModel extends ObjectModel {

    public enum Verb {
        create, add, remove, post, acknowledge, update, updateKey, leave,
        mute, unmute, favorite, unfavorite, share, start, join, decline, reject,
        cancel, terminate, schedule, hide, unhide, lock, unlock, assignModerator, unassignModerator,
        delete, tombstone, archive, unarchive,
        tag, untag, assign,  unassign, noops,
        addMicroappInstance, deleteMicroappInstance, set, unset
    }

    private ObjectModel target;
    private ObjectModel object;
    private PersonModel actor;
    private ProviderModel provider;
    private Verb verb;
    private PlaceModel location;
    private String encryptionKeyUrl;
    private ParentModel parent;

    private transient boolean isEncrypted;

    public static final Comparator<ActivityModel> ASCENDING_PUBLISH_TIME_COMPARATOR = (rhs, lhs) -> {
        int result = rhs.getPublished().compareTo(lhs.getPublished());
        if (result == 0 && lhs.getVerb() == Verb.acknowledge) {
            return -1;
        } else if (result == 0 && rhs.getVerb() == Verb.acknowledge) {
            return 1;
        }
        return result;
    };

    public static final Comparator<ActivityModel> DESCENDING_PUBLISH_TIME_COMPARATOR = (rhs, lhs) -> ASCENDING_PUBLISH_TIME_COMPARATOR.compare(lhs, rhs);

    public ActivityModel(String type) {
        super(Type.activity);
    }

    public ParentModel getParent() {
        return parent;
    }

    public boolean isReply() {
        return parent != null && parent.isReply();
    }

    public ObjectModel getTarget() {
        return target;
    }

    public ObjectModel getObject() {
        return object;
    }

    public PersonModel getActor() {
        return actor;
    }

    public Verb getVerb() {
        return verb;
    }

    public ProviderModel getProvider() {
        return provider;
    }

    public PlaceModel getLocation() {
        return location;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public boolean isFromSelf(@NonNull String personId) {
        PersonModel person = getActor();
        return person != null && personId.equals(person.getId());
    }

    public boolean isAddParticipant() {
        return verb == Verb.add && object != null && object.isPerson() && target != null && target.isConversation();
    }

    public boolean isCreateConversation() {
        return verb == Verb.create && object != null && object.isConversation();
    }

    public boolean isTag() {
        return verb == Verb.tag && object != null && object.isConversation();
    }

    public boolean isUnTag() {
        return verb == Verb.untag && object != null && object.isConversation();
    }

    public boolean isPostOrAddComment() {
        return (verb == Verb.post || verb == Verb.add)
                && object != null && object.isComment()
                && target != null && target.isConversation();
    }

    public boolean isPostOrShareContent() {
        return (verb == Verb.post || verb == Verb.share)
                && object != null && object.isContent()
                && target != null && target.isConversation();
    }

    public boolean isShareFile() {
        return verb == Verb.share && object instanceof ContentModel && (((ContentModel) object).isFile() || ((ContentModel) object).isVideo());
    }

    public boolean isShareImage() {
        return verb == Verb.share && object instanceof ContentModel &&  ((ContentModel) object).isImage();
    }

    public boolean isUpdateTitleAndSummaryActivity() {
        return verb == Verb.update && object != null && object.isConversation();
    }

    public boolean isSetTeamColor() {
        return verb == Verb.update && object != null && object.isTeamObject();
    }

    public boolean isMoveRoomToTeam() {
        return verb == Verb.add && object != null && object.isConversation();
    }

    public boolean isRemoveRoomFromTeam() {
        return verb == Verb.remove && object != null && object.isConversation();
    }

    public boolean isLocusActivity() {
        return object != null && object.isLocus();
    }

    public boolean isLeaveActivity() {
        return verb == Verb.leave && (object == null || object.isPerson()) && target != null && target.isConversation();
    }

    public boolean isUpdateContent() {
        return verb == Verb.update && object != null && object.isContent();
    }

    public boolean isUpdateKeyActivity() {
        return verb == Verb.updateKey && object != null && object.isConversation();
    }

    public boolean isSetAnnouncement() {
        return verb == Verb.set && object != null && object.isSpaceProperty() && ((SpacePropertyModel) object).containsTag(SpacePropertyModel.Tag.ANNOUNCEMENT);
    }

    public boolean isUnsetAnnouncement() {
        return verb == Verb.unset && object != null && object.isSpaceProperty() && ((SpacePropertyModel) object).containsTag(SpacePropertyModel.Tag.ANNOUNCEMENT);
    }

    public boolean isAssignRoomAvatar() {
        return verb == Verb.assign && object != null && object.isContent();
    }

    public boolean isUnassignRoomAvatar() {
        return verb == Verb.unassign && object != null && object.isContent();
    }

    public boolean isLocusSessionSummary() {
        return verb == Verb.update
                && object != null
                && object.isLocusSessionSummary()
                && target.getId() != null;
    }

    public boolean isAddNewTeamConversation() {
        return verb == Verb.add
                && object != null && object.isConversation()
                && target != null && (target.isConversation() || target.isTeamObject());
    }

    public String getContentDataId() {
        if (object != null && object.isContent()) {
            return getObject().getId();
        }
        return null;
    }

    public String getSparkMeetingId() {
        if (object != null && object.isEvent()) {
            return getObject().getId();
        }
        return null;
    }

    public String getConversationId() {
        ObjectModel target = getTarget();
        ObjectModel object = getObject();
        if (target != null &&  ObjectModel.Type.conversation.equals(target.getObjectType())) {
            return target.getId();
        }
        else if (target != null && ObjectModel.Type.team.equals(target.getObjectType())) {
            return ((TeamModel) target).getGeneralConversationUuid();
        }
        else if (object != null && ObjectModel.Type.conversation.equals(object.getObjectType())) {
            return object.getId();
        }
        else if (object != null && ObjectModel.Type.team.equals(object.getObjectType())) {
            return ((TeamModel) object).getGeneralConversationUuid();
        } else {
            return null;
        }
    }

    public String getConversationUrl() {
        ObjectModel target = getTarget();
        ObjectModel object = getObject();
        if (target != null && ObjectModel.Type.conversation.equals(target.getObjectType())) {
            return target.getUrl();
        }
        else if (object != null && ObjectModel.Type.conversation.equals(object.getObjectType())) {
            return object.getUrl();
        }
        else {
            String activityUrl = this.getUrl();
            return activityUrl.substring(0, activityUrl.lastIndexOf("/activities/")) + "/conversations/" + getConversationId();
        }
    }

    @Override
    public String toString() {
        return "Activity " + getId() + " " + getVerb() +
                " target{" + getTarget() +
                "} object{" + getObject() +
                "} actor:" + getActor() +
                " published:" + (getPublished() == null ? "0" : String.valueOf(getPublished().getTime()));
    }

    @Override
    public void encrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.encrypt(key);
        if (getObject() != null) {
            getObject().encrypt(key);
        }
        if (getTarget() != null) {
            getTarget().setDisplayName(null);
        }
    }

    @Override
    public void decrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.decrypt(key);
        if (getObject() != null) {
            getObject().decrypt(key);
        }
    }

    // Returns the publish time of the activity, or of the acknowledged activity if this is an ack
    public long getAckTime() {
        long ret = getPublished().getTime();
        if (verb == Verb.acknowledge && getObject() != null && getObject().getPublished().getTime() != 0) {
            ret = getObject().getPublished().getTime();
        }
        return ret;
    }

    public boolean isSelfMention(@NonNull Credentials credentials, long lastJoinedDate) {
        return isPersonallyMentioned(credentials) || isIncludedInGroupMention(credentials, lastJoinedDate);
    }

    private boolean isPersonallyMentioned(@NonNull Credentials user) {
        if (!(getObject() instanceof MentionableModel)) {
            return false;
        }
        MentionableModel object = (MentionableModel) getObject();
        if (object.getMentions() == null) {
            return false;
        }
        for (PersonModel mention : object.getMentions().getItems()) {
            if (Checker.isEqual(mention.getId(), user.getUserId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isIncludedInGroupMention(@NonNull Credentials credentials, long lastJoinedDate) {
        if (!(getObject() instanceof  MentionableModel)) {
            return false;
        }
        MentionableModel object = (MentionableModel) getObject();
        if (object.getGroupMentions() == null || object.getGroupMentions().size() == 0) {
            return false;
        }
        for (GroupMentionModel mention : object.getGroupMentions().getItems()) {
            // @All and @Here mentions apply to a user if the publish time on the activity is after the lastJoinedDate for the conversation
            if ((mention.getGroupType() == GroupMentionModel.Type.ALL || mention.getGroupType() == GroupMentionModel.Type.HERE) &&
                    !isFromSelf(credentials.getUserId()) &&
                    getPublished().getTime() >= lastJoinedDate) {
                return true;
            }
        }
        return false;
    }
}
