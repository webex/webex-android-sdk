package com.ciscowebex.androidsdk;

import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.space.Space;

import java.util.Date;

public interface WebexEvent {

    /**
     * Returns the payload of event.
     *
     * @return The payload of event.
     * @since 2.3.0
     */
    Payload getPayload();

    abstract class Base implements WebexEvent {

        WebexEvent.Payload payload;

        public Base(WebexEvent.Payload payload) {
            this.payload = payload;
        }

        /**
         * Returns the payload of event.
         *
         * @return The payload of event.
         * @since 2.3.0
         */
        @Override
        public Payload getPayload() {
            return payload;
        }
    }

    interface Data {

    }

    enum Type {
        CREATED, DELETED, UPDATED, SEEN;
    }

    enum Resource {
        MEMBERSHIPS, MESSAGES, SPACES;

        static Resource fromData(Data data) {
            if (data instanceof Message) {
                return MESSAGES;
            } else if (data instanceof Membership) {
                return MEMBERSHIPS;
            } else if (data instanceof Space) {
                return SPACES;
            }
            return null;
        }
    }

    class Payload {

        private String _actorId;

        private String _createdBy;

        private String _orgId;

        private Type _event;

        private Resource _resource;

        private Data _data;

        private Date _created = new Date();

        private String _ownedBy = "creator";

        private String _status = "active";

        protected Payload(Activity activity, AuthenticatedUser user, Data data) {
            Person person = activity.getActor();
            if (person != null) {
                _actorId = new WebexId(WebexId.Type.PEOPLE_ID, person.getId()).toHydraId();
            }
            if (user != null) {
                _createdBy = new WebexId(WebexId.Type.PEOPLE_ID, user.getUserId()).toHydraId();
                _orgId = new WebexId(WebexId.Type.ORGANIZATION_ID, user.getOrgId()).toHydraId();
            }
            _data = data;
            _resource = Resource.fromData(data);
            switch (activity.getVerb()) {
                case Verb.add:
                case Verb.create:
                case Verb.post:
                case Verb.share:
                    this._event = Type.CREATED;
                    break;
                case Verb.leave:
                case Verb.delete:
                    this._event = Type.DELETED;
                    break;
                case Verb.assignModerator:
                case Verb.unassignModerator:
                case Verb.hide:
                case Verb.update:
                    this._event = Type.UPDATED;
                    break;
                case Verb.acknowledge:
                    this._event = Type.SEEN;
                    break;
            }
        }

        public String getActorId() {
            return _actorId;
        }

        public String getCreatedBy() {
            return _createdBy;
        }

        public String getOrgId() {
            return _orgId;
        }

        public Type getEvent() {
            return _event;
        }

        public Resource getResource() {
            return _resource;
        }

        public Data getData() {
            return _data;
        }

        public Date getCreated() {
            return _created;
        }

        public String getOwnedBy() {
            return _ownedBy;
        }

        public String getStatus() {
            return _status;
        }
    }

}
