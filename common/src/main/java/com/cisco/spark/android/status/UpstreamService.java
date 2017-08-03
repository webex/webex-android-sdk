package com.cisco.spark.android.status;

import java.util.List;

public class UpstreamService {

    private String serviceName;
    private String serviceType;
    private String serviceState;
    private String message;
    private String lastUpdated;
    private List<UpstreamService> upstreamServices;
    private String baseUrl;
    private String defaultCharset;

    public UpstreamService(String serviceName, String serviceType, String serviceState, String message, String lastUpdated,
                           List<UpstreamService>  upstreamServices, String baseUrl, String defaultCharset) {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.serviceState = serviceState;
        this.message = message;
        this.lastUpdated = lastUpdated;
        this.upstreamServices = upstreamServices;
        this.baseUrl = baseUrl;
        this.defaultCharset = defaultCharset;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceState() {
        return serviceState;
    }

    public String getMessage() {
        return message;
    }

    public List<UpstreamService> getUpstreamServices() {
        return upstreamServices;
    }

    public String getDefaultCharset() {
        return defaultCharset;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
