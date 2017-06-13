package com.cisco.spark.android.sync;

import android.annotation.SuppressLint;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.text.TextUtils;

import com.cisco.spark.android.sync.ConversationContract.ParticipantEntry;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Loader to deliver up-to-date participant info
 */
public class ParticipantReadReceiptLoader extends AsyncTaskLoader<ArrayList<ParticipantReadReceiptLoader.ConversationParticipant>> {

    private String conversationId;
    private long lastModifiedTime;

    ParticipantContentObserver observer;

    public ParticipantReadReceiptLoader(Context context, String conversationId) {
        super(context);
        this.conversationId = conversationId;
        observer = new ParticipantContentObserver(new Handler());
    }

    @Override
    public void onStartLoading() {
        super.onStartLoading();

        getContext().getContentResolver().registerContentObserver(ParticipantEntry.CONTENT_URI, true, observer);
        forceLoad();
    }

    @Override
    public void onStopLoading() {
        super.onStopLoading();
        getContext().getContentResolver().unregisterContentObserver(observer);
    }

    @Override
    protected void onReset() {
        super.onReset();
        lastModifiedTime = 0;
    }

    public static class ConversationParticipant extends HashMap<ConversationContract.ParticipantEntry, String> implements Comparable<ConversationParticipant> {
        ConversationParticipant(Cursor c) {
            for (ParticipantEntry col : ParticipantEntry.values()) {
                put(col, c.getString(col.ordinal()));
            }
        }

        @Override
        public boolean equals(Object rhsObj) {
            // If we're referring to the same actor+conv key consider them equal for purposes of the Set
            if (rhsObj == null || !(rhsObj instanceof ConversationParticipant))
                return false;

            ConversationParticipant rhs = (ConversationParticipant) rhsObj;

            if (!getActorKey().equals(rhs.getActorKey()))
                return false;

            if (!TextUtils.equals(getConversationId(), rhs.getConversationId()))
                return false;

            return true;
        }

        public String getConversationId() {
            return get(ParticipantEntry.CONVERSATION_ID);
        }

        public long getLastActiveTime() {
            return Long.valueOf(get(ParticipantEntry.LAST_ACTIVE_TIME));
        }

        public String getLastAckedActivityId() {
            return get(ParticipantEntry.LASTACK_ACTIVITY_ID);
        }

        public ActorRecord.ActorKey getActorKey() {
            return new ActorRecord.ActorKey(get(ParticipantEntry.ACTOR_UUID));
        }

        public boolean isModerator() {
            int value = Integer.valueOf(get(ParticipantEntry.IS_MODERATOR));
            return value != 0;
        }

        @Override
        public int compareTo(ConversationParticipant rhs) {
            if (rhs == null)
                return 1;

            if (getLastActiveTime() == rhs.getLastActiveTime())
                return 0;

            return getLastActiveTime() < rhs.getLastActiveTime() ? 1 : -1;
        }
    }

    @Override
    @SuppressLint("Recycle") // TODO: Remove, Lint is giving a false positive.
    public ArrayList<ConversationParticipant> loadInBackground() {

        Ln.d("Loading participants. Conversation=" + conversationId);

        synchronized (this) {
            ArrayList<ConversationParticipant> ret = new ArrayList<ConversationParticipant>();

            Cursor c = null;
            try {
                if (conversationId != null) {
                    c = getContext().getContentResolver().query(ParticipantEntry.CONTENT_URI,
                            ParticipantEntry.DEFAULT_PROJECTION,
                            ParticipantEntry.TIME_MODIFIED + "> ? AND "
                                    + ParticipantEntry.CONVERSATION_ID + "=? AND "
                                    + ParticipantEntry.MEMBERSHIP_STATE + "=?",
                            new String[]{String.valueOf(lastModifiedTime), conversationId,
                                    String.valueOf(ParticipantEntry.MembershipState.ACTIVE.ordinal())},
                            null
                    );
                } else {
                    c = getContext().getContentResolver().query(ParticipantEntry.CONTENT_URI,
                            ParticipantEntry.DEFAULT_PROJECTION,
                            ParticipantEntry.TIME_MODIFIED + ">= ? AND "
                                    + ParticipantEntry.MEMBERSHIP_STATE + "=?",
                            new String[]{String.valueOf(lastModifiedTime),
                                    String.valueOf(ParticipantEntry.MembershipState.ACTIVE.ordinal())},
                            ParticipantEntry.CONVERSATION_ID + " ASC"
                    );
                }

                while (c != null && c.moveToNext()) {
                    ConversationParticipant participant = new ConversationParticipant(c);
                    ret.add(participant);

                    lastModifiedTime = Math.max(lastModifiedTime, c.getLong(ParticipantEntry.TIME_MODIFIED.ordinal()));
                }

                return ret;
            } catch (SQLiteDatabaseLockedException ex) {
                Ln.e(true, "Failed to acquire DB Lock", ex);
                return ret;
            } finally {
                if (c != null)
                    c.close();
            }
        }
    }

    protected class ParticipantContentObserver extends ContentObserver {

        public ParticipantContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean self) {
            onChange(self, null);
        }

        @Override
        public void onChange(boolean self, Uri uri) {
            onContentChanged();
        }
    }

    abstract public static class ParticipantReadReceiptLoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<ParticipantReadReceiptLoader.ConversationParticipant>> {
    }
}
