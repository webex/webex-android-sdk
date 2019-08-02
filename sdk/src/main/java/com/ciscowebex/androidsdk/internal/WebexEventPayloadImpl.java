package com.ciscowebex.androidsdk.internal;

import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.model.conversation.ActivityObject;
import com.cisco.spark.android.model.conversation.Content;
import com.cisco.spark.android.model.conversation.Conversation;
import com.cisco.spark.android.model.conversation.File;
import com.ciscowebex.androidsdk.WebexEventPayload;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.space.Space;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WebexEventPayloadImpl implements WebexEventPayload {

    @SerializedName("actorId")
    private String _actorId;

    @SerializedName("created")
    private Date _created;

    @SerializedName("createdBy")
    private String _createdBy;

    @SerializedName("event")
    private String _event;

    @SerializedName("orgId")
    private String _orgId;

    @SerializedName("ownedBy")
    private String _ownedBy;

    @SerializedName("resource")
    private String _resource;

    @SerializedName("status")
    private String _status;

    @SerializedName("payloadData")
    private WebexEventPayload.PayloadData _payloadData;

    public WebexEventPayloadImpl(Activity activity, AuthenticatedUser user, String resource) {
        if (null == activity) {
            Ln.e("WebexEventPayloadImpl activity is null");
            return;
        }
        Person person = activity.getActor();
        if (null != person)
            this._actorId = new WebexId(WebexId.Type.PEOPLE_ID, person.getId()).toHydraId();
        this._created = new Date();
        if (null != user) {
            this._createdBy = new WebexId(WebexId.Type.PEOPLE_ID, user.getUserId()).toHydraId();
            this._orgId = new WebexId(WebexId.Type.ORGANIZATION_ID, user.getOrgId()).toHydraId();
        }
        switch (activity.getVerb()) {
            case Verb.add:
            case Verb.create:
            case Verb.post:
            case Verb.share:
                this._event = "created";
                break;
            case Verb.leave:
            case Verb.delete:
                this._event = "deleted";
                break;
            case Verb.assignModerator:
            case Verb.unassignModerator:
            case Verb.hide:
            case Verb.update:
                this._event = "updated";
                break;
            case Verb.acknowledge:
                this._event = "seen";
                break;
        }
        this._ownedBy = "creator";
        this._resource = resource;
        this._status = "active";
        this._payloadData = new PayloadDataImpl(activity);
    }

    @Override
    public String getActorId() {
        return _actorId;
    }

    @Override
    public Date getCreated() {
        return _created;
    }

    @Override
    public String getCreatedBy() {
        return _createdBy;
    }

    @Override
    public String getEvent() {
        return _event;
    }

    @Override
    public String getOrgId() {
        return _orgId;
    }

    @Override
    public String getOwnedBy() {
        return _ownedBy;
    }

    @Override
    public String getResource() {
        return _resource;
    }

    @Override
    public String getStatus() {
        return _status;
    }

    @Override
    public PayloadData getPayloadData() {
        return _payloadData;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public class PayloadDataImpl implements WebexEventPayload.PayloadData {

        @SerializedName("created")
        private Date _created;

        @SerializedName("id")
        private String _id;

        @SerializedName("personEmail")
        private String _personEmail;

        @SerializedName("personId")
        private String _personId;

        @SerializedName("personDisplayName")
        private String _personDisplayName;

        @SerializedName("personOrgId")
        private String _personOrgId;

        @SerializedName("spaceId")
        private String _spaceId;

        @SerializedName("spaceType")
        private Space.SpaceType _spaceType;

        @SerializedName("text")
        private String _text;

        @SerializedName("files")
        private List<String> _files;

        @SerializedName("isModerator")
        private boolean _isModerator;

        @SerializedName("isSpaceHidden")
        private boolean _isSpaceHidden;

        @SerializedName("lastSeenId")
        private String _lastSeenId;

        public PayloadDataImpl(Activity activity) {
            ActivityObject activityObject = activity.getObject();
            if (null == activityObject) {
                Ln.e("PayloadDataImpl activityObject is null");
                return;
            }
            this._created = activity.getPublished();
            switch (_resource) {
                case "membership":
                    if (activity.getVerb().equals(Verb.hide)) {
                        this._spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getObject().getId()).toHydraId();
                        this._spaceType = Space.SpaceType.DIRECT;
                        this._isSpaceHidden = true;
                    } else {
                        this._spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getTarget().getId()).toHydraId();
                        this._spaceType = ((Conversation) activity.getTarget()).getTags().contains("ONE_ON_ONE") ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
                        this._isSpaceHidden = false;
                    }
                    Person person = null;
                    if (activity.getVerb().equals(Verb.acknowledge)) {
                        person = activity.getActor();
                        this._lastSeenId = new WebexId(WebexId.Type.MESSAGE_ID, activity.getObject().getId()).toHydraId();
                    } else if (activity.getObject() instanceof Person)
                        person = (Person) activity.getObject();
                    if (null != person) {
                        this._id = new WebexId(WebexId.Type.MEMBERSHIP_ID, person.getId() + ":" + WebexId.translate(this._spaceId)).toHydraId();
                        this._personId = new WebexId(WebexId.Type.PEOPLE_ID, person.getId()).toHydraId();
                        this._personEmail = person.getEmail();
                        this._personDisplayName = person.getDisplayName();
                        this._personOrgId = new WebexId(WebexId.Type.ORGANIZATION_ID, person.getOrgId()).toHydraId();
                        this._isModerator = person.getRoomProperties() != null && person.getRoomProperties().isModerator();
                    }
                    break;
                case "message":
                    this._spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getTarget().getId()).toHydraId();
                    this._spaceType = ((Conversation) activity.getTarget()).getTags().contains("ONE_ON_ONE") ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
                    if (null != activity.getActor()) {
                        this._personId = new WebexId(WebexId.Type.PEOPLE_ID, activity.getActor().getId()).toHydraId();
                        this._personEmail = activity.getActor().getEmail();
//                        this._personDisplayName = activity.getActor().getDisplayName();
//                        this._personOrgId = activity.getActor().getOrgId();
                    }
                    if (_event.equals("deleted")) {
                        this._id = new WebexId(WebexId.Type.MESSAGE_ID, activity.getObject().getId()).toHydraId();
                    } else {
                        this._id = new WebexId(WebexId.Type.MESSAGE_ID, activity.getId()).toHydraId();
                        this._text = activity.getObject().getDisplayName();
                    }
                    if (activity.getObject() instanceof Content) {
                        Content content = (Content) activity.getObject();
                        if (content.getFiles() != null && !content.getFiles().getItems().isEmpty()) {
                            this._files = new ArrayList<>();
                            for (File file : content.getFiles().getItems()) {
                                this._files.add(new WebexId(WebexId.Type.CONTENT_ID, file.getScr()).toHydraId());
                            }
                        }
                    }
                    break;
            }
        }

        @Override
        public Date getCreated() {
            return _created;
        }

        @Override
        public String getId() {
            return _id;
        }

        @Override
        public String getPersonEmail() {
            return _personEmail;
        }

        @Override
        public String getPersonId() {
            return _personId;
        }

        @Override
        public String getPersonDisplayName() {
            return _personDisplayName;
        }

        @Override
        public String getPersonOrgId() {
            return _personOrgId;
        }

        @Override
        public String getSpaceId() {
            return _spaceId;
        }

        @Override
        public Space.SpaceType getSpaceType() {
            return _spaceType;
        }

        @Override
        public String getText() {
            return _text;
        }

        @Override
        public List<String> getFiles() {
            return _files;
        }

        @Override
        public boolean isModerator() {
            return _isModerator;
        }

        @Override
        public boolean isSpaceHidden() {
            return _isSpaceHidden;
        }

        @Override
        public String getLastSeenId() {
            return _lastSeenId;
        }

        @Override
        public String toString() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }
}
