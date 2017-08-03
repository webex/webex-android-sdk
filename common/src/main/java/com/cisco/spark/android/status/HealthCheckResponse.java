package com.cisco.spark.android.status;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HealthCheckResponse {

    public enum ServiceState {
        @SerializedName("online")
        ONLINE,
        @SerializedName("fault")
        FAULT,
        @SerializedName("unreachable")
        UNREACHABLE,
    }

    private String serviceName;
    private String serviceType;
    private ServiceInstance serviceInstance;
    private ServiceState serviceState;
    private String message;
    private String lastUpdated;
    private List<UpstreamService> upstreamServices;
    private String defaultCharset;

    public HealthCheckResponse(String serviceName, String serviceType, ServiceInstance serviceInstance,
                               ServiceState serviceState, String message, String lastUpdated,
                               List<UpstreamService> upstreamServices, String defaultCharset) {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.serviceInstance = serviceInstance;
        this.serviceState = serviceState;
        this.message = message;
        this.lastUpdated = lastUpdated;
        this.upstreamServices = upstreamServices;
        this.defaultCharset = defaultCharset;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public ServiceState getServiceState() {
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
}
