package com.cisco.spark.android.room;

import android.net.Uri;

import com.cisco.spark.android.lyra.BindingResponses;
import com.cisco.spark.android.lyra.model.Identity;
import com.cisco.spark.android.lyra.model.Links;
import com.cisco.spark.android.lyra.model.LyraSpaceOccupants;
import com.cisco.spark.android.lyra.model.LyraSpaceSessions;
import com.cisco.spark.android.model.AudioState;

import java.util.UUID;

public class LyraSpaceResponse {

    private Uri url;
    private Identity identity;
    private LyraSpaceOccupants occupants;
    private LyraSpaceSessions sessions;
    private AudioState audio;
    private BindingResponses binding;
    private Links links;

    public Uri getUrl() {
        return url;
    }

    public Identity getIdentity() {
        return identity;
    }

    public LyraSpaceOccupants getOccupants() {
        return occupants;
    }

    public LyraSpaceSessions getSessions() {
        return sessions;
    }

    public AudioState getAudio() {
        return audio;
    }

    public BindingResponses getBindings() {
        return binding;
    }

    public Links getLinks() {
        return links;
    }

    public UUID getId() {
        return identity.getId();
    }

    public String getDisplayName() {
        return identity.getDisplayName();
    }
}
