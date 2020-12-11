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

package com.ciscowebex.androidsdk.internal.mercury;

public abstract class MercuryEvent {

    public enum Type {
        CONVERSATION_ACTIVITY("conversation.activity"),
        KEY_PUSH("encryption.client_encrypt_keys"),
        KMS_ACK("encryption.kms_ack"),
        KMS_MESSAGE("encryption.kms_message"),
        CONVERSATION_BADGE_COUNT("conversation.badge_count"),
        LOCUS_PARTICIPANT_JOINED("locus.participant_joined"),
        LOCUS_PARTICIPANT_LEFT("locus.participant_left"),
        LOCUS_PARTICIPANT_DECLINED("locus.participant_declined"),
        LOCUS_PARTICIPANT_ALERTED("locus.participant_alerted"),
        LOCUS_PARTICIPANT_AUDIO_MUTED("locus.participant_audio_muted"),
        LOCUS_PARTICIPANT_AUDIO_UNMUTED("locus.participant_audio_unmuted"),
        LOCUS_PARTICIPANT_VIDEO_MUTED("locus.participant_video_muted"),
        LOCUS_PARTICIPANT_VIDEO_UNMUTED("locus.participant_video_unmuted"),
        LOCUS_PARTICIPANT_UPDATED("locus.participant_updated"),
        LOCUS_SELF_CHANGED("locus.self_changed"),
        LOCUS_FLOOR_GRANTED("locus.floor_granted"),
        LOCUS_FLOOR_RELEASED("locus.floor_released"),
        LOCUS_PARTICIPANT_BROADCAST("locus.participant_broadcast"),
        LOCUS_PARTICIPANT_CONTROLS_UPDATED("locus.participant_controls_updated"),
        LOCUS_UPDATED("locus.updated"),
        LOCUS_CONTROLS_UPDATED("locus.controls_updated"),
        LOCUS_DIFFERENCE("locus.difference"),
        LOCUS_ROAP_MESSAGE("locus.message.roap"),
        LOCUS_PARTICIPANT_ROLES_UPDATED("locus.participant_roles_updated"),
        LYRA_COMMAND("lyra.command_invoke"),
        PONG("pong"),
        START_TYPING("status.start_typing"),
        STOP_TYPING("status.stop_typing"),
        BOARD_ACTIVITY("board.activity"),
        ROOM_UPDATED_EVENT("room.room_updated"),
        ROOM_DEVICE_ENTERED_EVENT("room.device_entered"),
        LYRA_SPACE_AUDIO_VOLUME_CHANGE("lyra.space_audio_volume_change_action"),
        LYRA_SPACE_AUDIO_VOLUME_SET("lyra.space_audio_volume_set_action"),
        LYRA_SPACE_AUDIO_MICROPHONES_MUTE("lyra.space_audio_microphones_mute_action"),
        ROOM_DEVICE_EXITED_EVENT("room.device_exited"),
        ROOM_CONTROL("room.control"),
        ROOM_BRING_ROOM_INTO_CALL("room.bring_room_into_call"),
        ROOM_REQUEST_LOGS("room.request_logs"),
        ROOM_SET_UPGRADE_CHANNEL("room.setUpgradeChannel"),
        ROOM_RENEW_RSU("room.renewRSU"),
        ROOM_SET_ROOM_CONTROL_USER("room.set_room_control_user"),
        ROOM_IDENTITY_CHANGED("room.identityDataChanged"),
        ROOM_IDENTITY_DELETED("room.identityDeleted"),
        ROOM_PSTN_CHANGED("room.pstnChanged"),
        JANUS_USER_SESSIONS("janus.user_sessions"),
        PRESENCE_SUBSCRIPTION_UPDATE("apheleia.subscription_update"),
        USER_APP_ITEM("user.app_item"),
        LYRA_SPACE_UPDATE_EVENT("lyra.space_updated"),
        MERCURY_REGISTRATION_STATUS("mercury.registration_status"),
        VOICEMAIL_INFO("voicemail.info"),
        CALENDAR_MEETING_CREATE("calendar.meeting.create"),
        CALENDAR_MEETING_CREATE_MINIMAL("calendar.meeting.create.minimal"),
        CALENDAR_MEETING_UPDATE("calendar.meeting.update"),
        CALENDAR_MEETING_UPDATE_MINIMAL("calendar.meeting.update.minimal"),
        CALENDAR_MEETING_DELETE("calendar.meeting.delete"),
        FEATURE_TOGGLE_UPDATE("featureToggle_update");

        private final String phrase;

        Type(String phrase) {
            this.phrase = phrase;
        }

        public static Type fromString(String type) {
            if (type != null) {
                for (Type eventType : Type.values()) {
                    if (type.equalsIgnoreCase(eventType.phrase)) {
                        return eventType;
                    }
                }
            }
            return null;
        }

        public String phrase() {
            return this.phrase;
        }

        @Override
        public String toString() {
            return phrase;
        }
    }

    private Type eventType;

    public Type getEventType() {
        return eventType;
    }


}
