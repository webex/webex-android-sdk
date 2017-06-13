package com.cisco.spark.android.util;


import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.calliope.CalliopeClusterResponse;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.events.StunTraceServerResultEvent;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.FeatureToggle;
import com.cisco.spark.android.wdm.Features;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.webex.wme.TraceServerSink;
import com.webex.wme.WmeStunTraceResult;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class LinusReachabilityService implements Component, TraceServerSink {
    private final DeviceRegistration deviceRegistration;
    private final EventBus bus;
    private final ApiClientProvider apiClientProvider;
    private final Gson gson;
    private final NaturalLog ln;
    private final MediaEngine mediaEngine;
    private boolean initialized;
    private String ipAddress;
    private long maxAge;

    private Map<String, Object> reachabilityResults;
    private String clusterInfo;

    private Date lastReachabilityCheckTime;

    public LinusReachabilityService(DeviceRegistration deviceRegistration,
                                    ApiClientProvider apiClientProvider,
                                    EventBus bus,
                                    Gson gson,
                                    Ln.Context lnContext,
                                    MediaEngine mediaEngine) {
        this.deviceRegistration = deviceRegistration;
        this.bus = bus;
        this.apiClientProvider = apiClientProvider;
        this.gson = gson;
        this.ln = Ln.get(lnContext, "LinusReachabilityService");
        this.mediaEngine = mediaEngine;
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }

    @Override
    public boolean shouldStart() {
        Ln.i("ReachabilityCheck:LinusReachabilityService shouldStart " + isCalliopeDiscoveryFeatureToggleEnabled());
        return (isCalliopeDiscoveryFeatureToggleEnabled());
    }

    @Override
    public void start() {
        ln.i("ReachabilityCheck:LinusReachabilityService - start()");
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }
        initialize();
        performReachabilityCheck();

    }

    private void performReachabilityCheck() {
        String currentIpAddress = NetworkUtils.getLocalIpAddress();
        ln.d("ReachabilityCheck:IP Address" + ipAddress + " , current IP Address " + currentIpAddress);
        if (ipAddress != null && ipAddress.equals(currentIpAddress) && !hasRechabilityCheckMaxTimeElapsed()) {
            ln.i("ReachabilityCheck:Network has not changed and max time for check not elapsed..skipping reachability checks");
            return;
        }

        ipAddress = currentIpAddress;

        performStunTraceCheck(getClusterInfo());
    }

    private String getClusterInfo() {
        if (clusterInfo != null && !hasRechabilityCheckMaxTimeElapsed()) {
            Ln.d("ReachabilityCheck:getClusterInfo:returning cached clusterInfo" + clusterInfo);
            return clusterInfo;
        }
        CalliopeClusterResponse calliopeClusterResponse = getCalliopeClusterDetails();
        Ln.d("ReachabilityCheck:getClusterInfo:calliopeClusterResponse" + calliopeClusterResponse);

        if (calliopeClusterResponse != null) {
            clusterInfo = gson.toJson(calliopeClusterResponse.getClusterInfo());
            Ln.i("ReachabilityCheck:getClusterInfo:clusterInfo" + clusterInfo);
        }
        Ln.d("ReachabilityCheck:getClusterInfo:clusterInfo" + clusterInfo);
        return clusterInfo;
    }

    private CalliopeClusterResponse getCalliopeClusterDetails() {

        //Clear current reachability data stored to ensure there is no stale information
        clearLinusReachabilityData();

        lastReachabilityCheckTime = new Date();
        CalliopeClusterResponse clusters = apiClientProvider.getCalliopeClient().getClusters();
        Ln.d("getCalliopeClusterDetails - string" + gson.toJson(clusters));

        maxAge = MILLISECONDS.convert(7200, SECONDS);
        Ln.i("getCalliopeClusterDetails - maxAge" + maxAge);

        return clusters;
    }

    public void performStunTraceCheck(String clusters) {
        if (clusters != null && clusters.length() > 0) {
            mediaEngine.startTraceServer(clusters);
        }

    }

    @Override
    public void stop() {
        Ln.d("LinusReachabilityService:stop");
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }
    }

    private void clearLinusReachabilityData() {
        this.reachabilityResults = null;
        this.clusterInfo = null;
    }

    private void initialize() {
        ln.d("LinusReachabilityService:initialize(), initialized = " + initialized);

        if (initialized) {
            return;
        }

        mediaEngine.setTraceServerSink(this);
        initialized = true;

    }

    @Override
    public void OnTraceServerResult(WmeStunTraceResult wmeStunTraceResult, String s) {
        ln.d("LinusReachabilityService:TraceServer result, detail = " + s);
        try {
            if (s != null) {
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> map = gson.fromJson(s, type);
                Ln.d("LinusReachabilityService:TraceServer result, map = " + map);
                reachabilityResults = map;
                bus.post(new StunTraceServerResultEvent(s));
            }
        } catch (Exception e) {
            Ln.e("Exception in OnTraceServerResult:" + e);
        }

    }

    public void uninitialize() {
        Ln.d("LinusReachabilityService:uninitialize(), initialized = " + initialized);
        initialized = false;
        clearLinusReachabilityData();
        lastReachabilityCheckTime = null;
        ipAddress = null;
        maxAge = 0L;
    }

    public Map<String, Object> getLatestLinusReachabilityResults() {
        return this.reachabilityResults;
    }

    private boolean hasRechabilityCheckMaxTimeElapsed() {
        try {
            if (lastReachabilityCheckTime == null || maxAge == 0L) {
                return true;
            }
            Date now = new Date();
            long duration = now.getTime() - lastReachabilityCheckTime.getTime();

            if (duration >= maxAge) {
                return true;
            }

        } catch (Exception e) {
            Ln.d("Exception in hasRechabilityCheckMaxTimeElapsed " + e);
        }
        return false;
    }

    private boolean isCalliopeDiscoveryFeatureToggleEnabled() {
        boolean isEnabled = false;
        FeatureToggle value = deviceRegistration.getFeatures().getFeature(Features.FeatureType.DEVELOPER, Features.CALLIOPE_DISCOVERY_FEATURE);
        if (value != null) {
            isEnabled = value.getBooleanVal();
        }
        return isEnabled;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LogoutEvent event) {
        Ln.d("LinusReachabilityService:logout Event");
        uninitialize();
    }

}

