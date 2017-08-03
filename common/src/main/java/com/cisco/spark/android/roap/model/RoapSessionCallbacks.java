package com.cisco.spark.android.roap.model;

import java.util.List;

public interface RoapSessionCallbacks {

    void sendRoapMessage(final RoapBaseMessage msg);

    void receivedRoapAnswer(List<String> sdpList);
    void receivedRoapOffer(List<String> sdpList);
}
