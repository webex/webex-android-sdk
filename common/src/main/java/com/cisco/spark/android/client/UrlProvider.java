package com.cisco.spark.android.client;

import android.support.annotation.Nullable;

public interface UrlProvider {

    String getAtlasUrl();
    String getRegionUrl();
    String getUsersApiUrl();
    String getServiceApiUrl();
    String getCalliopeRegistrarUrl();
    String getOauth2Url();
    String getAclServiceUrl();
    String getIdbrokerTokenUrl();
    String getIdentityApiUrl();

    @Nullable String getMetricsApiUrl();
}
