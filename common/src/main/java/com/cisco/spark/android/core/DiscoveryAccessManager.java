package com.cisco.spark.android.core;

import android.support.annotation.Nullable;

import com.cisco.spark.android.model.RegionInfo;
import com.github.benoitdion.ln.Ln;

import de.greenrobot.event.EventBus;

public class DiscoveryAccessManager implements AccessManager {
    private final ApiClientProvider apiClientProvider;
    private final EventBus bus;
    private boolean isAccessGranted = true;

    public DiscoveryAccessManager(ApiClientProvider apiClientProvider, EventBus bus) {
        this.apiClientProvider = apiClientProvider;
        this.bus = bus;
    }

    @Override
    @Nullable public RegionInfo getRegion() {
        RegionInfo regionInfo;
        try {
            regionInfo = apiClientProvider.getRegionClient().getRegion().execute().body();
            Ln.i("Region info: Country: %s Region: %s", regionInfo.getCountryCode(), regionInfo.getRegionCode());
        } catch (Exception e) {
            Ln.i(e);
            regionInfo = null;
        }
        return regionInfo;
    }

    @Override
    public void reset() {
        isAccessGranted = true;
    }

    @Override
    public void revokeAccess() {
        isAccessGranted = false;
        bus.post(new AccessRevokedEvent());
    }

    @Override
    public void grantAccess() {
        isAccessGranted = true;
    }

    @Override
    public boolean isAccessGranted() {
        return isAccessGranted;
    }
}
