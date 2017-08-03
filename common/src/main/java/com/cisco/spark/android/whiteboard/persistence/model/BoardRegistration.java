package com.cisco.spark.android.whiteboard.persistence.model;

// https://sqbu-github.cisco.com/WebExSquared/cloud-apps/blob/master/board/common/src/main/java/com/cisco/wx2/board/common/Registration.java

import android.net.Uri;

public class BoardRegistration {
    private String binding;
    private long messageTtl;
    private Uri mercuryConnectionServiceClusterUrl;
    private Uri webSocketUrl;
    private boolean sharedWebSocket;
    private Action action;

    public BoardRegistration(String binding,
                             Uri mercuryConnectionServiceClusterUrl,
                             Uri webSocketUrl,
                             Action action) {
        this.binding = binding;
        this.mercuryConnectionServiceClusterUrl = mercuryConnectionServiceClusterUrl;
        this.webSocketUrl = webSocketUrl;
        this.action = action;
    }

    public BoardRegistration(String binding,
                        long messageTtl,
                        Uri mercuryConnectionServiceClusterUrl,
                        Uri webSocketUrl,
                        boolean sharedWebSocket,
                        Action action) {
        this.binding = binding;
        this.messageTtl = messageTtl;
        this.mercuryConnectionServiceClusterUrl = mercuryConnectionServiceClusterUrl;
        this.webSocketUrl = webSocketUrl;
        this.sharedWebSocket = sharedWebSocket;
        this.action = action;
    }

    public String getBinding() {
        return binding;
    }

    public long getMessageTtl() {
        return messageTtl;
    }

    public Uri getMercuryConnectionServiceClusterUrl() {
        return mercuryConnectionServiceClusterUrl;
    }

    public Uri getWebSocketUrl() {
        return webSocketUrl;
    }

    public boolean getIsSharedWebSocket() {
        return sharedWebSocket;
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        ADD, REPLACE, REMOVE
    }
}
