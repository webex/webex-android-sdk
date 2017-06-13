package com.cisco.spark.android.wdm;

import com.cisco.spark.android.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16, manifest = Config.NONE)
public class FeaturesTest {

    private static final FeatureToggle PMR_TOGGLE_ON = new FeatureToggle(Features.NATIVE_CLIENT_LOBBY, "true", false);
    private static final FeatureToggle PMR_TOGGLE_OFF = new FeatureToggle(Features.NATIVE_CLIENT_LOBBY, "true", false);

    @Test
    public void testToggleOverrides() {

        Features features = new Features();

        assertFalse("Toggle not set", features.isNativeClientLobbyEnabled());
        features.clear();
        assertFalse("Toggle not set, then cleared", features.isNativeClientLobbyEnabled());

        features.setDeveloperFeature(PMR_TOGGLE_ON);
        assertTrue("Toggle explicitly set", features.isNativeClientLobbyEnabled());
        features.clear();
        assertFalse("Toggle set, then cleared", features.isNativeClientLobbyEnabled());

        Map<String, FeatureToggle> toggles = new HashMap<>();
        toggles.put(PMR_TOGGLE_ON.getKey(), PMR_TOGGLE_ON);

        Features.setDeviceOverrides(toggles);
        assertTrue("Toggle set by device override", features.isNativeClientLobbyEnabled());
        features.clear();
        assertTrue("Toggle set by device override, should have ignored clear", features.isNativeClientLobbyEnabled());
        features.setDeveloperFeature(PMR_TOGGLE_OFF);
        assertTrue("Toggle set by device override, should have ignored set to false", features.isNativeClientLobbyEnabled());

        features = new Features();
        assertTrue("Toggle set by device override, should persist new object creation", features.isNativeClientLobbyEnabled());

        features.setDeveloperFeature(PMR_TOGGLE_ON);
        Features.setDeviceOverrides(null);
        assertTrue("Toggle override unset, but set manully", features.isNativeClientLobbyEnabled());

        features = new Features();
        assertFalse("Toggle override unset, should be cleared by new object creation", features.isNativeClientLobbyEnabled());
    }
}
