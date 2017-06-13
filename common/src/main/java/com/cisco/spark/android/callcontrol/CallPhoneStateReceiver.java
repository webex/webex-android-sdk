package com.cisco.spark.android.callcontrol;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.cisco.spark.android.callcontrol.events.CallControlPhoneStateChangedEvent;
import com.cisco.spark.android.core.SquaredBroadcastReceiver;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class CallPhoneStateReceiver extends SquaredBroadcastReceiver {
    private static String lastState;

    @Inject
    EventBus bus;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!isInitialized()) {
            return;
        }

        if (intent != null && intent.getExtras() != null) {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);

            if (!stateStr.equals(lastState)) {
                lastState = stateStr;
                Ln.d("CallPhoneStateReceiver.onReceive() state = " + stateStr);

                int state = 0;
                if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    state = TelephonyManager.CALL_STATE_IDLE;
                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    state = TelephonyManager.CALL_STATE_RINGING;
                }

                bus.post(new CallControlPhoneStateChangedEvent(state));
            }
        }
    }
}
