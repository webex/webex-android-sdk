package com.cisco.spark.android.wdm;

import android.net.Uri;

import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.client.WhistlerTestClient;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class DeviceRegistration {

    private static final String MERCURY_REGISTRATION_QUERIES =
            "?mercuryRegistrationStatus=true";

    private String id;
    private Uri url;
    private Services services;
    private String deviceSettingsString;
    private Features features;
    private Uri webSocketUrl;
    private boolean showSupportText;
    private Uri reportingSiteUrl;
    private String reportingSiteDesc;
    private Uri partnerLogoUrl;
    private String partnerCompanyName;
    private Uri customerLogoUrl;
    private String customerCompanyName;
    private Uri supportProviderLogoUrl;
    private String supportProviderCompanyName;
    private String clientSecurityPolicy;
    private Date modificationTime;

    private Uri deviceStatusUrl;
    private boolean isDeviceManaged;
    private String deviceIdentifier;

    // maintain two whitelists where we will send our auth:
    // a static whitelist from BuildConfig, and
    // a second that comes from the service catalog from the server,
    // which can change
    transient private Set<String> buildConfigWhitelist = new ConcurrentSkipListSet<>();
    transient private Set<String> serviceCatalogWhitelist = new ConcurrentSkipListSet<>();

    // Synchronize writes to this object to avoid a ConcurrentModificationException in gson.toJson()
    final transient private Object synclock;

    public DeviceRegistration() {
        super();

        features = new Features();
        services = new Services();
        synclock = features.getSyncObject();
    }

    public void reset(UrlProvider urlProvider) {
        synchronized (synclock) {
            id = null;
            url = null;
            services = new Services();
            this.deviceSettingsString = null;
            this.webSocketUrl = null;
            features = new Features();
            this.showSupportText = false;
            this.reportingSiteUrl = null;
            this.reportingSiteDesc = null;
            this.partnerLogoUrl = null;
            this.partnerCompanyName = null;
            this.customerLogoUrl = null;
            this.customerCompanyName = null;
            this.supportProviderLogoUrl = null;
            this.supportProviderCompanyName = null;
            this.clientSecurityPolicy = null;
            this.modificationTime = null;
            buildConfigWhitelist.clear();
            serviceCatalogWhitelist.clear();
            initialize(urlProvider);
        }
    }

    public void initialize(UrlProvider urlProvider) {
        synchronized (synclock) {
            buildConfigWhitelist.add(Uri.parse(urlProvider.getUsersApiUrl()).getHost());
            buildConfigWhitelist.add(Uri.parse(urlProvider.getServiceApiUrl()).getHost());
            buildConfigWhitelist.add(Uri.parse(urlProvider.getCalliopeRegistrarUrl()).getHost());

            String aclServiceUrl = urlProvider.getAclServiceUrl();
            if (aclServiceUrl != null) {
                buildConfigWhitelist.add(Uri.parse(aclServiceUrl).getHost());
            }

            String metricsApiUrl = urlProvider.getMetricsApiUrl();
            if (metricsApiUrl != null) {
                buildConfigWhitelist.add(Uri.parse(metricsApiUrl).getHost());
            }

            refreshWhitelist();
        }
    }

    private void refreshWhitelist() {
        synchronized (synclock) {
            serviceCatalogWhitelist = new ConcurrentSkipListSet<>();
            whitelist(getAclServiceUrl());
            whitelist(getAvatarServiceUrl());
            whitelist(getConversationServiceUrl());
            whitelist(getLocusServiceUrl());
            whitelist(getJanusServiceUrl());
            whitelist(getMetricsServiceUrl());
            whitelist(getRoomServiceUrl());
            whitelist(getWebSocketUrl());
            whitelist(getFilesServiceUrl());
            whitelist(getSquaredFilesServiceUrl());
            whitelist(getProximityServiceUrl());
            whitelist(getEncryptionServiceUrl());
            whitelist(getAdminServiceUrl());
            whitelist(getFeatureServiceUrl());
            whitelist(getCalendarServiceUrl());
            whitelist(getStickiesServiceUrl());
            whitelist(getSearchServiceUrl());
            whitelist(getSwUpgradeServiceUrl());
            whitelist(getAdminServiceUrl());
            whitelist(getPresenceServiceUrl());
            whitelist(getCalliopeDiscoveryServiceUrl());
            whitelist(Uri.parse(WhistlerTestClient.URL));
            whitelist(getUserAppsServiceUrl());
            whitelist(getBoardServiceUrl());
            whitelist(getHecateServiceUrl());
            whitelist(getLyraServiceUrl());
        }
    }

    public void populateFrom(DeviceRegistration deviceRegistration) {
        synchronized (synclock) {
            if (this == deviceRegistration)
                return;

            if (deviceRegistration.id == null && deviceRegistration.url != null)
                this.id = deviceRegistration.url.getLastPathSegment();
            else
                this.id = deviceRegistration.id;
            this.url = deviceRegistration.url;
            this.services = deviceRegistration.services;
            this.deviceSettingsString = deviceRegistration.deviceSettingsString;
            this.webSocketUrl = deviceRegistration.getWebSocketUrl();
            this.features = deviceRegistration.getFeatures();
            Ln.v("Custom notifications feature toggle enabled ? %s", features.isCustomNotificationsEnabled());
            Ln.v("Custom notifications user toggles direct: %s, @mentions: %s, group: %s",
                    features.isUserFeatureEnabled(Features.USER_TOGGLE_DIRECT_MESSAGE_NOTIFICATIONS),
                    features.isUserFeatureEnabled(Features.USER_TOGGLE_MENTION_NOTIFICATIONS),
                    features.isUserFeatureEnabled(Features.USER_TOGGLE_GROUP_MESSAGE_NOTIFICATIONS));
            Ln.v("Listing all user feature toggles:");
            for (FeatureToggle featureToggle : features.getUserFeatures()) {
                Ln.v("%-60s : %-5s lastModified: %s", featureToggle.getKey(), featureToggle.getVal(), featureToggle.getLastModified());
            }
            this.showSupportText = deviceRegistration.showSupportText();
            this.reportingSiteUrl = deviceRegistration.getReportingSiteUrl();
            this.reportingSiteDesc = deviceRegistration.getReportingSiteDescription();
            this.partnerLogoUrl = deviceRegistration.getPartnerLogoUrl();
            this.partnerCompanyName = deviceRegistration.getPartnerCompanyName();
            this.customerLogoUrl = deviceRegistration.getCustomerLogoUrl();
            this.customerCompanyName = deviceRegistration.getCustomerCompanyName();
            this.supportProviderLogoUrl = deviceRegistration.getSupportProviderLogoUrl();
            this.supportProviderCompanyName = deviceRegistration.getSupportProviderCompanyName();
            this.isDeviceManaged = deviceRegistration.getIsDeviceManaged();
            this.deviceStatusUrl = deviceRegistration.getDeviceStatusURL();
            this.deviceIdentifier = deviceRegistration.getDeviceIdentifier();
            this.clientSecurityPolicy = deviceRegistration.getClientSecurityPolicy();
            this.modificationTime = deviceRegistration.getModificationTime();
            refreshWhitelist();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        synchronized (synclock) {
            this.id = id;
        }
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public Uri getUrl() {
        return url;
    }

    public Features getFeatures() {
        return features;
    }

    public Uri getWebSocketUrl() {
        if (webSocketUrl == null) {
            return null;
        }

        return addQueriesForWebSocketUrl();
    }

    public Uri getConversationServiceUrl() {
        return services == null ? null : services.conversationServiceUrl;
    }

    public Uri getRoomServiceUrl() {
        return services == null ? null : services.roomServiceUrl;
    }

    public Uri getLocusServiceUrl() {
        return services == null ? null : services.locusServiceUrl;
    }

    public Uri getJanusServiceUrl() {
        return services == null ? null : services.janusServiceUrl;
    }

    public Uri getAclServiceUrl() {
        return services == null ? null : services.aclServiceUrl;
    }

    public Uri getAvatarServiceUrl() {
        return services == null ? null : services.avatarServiceUrl;
    }

    public Uri getMetricsServiceUrl() {
        return services == null ? null : services.metricsServiceUrl;
    }

    public Uri getEncryptionServiceUrl() {
        return services != null ? services.encryptionServiceUrl : null;
    }

    public Uri getProximityServiceUrl() {
        return services != null ? services.proximityServiceUrl : null;
    }

    public Uri getFilesServiceUrl() {
        return services != null ? services.filesServiceUrl : null;
    }

    public Uri getSquaredFilesServiceUrl() {
        return services != null ? services.squaredFilesServiceUrl : null;
    }

    public Uri getAdminServiceUrl() {
        return services != null ? services.atlasServiceUrl : null;
    }

    public Uri getFeatureServiceUrl() {
        return services != null ? services.featureServiceUrl : null;
    }

    public Uri getCalendarServiceUrl() {
        return services != null ? services.calendarServiceUrl : null;
    }

    public Uri getStickiesServiceUrl() {
        return services != null ? services.stickiesServiceUrl : null;
    }

    public Uri getSearchServiceUrl() {
        return services != null ? services.argonautServiceUrl : null;
    }

    public Uri getSwUpgradeServiceUrl() {
        return services == null ? null : services.swupgradeServiceUrl;
    }

    public Uri getCalliopeDiscoveryServiceUrl() {
        return services == null ? null : services.calliopeDiscoveryServiceUrl;
    }

    public Uri getUserAppsServiceUrl() {
        return services == null ? null : services.userAppsServiceUrl;
    }

    public Uri getHecateServiceUrl() {
        return services != null ? services.hecateServiceUrl : null;
    }

    public String getDeviceSettingsString() {
        return deviceSettingsString;
    }

    public boolean sendAuthToHost(String host) {
        synchronized (synclock) {
            return host != null && (serviceCatalogWhitelist.contains(host) || buildConfigWhitelist.contains(host));
        }
    }

    public boolean sendAuthToHost(Uri uri) {
        return sendAuthToHost(uri.getHost());
    }

    public boolean showSupportText() {
        return this.showSupportText;
    }

    public String getReportingSiteDescription() {
        return this.reportingSiteDesc;
    }

    public Uri getReportingSiteUrl() {
        return this.reportingSiteUrl;
    }

    public String getSupportProviderCompanyName() {
        return supportProviderCompanyName;
    }

    public Uri getSupportProviderLogoUrl() {
        return supportProviderLogoUrl;
    }

    public String getCustomerCompanyName() {
        return customerCompanyName;
    }

    public Uri getCustomerLogoUrl() {
        return customerLogoUrl;
    }

    public String getPartnerCompanyName() {
        return partnerCompanyName;
    }

    public Uri getPartnerLogoUrl() {
        return partnerLogoUrl;
    }

    public Uri getPresenceServiceUrl() {
        return services.apheleiaServiceUrl;
    }

    public Uri getBoardServiceUrl() {
        return services.boardServiceUrl;
    }

    public Uri getLyraServiceUrl() {
        return services.lyraServiceUrl;
    }

    public String getClientSecurityPolicy() {
        return clientSecurityPolicy;
    }

    public void setFeaturesLoaded(boolean loaded) {
        synchronized (synclock) {
            features.setFeaturesLoaded(loaded);
        }
    }

    public void whitelist(Uri uri) {
        synchronized (synclock) {
            if (uri != null && uri.getHost() != null) {
                serviceCatalogWhitelist.add(uri.getHost());
            }
        }
    }

    public Set<String> getWhiteList() {
        synchronized (synclock) {
            Set<String> whitelist = new ConcurrentSkipListSet<>();
            whitelist.addAll(buildConfigWhitelist);
            whitelist.addAll(serviceCatalogWhitelist);
            return whitelist;
        }
    }

    public String toJson(Gson gson) {
        synchronized (synclock) {
            return gson.toJson(this);
        }
    }

    public boolean getIsDeviceManaged() {
        return isDeviceManaged;
    }

    public Uri getDeviceStatusURL() {
        return deviceStatusUrl;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    private static class Services {
        private Uri aclServiceUrl;
        private Uri conversationServiceUrl;
        private Uri roomServiceUrl;
        private Uri locusServiceUrl;
        private Uri janusServiceUrl;
        private Uri avatarServiceUrl;
        private Uri metricsServiceUrl;
        private Uri encryptionServiceUrl;
        private Uri proximityServiceUrl;
        private Uri filesServiceUrl;
        private Uri squaredFilesServiceUrl;
        private Uri atlasServiceUrl;
        private Uri featureServiceUrl;
        private Uri calendarServiceUrl;
        private Uri stickiesServiceUrl;
        private Uri argonautServiceUrl;
        private Uri swupgradeServiceUrl;
        private Uri apheleiaServiceUrl;
        private Uri calliopeDiscoveryServiceUrl;
        private Uri userAppsServiceUrl;
        private Uri boardServiceUrl;
        private Uri hecateServiceUrl;
        private Uri lyraServiceUrl;
    }

    private Uri addQueriesForWebSocketUrl() {
        Uri result;
        if (getFeatures().isUseSharedFeatureToggleEnabled()) {
            if (webSocketUrl.toString().endsWith(MERCURY_REGISTRATION_QUERIES)) {
                result = webSocketUrl;
            } else {
                // webSocketUrl might comes from the sharedPreference,
                // need add the query condition manually
                result = Uri.parse(webSocketUrl + MERCURY_REGISTRATION_QUERIES);
            }
        } else {
            result = webSocketUrl;
        }

        return result;
    }

}
