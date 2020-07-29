package com.ciscowebex.androidsdk.internal.model;

import me.helloworld.utils.Checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicesClusterModel {

    private class ServiceClusterModel {
        private String id;
        private String serviceName;
        private List<Map<String, Object>> serviceUrls = new ArrayList();
    }

    private List<ServiceClusterModel> services = new ArrayList<>();

    public Map<String, String> getClusterUrls() {
        Map<String, String> ret = new HashMap<>();
        for (ServiceClusterModel model : services) {
            if (!Checker.isEmpty(model.id) && !Checker.isEmpty(model.serviceUrls)) {
                ret.put(model.id, (String) model.serviceUrls.get(0).get("baseUrl"));
            }
        }
        return ret;
    }

}
