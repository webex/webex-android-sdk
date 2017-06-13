package com.cisco.spark.android.sync;

import android.content.Context;
import android.text.TextUtils;

import com.cisco.spark.android.R;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.model.Participants;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

public class TitleBuilder {
    private final Context context;
    private final ActorRecordProvider actorRecordProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    public final static int DEFAULT_TITLE_MAX_NAMES = 3;

    @Inject
    public TitleBuilder(Context context, ActorRecordProvider actorRecordProvider, AuthenticatedUserProvider authenticatedUserProvider) {
        this.context = context;
        this.actorRecordProvider = actorRecordProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Asynchronous title builder, use only if the synchronous one will not do
     */
    public boolean build(String title, final Participants participants, final Action<String> action) {
        if (action == null) {
            return true;
        }

        if (!TextUtils.isEmpty(title)) {
            action.call(title);
            return true;
        }

        if (participants == null || participants.get().size() == 0) {
            action.call("");
            return true;
        }

        final List<String> participantUuidList = Arrays.asList(participants.getIds());

        return actorRecordProvider.loadActors(participantUuidList, new Action<List<ActorRecord>>() {
            @Override
            public void call(List<ActorRecord> item) {
                ArrayList<String> lastThreeParticipantsNames = new ArrayList<>();
                int count = 0;
                int size = participantUuidList.size();
                for (String uuid : participantUuidList) {
                    if (uuid.equals(authenticatedUserProvider.getAuthenticatedUser().getKey().getUuid())) {
                        continue;
                    }

                    if (count == 3) {
                        lastThreeParticipantsNames.remove(2);
                        lastThreeParticipantsNames.add(String.format(context.getString(R.string.more), size - count + 1));
                        break;
                    }

                    String displayName;
                    if (actorRecordProvider.getCached(uuid) != null) {
                        displayName = actorRecordProvider.getCached(uuid).getDisplayName();
                    } else {
                        continue;
                    }
                    lastThreeParticipantsNames.add(NameUtils.getShortName(displayName));
                    count++;
                }
                if (lastThreeParticipantsNames.isEmpty())
                    action.call(context.getString(R.string.empty_space));
                else
                    action.call(Strings.join(", ", lastThreeParticipantsNames));
            }
        });
    }

    /**
     * Synchronous title builder
     */
    public String build(String title, List<ActorRecord> topParticipants, int totalParticipants) {
        return build(title, topParticipants, totalParticipants, null);
    }

    public String build(String title, List<ActorRecord> topParticipants, int totalParticipants, String creatorUUID) {
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        if (topParticipants == null) {
            return context.getString(R.string.empty_space);
        }

        //dedup
        HashSet<ActorRecord> participantSet = new HashSet<>(topParticipants);
        topParticipants = new ArrayList<>(participantSet);

        ArrayList<String> topParticipantsNames = new ArrayList<>();
        String displayName = "";
        for (ActorRecord actorRecord : topParticipants) {
            if (actorRecord.isAuthenticatedUser(authenticatedUserProvider.getAuthenticatedUser())) {
                continue;
            }

            displayName = actorRecord.getDisplayName();
            if (Strings.isEmailAddress(displayName)) {
                ActorRecord updatedActor = actorRecordProvider.get(actorRecord.getEmail());
                if (updatedActor != null)
                    displayName = updatedActor.getDisplayName();
            }

            if (topParticipantsNames.size() == DEFAULT_TITLE_MAX_NAMES - 1) {
                if (totalParticipants == DEFAULT_TITLE_MAX_NAMES + 1) {
                    topParticipantsNames.add(NameUtils.getFirstName(displayName));
                } else if (totalParticipants == 0) {
                    topParticipantsNames.add(context.getString(R.string.more_no_number));
                }
                break;
            }
            topParticipantsNames.add(NameUtils.getFirstName(displayName));
        }

        if (topParticipantsNames.isEmpty())
            return context.getString(R.string.empty_space);

        Collections.sort(topParticipantsNames);
        int remainingParticipants = totalParticipants - topParticipantsNames.size() - 1;

        //if there is only one other participant display their full name
        if (topParticipantsNames.size() == 1 && remainingParticipants <= 0)
            return NameUtils.getShortName(displayName);

        //display " + n more" at the end to catch any participants we missed
        if (remainingParticipants > 0) {
            topParticipantsNames.add(String.format(context.getString(R.string.more), totalParticipants - topParticipantsNames.size() - 1));
        }

        return Strings.join(", ", topParticipantsNames);
    }
}
