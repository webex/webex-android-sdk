package com.cisco.spark.android.mercury;

import com.cisco.spark.android.log.Lns;
import com.cisco.spark.android.mercury.events.ConversationActivityEvent;
import com.cisco.spark.android.mercury.events.KeyPushEvent;
import com.cisco.spark.android.mercury.events.KmsAckEvent;
import com.cisco.spark.android.mercury.events.KmsMessageResponseEvent;
import com.cisco.spark.android.mercury.events.LocusChangedEvent;
import com.cisco.spark.android.mercury.events.LocusDeltaEvent;
import com.cisco.spark.android.mercury.events.LyraActivityEvent;
import com.cisco.spark.android.mercury.events.LyraSpaceAudioMicrophonesMuteActionEvent;
import com.cisco.spark.android.mercury.events.LyraSpaceAudioVolumeChangeEvent;
import com.cisco.spark.android.mercury.events.LyraSpaceAudioVolumeSetEvent;
import com.cisco.spark.android.mercury.events.RoomControlEvent;
import com.cisco.spark.android.mercury.events.RoomDeviceEnteredEvent;
import com.cisco.spark.android.mercury.events.RoomDeviceExitedEvent;
import com.cisco.spark.android.mercury.events.RoomIdentityChangedEvent;
import com.cisco.spark.android.mercury.events.RoomIdentityDeletedEvent;
import com.cisco.spark.android.mercury.events.RoomRenewRsuEvent;
import com.cisco.spark.android.mercury.events.RoomRequestLogsEvent;
import com.cisco.spark.android.mercury.events.RoomSetRoomControlUserEvent;
import com.cisco.spark.android.mercury.events.RoomSetUpgradeChannelEvent;
import com.cisco.spark.android.mercury.events.RoomUpdatedEvent;
import com.cisco.spark.android.mercury.events.UserFeatureUpdate;
import com.cisco.spark.android.mercury.events.UserRecentSessionsEvent;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.presence.PresenceStatusResponse;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.JsonUtils;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class MercuryDataObjectTypeAdapter implements JsonDeserializer<MercuryData> {
    @Override
    public MercuryData deserialize(JsonElement jsonElement, Type type,
                                   JsonDeserializationContext jsonDeserializationContext) {
        JsonObject object = jsonElement.getAsJsonObject();
        JsonElement eventTypeElement = object.get("eventType");
        String eventTypeString = eventTypeElement.getAsString();

        if (Strings.isEmpty(eventTypeString)) {
            Ln.w(false, "No eventType in Mercury payload.");
            return null;
        }

        MercuryData data = null;
        MercuryEventType eventType = MercuryEventType.fromString(eventTypeString);
        if (eventType != null) {
            Lns.mercury().i("Mercury->EventBus: event type: %s", eventType.name());
            switch (eventType) {
                case BOARD_ACTIVITY:
                    data = jsonDeserializationContext.<WhiteboardActivityEvent>deserialize(jsonElement, WhiteboardActivityEvent.class);
                    break;

                case CONVERSATION_ACTIVITY:
                    data = jsonDeserializationContext.<ConversationActivityEvent>deserialize(jsonElement, ConversationActivityEvent.class);
                    if (data != null && ((ConversationActivityEvent) data).getActivity() != null)
                        ((ConversationActivityEvent) data).getActivity().setSource(ConversationContract.ActivityEntry.Source.MERCURY);
                    break;
                case KEY_PUSH:
                    jsonElement = JsonUtils.extractJsonObjectFromString(jsonElement);
                    data = jsonDeserializationContext.<KeyPushEvent>deserialize(jsonElement, KeyPushEvent.class);
                    break;
                case KMS_ACK:
                    data = jsonDeserializationContext.<KmsAckEvent>deserialize(jsonElement, KmsAckEvent.class);
                    break;
                case KMS_MESSAGE:
                    data = jsonDeserializationContext.<KmsMessageResponseEvent>deserialize(jsonElement, KmsMessageResponseEvent.class);
                    break;
                case CONVERSATION_BADGE_COUNT:
                    break;

                case LOCUS_CONTROLS_UPDATED:
                case LOCUS_UPDATED:
                case LOCUS_PARTICIPANT_ALERTED:
                case LOCUS_PARTICIPANT_JOINED:
                case LOCUS_PARTICIPANT_LEFT:
                case LOCUS_PARTICIPANT_DECLINED:
                case LOCUS_PARTICIPANT_AUDIO_MUTED:
                case LOCUS_PARTICIPANT_AUDIO_UNMUTED:
                case LOCUS_PARTICIPANT_VIDEO_MUTED:
                case LOCUS_PARTICIPANT_VIDEO_UNMUTED:
                case LOCUS_SELF_CHANGED:
                case LOCUS_PARTICIPANT_CONTROLS_UPDATED:
                case LOCUS_PARTICIPANT_UPDATED:
                case LOCUS_FLOOR_GRANTED:
                case LOCUS_FLOOR_RELEASED:
                    data = jsonDeserializationContext.<LocusChangedEvent>deserialize(jsonElement, LocusChangedEvent.class);
                    break;
                case LOCUS_DIFFERENCE:
                    data = jsonDeserializationContext.<LocusDeltaEvent>deserialize(jsonElement, LocusDeltaEvent.class);
                    break;
                case START_TYPING:
                case STOP_TYPING_EVENT:
                    data = jsonDeserializationContext.<TypingEvent>deserialize(jsonElement, TypingEvent.class);
                    break;
                case ROOM_UPDATED_EVENT:
                    data = jsonDeserializationContext.<RoomUpdatedEvent>deserialize(jsonElement, RoomUpdatedEvent.class);
                    break;
                // TODO: stistryn: verify if this event is sent to the clients
                case ROOM_DEVICE_ENTERED_EVENT:
                    data = jsonDeserializationContext.<RoomDeviceEnteredEvent>deserialize(jsonElement, RoomDeviceEnteredEvent.class);
                    break;
                case ROOM_DEVICE_EXITED_EVENT:
                    data = jsonDeserializationContext.<RoomDeviceExitedEvent>deserialize(jsonElement, RoomDeviceExitedEvent.class);
                    break;
                case LYRA_SPACE_AUDIO_VOLUME_CHANGE:
                    data = jsonDeserializationContext.<LyraSpaceAudioVolumeChangeEvent>deserialize(jsonElement, LyraSpaceAudioVolumeChangeEvent.class);
                    break;
                case LYRA_SPACE_AUDIO_VOLUME_SET:
                    data = jsonDeserializationContext.<LyraSpaceAudioVolumeSetEvent>deserialize(jsonElement, LyraSpaceAudioVolumeSetEvent.class);
                    break;
                case LYRA_SPACE_AUDIO_MICROPHONES_MUTE:
                    data = jsonDeserializationContext.<LyraSpaceAudioMicrophonesMuteActionEvent>deserialize(jsonElement, LyraSpaceAudioMicrophonesMuteActionEvent.class);
                    break;
                case JANUS_USER_SESSIONS:
                    data = jsonDeserializationContext.<UserRecentSessionsEvent>deserialize(jsonElement, UserRecentSessionsEvent.class);
                    break;
                case PRESENCE_SUBSCRIPTION_UPDATE:
                    data = jsonDeserializationContext.<PresenceStatusResponse>deserialize(jsonElement, PresenceStatusResponse.class);
                    break;
                case USER_APP_ITEM:
                    data = jsonDeserializationContext.<UserFeatureUpdate>deserialize(jsonElement, UserFeatureUpdate.class);
                    break;
                case LYRA_SPACE_UPDATE_EVENT:
                    data = jsonDeserializationContext.<LyraActivityEvent>deserialize(jsonElement, LyraActivityEvent.class);
                    break;
                case ROOM_REQUEST_LOGS:
                    data = jsonDeserializationContext.<RoomRequestLogsEvent>deserialize(jsonElement, RoomRequestLogsEvent.class);
                    break;
                case ROOM_SET_UPGRADE_CHANNEL:
                    data = jsonDeserializationContext.<RoomSetUpgradeChannelEvent>deserialize(jsonElement, RoomSetUpgradeChannelEvent.class);
                    break;
                case ROOM_RENEW_RSU:
                    data = jsonDeserializationContext.<RoomRenewRsuEvent>deserialize(jsonElement, RoomRenewRsuEvent.class);
                    break;
                case ROOM_CONTROL:
                    data = jsonDeserializationContext.<RoomControlEvent>deserialize(jsonElement, RoomControlEvent.class);
                    break;
                case ROOM_SET_ROOM_CONTROL_USER:
                    data = jsonDeserializationContext.<RoomSetRoomControlUserEvent>deserialize(jsonElement, RoomSetRoomControlUserEvent.class);
                    break;
                case ROOM_IDENTITY_CHANGED:
                    data = jsonDeserializationContext.<RoomIdentityChangedEvent>deserialize(jsonElement, RoomIdentityChangedEvent.class);
                    break;
                case ROOM_IDENTITY_DELETED:
                    data = jsonDeserializationContext.<RoomIdentityDeletedEvent>deserialize(jsonElement, RoomIdentityDeletedEvent.class);
                    break;
                case MERCURY_REGISTRATION_STATUS:
                    data = jsonDeserializationContext.<MercuryRegistration>deserialize(jsonElement, MercuryRegistration.class);
                    break;
                default:
                    Ln.w(false, "Unknown Mercury Event Type: %s", eventTypeString);
                    break;
            }
        }

        return data;
    }
}
