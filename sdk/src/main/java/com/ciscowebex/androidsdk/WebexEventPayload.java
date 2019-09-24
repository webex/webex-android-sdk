package com.ciscowebex.androidsdk;

import com.ciscowebex.androidsdk.space.Space;

import java.util.Date;
import java.util.List;

public interface WebexEventPayload {
    String getActorId();

    Date getCreated();

    String getCreatedBy();

    String getEvent();

    String getOrgId();

    String getOwnedBy();

    String getResource();

    String getStatus();

    PayloadData getPayloadData();

    interface PayloadData {

        Date getCreated();

        String getId();

        String getPersonEmail();

        String getPersonId();

        String getPersonDisplayName();

        String getPersonOrgId();

        String getSpaceId();

        Space.SpaceType getSpaceType();

        String getText();

        List<String> getFiles();

        boolean isModerator();

        boolean isSpaceHidden();

        String getLastSeenId();
    }
}
