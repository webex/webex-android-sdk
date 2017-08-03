package com.cisco.spark.android.client;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.OAuth2;

public class SquaredUrlProvider implements UrlProvider {

    @Override
    public String getAtlasUrl() {
        return BuildConfig.ATLAS_API_URL;
    }

    @Override
    public String getRegionUrl() {
        return BuildConfig.REGION_API_URL;
    }

    @Override
    public String getUsersApiUrl() {
        return BuildConfig.USERS_API_URL;
    }

    @Override
    public String getServiceApiUrl() {
        return BuildConfig.WX2_SERVICE_API_URL;
    }

    @Override
    public String getCalliopeRegistrarUrl() {
        return BuildConfig.CALLIOPE_REGISTRAR_URL;
    }

    @Override
    public String getOauth2Url() {
        return OAuth2.BASE_URL;
    }

    @Override
    public String getAclServiceUrl() {
        return BuildConfig.WB_ACL_URL;
    }

    @Override
    public String getMetricsApiUrl() {
        return BuildConfig.METRICS_API_URL;
    }

    @Override
    public String getIdbrokerTokenUrl() {
        return BuildConfig.IDBROKER_TOKEN_URL;
    }

    @Override
    public String getIdentityApiUrl() {
        return BuildConfig.IDENTITY_API_URL;
    }
}
