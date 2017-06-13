package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;

import java.util.List;

public class RegistrationRequestResource {

    private static final int REPLICATED = 0;
    private static final int BALANCED = 1;

    private static final String REPLACED = "REPLACED";

    private List<String> bindings;
    private Integer deliveryStrategy = BALANCED;
    private Integer deviceType; // Refer WDM registration for supported devices
    private Integer messageTtl;
    private Uri mercuryConnectionServiceClusterUrl;
    private Uri webSocketUrl;

    public boolean isSharedWebSocket() {
        return sharedWebSocket;
    }

    private boolean sharedWebSocket;
    final private String action;

    public RegistrationRequestResource(List<String> bindings) {
        this.bindings = bindings;
        this.action = REPLACED;
    }

    public List<String> getBindings() {
        return bindings;
    }
}
