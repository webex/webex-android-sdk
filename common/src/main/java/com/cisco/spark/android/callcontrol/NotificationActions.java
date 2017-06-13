package com.cisco.spark.android.callcontrol;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.util.Action;

public class NotificationActions {
    private Action<LocusKey> answerAction;
    private Action<LocusKey> pairedAnswerAction;
    private Action<LocusKey> declineAction;
    private Action<LocusKey> timeoutAction;

    public NotificationActions() {
    }

    public NotificationActions(Action<LocusKey> answerAction, Action<LocusKey> declineAction) {
        this.answerAction = answerAction;
        this.declineAction = declineAction;
    }

    public NotificationActions(Action<LocusKey> answerAction, Action<LocusKey> declineAction, Action<LocusKey> pairedAnswerAction, Action<LocusKey> timeoutAction) {
        this.answerAction = answerAction;
        this.declineAction = declineAction;
        this.pairedAnswerAction = pairedAnswerAction;
        this.timeoutAction = timeoutAction;
    }

    public Action<LocusKey> getAnswerAction() {
        return answerAction;
    }

    public Action<LocusKey> getDeclineAction() {
        return declineAction;
    }

    public Action<LocusKey> getPairedAnswerAction() {
        return pairedAnswerAction;
    }

    public Action<LocusKey> getTimeoutAction() {
        return timeoutAction;
    }
}
