package com.cisco.spark.android.locus.model;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class LocusSelfRepresentation extends LocusParticipant {
    public static class AlertType {
        public final static String ALERT_NONE  = "NONE";    // No Alert or stop alerting
        public final static String ALERT_FULL  = "FULL";    // Audible and visual alert

        private String action;
        private Date expiration;

        public AlertType() {  }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Date getExpiration() {
            return expiration;
        }

        public void setExpiration(Date expiration) {
            this.expiration = expiration;
        }
    }

    private AlertType alertType;
    private boolean enableDTMF;

    public LocusSelfRepresentation() {
    }

    public boolean isEnableDTMF() {
        return enableDTMF;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }


    public static class Builder  {
        private LocusSelfRepresentation self;

        public Builder() {
            self = new LocusSelfRepresentation();
        }

        public Builder setId(UUID id) {
            self.id = id;
            return this;
        }

        /**
         * Plain UnitTests cannot use the android.net.Uri field so use caution, and only set this
         * field in Robolectric tests.
         */
        public Builder setUri(String uri) {
            self.url = Uri.parse(uri);
            return this;
        }

        public Builder setPerson(LocusParticipantInfo person) {
            self.person = person;
            return this;
        }

        public Builder setState(State state) {
            self.state = state;
            return this;
        }

        public Builder setParticipantControls(LocusParticipantControls controls) {
            self.controls = controls;
            return this;
        }

        public Builder addIntent(Intent intent) {
            if (self.intents == null) {
                self.intents = new ArrayList<>();
            }
            self.intents.add(intent);
            return this;
        }

        public Builder setAlertType(AlertType alertType) {
            self.alertType = alertType;
            return this;
        }

        public Builder setType(Type type) {
            self.type = type;
            return this;
        }

        public Builder setStatus(LocusParticipantStatus status) {
            self.status = status;
            return this;
        }

        public Builder setDeviceUrl(Uri url) {
            self.deviceUrl = url;
            return this;
        }

        public Builder addDevice(LocusParticipantDevice device) {
            if (self.devices == null) {
                self.devices = new ArrayList<>();
            }
            self.devices.add(device);
            return this;
        }

        public Builder addSuggestedMedia(SuggestedMedia suggestedMedia) {
            if (self.suggestedMedia == null) {
                self.suggestedMedia = new ArrayList<>();
            }

            self.suggestedMedia.add(suggestedMedia);
            return this;
        }

        public LocusSelfRepresentation build() {
            return self;
        }

        /**
         * Plain UnitTests cannot use the android.net.Uri field so use caution, and only set this
         * field in Robolectric tests.
         */
        public Builder withInvitedBy(LocusParticipant inviter) {
            self.invitedBy = inviter.url;
            self.guest = true;
            return this;
        }
    }
}
