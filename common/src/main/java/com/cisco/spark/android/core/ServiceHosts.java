package com.cisco.spark.android.core;

import com.cisco.spark.android.wdm.ServiceHost;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ServiceHosts {
    private class ServiceHostInfo {
        private String host;
        private Integer priority;
        private boolean failed = false;

        ServiceHostInfo(String host, Integer priority) {
            this.host = host;
            this.priority = priority;
        }

        String getHost() {
            return host;
        }

        Integer getPriority() {
            return priority;
        }

        boolean getFailed() {
            return failed;
        }

        void setFailed(boolean failed) {
            this.failed = failed;
        }
    }

    final private List<ServiceHostInfo> serviceHostInfos;
    final private List<String> activeHosts;

    public ServiceHosts(List<ServiceHost> hosts) {

        serviceHostInfos = new ArrayList<>();

        for (ServiceHost host : hosts) {
            serviceHostInfos.add(new ServiceHostInfo(host.getHost(), host.getPriority()));
        }

        Collections.sort(serviceHostInfos, (s1, s2) -> s1.getPriority() - s2.getPriority());

        activeHosts = new ArrayList<>();
        getHighestPriorityHosts(activeHosts);
    }

    public String getHost() {

        synchronized (activeHosts) {

            if (activeHosts.isEmpty()) {
                return null;
            }

            return activeHosts.get(new Random().nextInt(activeHosts.size()));
        }
    }

    public void markHostFailed(String host) {
        if (host == null || host.isEmpty()) {
            return;
        }

        synchronized (serviceHostInfos) {
            int availableUrls = 0;

            for (ServiceHostInfo urlInfo : serviceHostInfos) {
                if (urlInfo.getHost().equals(host)) {
                    Ln.i("ServiceHosts: marking %s as failed", host);
                    urlInfo.setFailed(true);
                }

                if (!urlInfo.getFailed()) {
                    availableUrls++;
                }
            }

            if (availableUrls == 0) {
                // all Urls are marked failed, clear the failures and start at the beginning.
                for (ServiceHostInfo urlInfo : serviceHostInfos) {
                    urlInfo.setFailed(false);
                }
            }

            synchronized (activeHosts) {
                getHighestPriorityHosts(activeHosts);
            }
        }
    }

    private void getHighestPriorityHosts(List<String> hosts) {
        int priority = -1;
        hosts.clear();

        for (ServiceHostInfo urlInfo : serviceHostInfos) {
            if (!urlInfo.getFailed()) {
                if (hosts.isEmpty()) {
                    priority = urlInfo.getPriority();
                }

                if (priority != urlInfo.getPriority()) {
                    break;
                }

                hosts.add(urlInfo.getHost());
            }
        }
    }
}
