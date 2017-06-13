package com.cisco.spark.android.sync;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import com.cisco.spark.android.model.ConversationParticipant;

import java.util.ArrayList;

import static com.cisco.spark.android.sync.ConversationContract.vw_Participant;

public class ParticipantLoader extends AsyncTaskLoader<ArrayList<ConversationParticipant>> {
    private String conversationId;

    ParticipantContentObserver observer;

    public ParticipantLoader(Context context, String conversationId) {
        super(context);
        this.conversationId = conversationId;
        observer = new ParticipantContentObserver(new Handler());
    }

    @Override
    public void onStartLoading() {
        super.onStartLoading();

        getContext().getContentResolver().registerContentObserver(vw_Participant.CONTENT_URI, true, observer);
        forceLoad();
    }

    @Override
    public void onStopLoading() {
        super.onStopLoading();
        getContext().getContentResolver().unregisterContentObserver(observer);
    }

    @Override
    public ArrayList<ConversationParticipant> loadInBackground() {
        synchronized (this) {
            ArrayList<ConversationParticipant> ret = new ArrayList<ConversationParticipant>();

            Cursor c = null;
            try {
                c = getContext().getContentResolver().query(vw_Participant.CONTENT_URI,
                        vw_Participant.DEFAULT_PROJECTION,
                        vw_Participant.CONVERSATION_ID + "=?",
                        new String[]{conversationId},
                        vw_Participant.DISPLAY_NAME.name() + " ASC");

                while (c != null && c.moveToNext()) {
                    ConversationParticipant participant = new ConversationParticipant(c);
                    ret.add(participant);
                }

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

    abstract public static class ParticipantLoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<ConversationParticipant>> {
    }
}
