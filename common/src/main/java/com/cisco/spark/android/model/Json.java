package com.cisco.spark.android.model;

import android.net.Uri;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusKeyTypeAdapter;
import com.cisco.spark.android.meetings.WhistlerLoginType;
import com.cisco.spark.android.meetings.WhistlerLoginTypeAdapter;
import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryDataObjectTypeAdapter;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.mercury.MercuryEventTypeAdapter;
import com.cisco.spark.android.presence.PresenceStatus;
import com.cisco.spark.android.presence.PresenceStatusTypeAdapter;
import com.cisco.spark.android.roap.model.RoapBaseMessage;
import com.cisco.spark.android.roap.model.RoapMessageTypeAdapter;
import com.cisco.spark.android.sync.ParticipantUpdate;
import com.cisco.spark.android.sync.ParticipantUpdateTypeAdapter;
import com.cisco.wx2.diagnostic_events.EventType;
import com.cisco.wx2.diagnostic_events.EventTypeDeserializer;
import com.cisco.wx2.diagnostic_events.InstantConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.Instant;

import java.util.Date;

public class Json {
    private Json() {
    }

    public static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(ActivityObject.class, new ActivityObjectTypeAdapter())
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .registerTypeHierarchyAdapter(LocusKey.class, new LocusKeyTypeAdapter())
                .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter())
                .registerTypeAdapter(MercuryData.class, new MercuryDataObjectTypeAdapter())
                .registerTypeAdapter(MercuryEventType.class, new MercuryEventTypeAdapter())
                .registerTypeAdapter(ParticipantUpdate.class, new ParticipantUpdateTypeAdapter())
                .registerTypeAdapter(PresenceStatus.class, new PresenceStatusTypeAdapter())
                .registerTypeAdapter(WhistlerLoginType.class, new WhistlerLoginTypeAdapter())
                .registerTypeAdapter(RoapBaseMessage.class, new RoapMessageTypeAdapter())
                .registerTypeAdapter(Instant.class, new InstantConverter())
                .registerTypeAdapter(EventType.class, new EventTypeDeserializer())
                .create();
    }
}
