package com.ciscowebex.androidsdk.internal.model;

public class ReachabilityModel {

    public static class ReachabilityTransportStatusModel {
        public boolean reachable;
        public long latencyInMilliseconds;
    }

    public ReachabilityTransportStatusModel udp;
    public ReachabilityTransportStatusModel tcp;
    public ReachabilityTransportStatusModel https;
    public ReachabilityTransportStatusModel xtls;
}
