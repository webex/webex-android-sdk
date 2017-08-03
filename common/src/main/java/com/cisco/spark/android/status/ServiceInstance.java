package com.cisco.spark.android.status;

public class ServiceInstance {
    private String instanceId;
    private String host;
    private String port;

    public ServiceInstance(String instanceId, String host, String port) {
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }
}
