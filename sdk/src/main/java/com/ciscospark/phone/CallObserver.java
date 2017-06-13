package com.ciscospark.phone;

/**
 * Created on 12/06/2017.
 */

public interface CallObserver {
    void onRinging(Call call);
    void onConnected(Call call);
    void onDisconnected(Call call);
    void onMediaChanged(Call call);
}
