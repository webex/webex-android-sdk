package com.cisco.spark.android.wdm;


public class ServiceHost {
    private String host;
    private int priority;

    public ServiceHost() {
    }

    public ServiceHost(String host, int priority) {
        this.host = host;
        this.priority = priority;
    }

    public void setHost(String host) {
        this.host = host;
    }
    public String getHost() {
        return host;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
