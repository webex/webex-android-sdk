package com.cisco.spark.android.callcontrol;

import javax.inject.*;

@Singleton
public class CallOptions {
    private boolean answerNextCall;
    private boolean declineNextCall;

    @Inject
    public CallOptions() {
    }

    public boolean shouldAnswerCall() {
        boolean currentAnswerNextCall =  answerNextCall;
        answerNextCall = false;
        return currentAnswerNextCall;
    }

    public void answerNextCall() {
        this.answerNextCall = true;
    }

    public boolean shouldDeclineCall() {
        boolean currentDeclineNextCall =  declineNextCall;
        declineNextCall = false;
        return currentDeclineNextCall;
    }

    public void declineNextCall() {
        this.declineNextCall = true;
    }
}
