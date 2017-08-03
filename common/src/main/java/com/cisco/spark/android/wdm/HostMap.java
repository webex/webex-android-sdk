package com.cisco.spark.android.wdm;


import android.net.Uri;

import java.util.List;
import java.util.Map;

public class HostMap {
    private Map<String, Uri> serviceLinks;
    private Map<String, List<ServiceHost>> hostCatalog;

    public HostMap() {
    }

    public Uri getServiceLink(String service) {
        return serviceLinks.get(service);
    }

    Map<String, List<ServiceHost>> getHostCatalog() {
        return hostCatalog;
    }

    public List<ServiceHost> getServiceHost(String hostname) {
        return hostCatalog.get(hostname);
    }
}
