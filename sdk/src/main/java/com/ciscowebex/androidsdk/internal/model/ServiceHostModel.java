package com.ciscowebex.androidsdk.internal.model;

import java.util.HashMap;
import java.util.Map;

public class ServiceHostModel {

    private Map<String, String> serviceLinks = new HashMap<>();

    public String getServiceUrl(String key) {
        return serviceLinks.get(key);
    }

}
