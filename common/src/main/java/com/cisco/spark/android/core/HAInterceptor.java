package com.cisco.spark.android.core;

import android.net.Uri;

import com.cisco.spark.android.wdm.HostMap;
import com.cisco.spark.android.wdm.ServiceHost;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HAInterceptor implements Interceptor {

    private String hostname;

    private ServiceHosts serviceHosts = null;


    public HAInterceptor(Uri url, HostMap hostMap) {
        this.hostname = url == null ? "" : url.getHost();

        // hostMap is currently only populated when the 'wdm-u2c-lookup2' toggle is enabled.
        if (hostMap != null) {

            List<ServiceHost> serviceHosts = hostMap.getServiceHost(this.hostname);

            if (serviceHosts != null) {
                this.serviceHosts = new ServiceHosts(serviceHosts);
            }
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        if (serviceHosts == null) {
            // There are no configured HA servers for this service.
            return chain.proceed(chain.request());
        }

        Request originalRequest = chain.request();

        String originalUrl = originalRequest.url().toString();


        Request newRequest = originalRequest;
        String newHost = "";

        if (originalUrl.contains(hostname)) {

            newHost = serviceHosts.getHost();

            if (newHost != null) {
                String newUrl = originalUrl.replace(hostname, newHost);
                newRequest = originalRequest.newBuilder().url(newUrl).build();
            }
        }

        try {
            Response response = chain.proceed(newRequest);

            if (isServerError(response)) {
                serviceHosts.markHostFailed(newHost);
            }

            return response;
        } catch (Exception e) {
            serviceHosts.markHostFailed(newHost);
            throw e;
        }
    }

    private boolean isServerError(Response response) {
        return response.code() >= 500;
    }
}
