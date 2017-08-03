package com.cisco.spark.android.core;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.authenticator.OAuth2Client;
import com.cisco.spark.android.client.AclClient;
import com.cisco.spark.android.client.AdminClient;
import com.cisco.spark.android.client.AvatarClient;
import com.cisco.spark.android.client.CalendarServiceClient;
import com.cisco.spark.android.client.CalliopeClient;
import com.cisco.spark.android.client.ConversationClient;
import com.cisco.spark.android.client.FeatureClient;
import com.cisco.spark.android.client.HecateClient;
import com.cisco.spark.android.client.HyperSecRestClient;
import com.cisco.spark.android.client.HypermediaLocusClient;
import com.cisco.spark.android.client.JanusClient;
import com.cisco.spark.android.client.LinusClient;
import com.cisco.spark.android.client.LocusClient;
import com.cisco.spark.android.client.LyraClient;
import com.cisco.spark.android.client.LyraProximityServiceClient;
import com.cisco.spark.android.client.MetricsClient;
import com.cisco.spark.android.client.MetricsPreloginClient;
import com.cisco.spark.android.client.PresenceServiceClient;
import com.cisco.spark.android.client.RegionClient;
import com.cisco.spark.android.client.RetentionClient;
import com.cisco.spark.android.client.RoomEmulatorServiceClient;
import com.cisco.spark.android.client.RoomServiceClient;
import com.cisco.spark.android.client.SearchClient;
import com.cisco.spark.android.client.SecRestClient;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.client.UserClient;
import com.cisco.spark.android.client.WebExFilesClient;
import com.cisco.spark.android.client.WhistlerTestClient;
import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.flag.FlagClient;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.WdmClient;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.inject.Provider;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientProvider extends BaseApiClientProvider {
    private static Converter<ResponseBody, ErrorDetail> errorDetailConverter;

    protected final DeviceRegistration deviceRegistration;

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UrlProvider urlProvider;
    private AclClient aclClient;
    private WebExFilesClient webExFilesClient;
    private AvatarClient avatarClient;
    private HyperSecRestClient hyperSecRestClient;
    private RoomServiceClient roomServiceClient;
    private RoomEmulatorServiceClient roomEmulatorServiceClient;
    private WhiteboardPersistenceClient whiteboardPersistenceClient;
    private PresenceServiceClient apheleiaClient;
    private MetricsClient metricsClient;
    private MetricsPreloginClient metricsPreloginClient;
    private final NaturalLog ln;
    private OAuth2Client oAuth2Client;
    private UserClient userClient;
    private FlagClient flagClient;
    private RegionClient regionClient;
    private ConversationClient conversationClient;
    private RetentionClient retentionClient;
    private JanusClient janusClient;
    private FeatureClient featureClient;
    private LyraClient lyraClient;
    private WdmClient wdmClient;
    private LocusClient locusClient;
    private CalendarServiceClient calendarServiceClient;
    private HypermediaLocusClient hypermediaLocusClient;
    protected LinusClient linusClient;
    private SecRestClient securityClient;
    private AdminClient adminClient;
    private WhistlerTestClient whistlerTestClient;
    private SearchClient searchClient;
    private CalliopeClient calliopeClient;
    private HecateClient hecateClient;
    private LyraProximityServiceClient lyraProximityServiceClient;

    public static final Callback<Void> NULL_CALLBACK = new Callback<Void>() {
        @Override
        public void onResponse(Call call, retrofit2.Response response) {
        }

        @Override
        public void onFailure(Call call, Throwable t) {
            Ln.e("t, Failed Async Request: " + call.request().url());
        }
    };

    public ApiClientProvider(AuthenticatedUserProvider authenticatedUserProvider,
                             UserAgentProvider userAgentProvider,
                             TrackingIdGenerator trackingIdGenerator,
                             Gson gson, DeviceRegistration deviceRegistration,
                             EventBus bus, Settings settings, Context context,
                             Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientBuilderProvider,
                             Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {

        super(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue,  new SquaredCertificatePinner(context, deviceRegistration.getWhiteList(), bus, urlProvider));

        this.authenticatedUserProvider = authenticatedUserProvider;
        this.deviceRegistration = deviceRegistration;
        this.urlProvider = urlProvider;

        this.ln = Ln.get(lnContext, "Api");
    }

    @Override
    protected String getAuthHeader() {
        AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUserOrNull();
        if (user != null)
            return user.getConversationAuthorizationHeader();
        return null;
    }

    @Override
    protected boolean shouldRefreshTokensNow() {
        AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUserOrNull();
        if (user != null && user.getOAuth2Tokens() != null) {
            return user.getOAuth2Tokens().shouldRefreshNow();
        }
        return false;
    }

    public synchronized WebExFilesClient getFilesClient() {
        if (webExFilesClient == null) {
            Interceptor interceptor = new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws IOException {
                    Request request = chain.request()
                            .newBuilder()
                            .header(TRACKING_ID_HEADER, trackingIdGenerator.nextTrackingId())
                            .build();
                    return chain.proceed(request);
                }
            };

            OkHttpClient.Builder clientBuilder = okHttpClientNoInterceptors()
                    .addInterceptor(interceptor)
                    .addInterceptor(getTokenRefreshInterceptor())
                    .addNetworkInterceptor(getAuthInterceptor(deviceRegistration));

            for (LoggingInterceptor logInterceptor : getLoggingInterceptors(getLogLevel())) {
                clientBuilder.addNetworkInterceptor(logInterceptor);
            }

            webExFilesClient = retrofit(clientBuilder.build()).build().create(WebExFilesClient.class);
        }
        return webExFilesClient;
    }

    public synchronized AclClient getAclClient() {
        if (aclClient == null) {
            // TODO: Use deviceRegistration.getAclServiceUrl() if wdm is ready.
            // TODO: Also need to remove getAclServiceUrl in urlProvider
            String aclServiceUrl = urlProvider.getAclServiceUrl();
            if (TextUtils.isEmpty(aclServiceUrl)) {
                ln.e("Acl service url is null");
                throw new NotAuthenticatedException();
            }
            Uri url = Uri.parse(aclServiceUrl);
            url = Uri.withAppendedPath(url, "");
            aclClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, url, deviceRegistration).build()).baseUrl(url.toString()).build().create(AclClient.class);
        }
        return aclClient;
    }

    public synchronized AclClient getAclClient(Uri url) {

        Interceptor interceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request()
                        .newBuilder()
                        .header(TRACKING_ID_HEADER, trackingIdGenerator.nextTrackingId())
                        .build();
                return chain.proceed(request);
            }
        };

        String urlString = sanitizeRetrofit2Url(url.toString());

        OkHttpClient.Builder clientBuilder = okHttpClientNoInterceptors()
                .addInterceptor(interceptor)
                .addInterceptor(getTokenRefreshInterceptor())
                .addInterceptor(getHAIntercepter(url, deviceRegistration))
                .addNetworkInterceptor(getAuthInterceptor(deviceRegistration));

        for (LoggingInterceptor logInterceptor : getLoggingInterceptors(LoggingInterceptor.Level.REQUEST_BODY)) {
            clientBuilder.addNetworkInterceptor(logInterceptor);
        }

        return retrofit(clientBuilder.build()).baseUrl(urlString).build().create(AclClient.class);
    }

    public String sanitizeRetrofit2Url(String url) {

        if (url.charAt(url.length() - 1) != '/') {
            url += "/";
        }

        return url;
    }

    public synchronized AvatarClient getAvatarClient() {
        if (avatarClient == null) {
            Uri url = deviceRegistration.getAvatarServiceUrl();
            if (url == null) {
                ln.w("Avatar service url is null");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            avatarClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, url, deviceRegistration).build()).baseUrl(url.toString()).build().create(AvatarClient.class);
        }
        return avatarClient;
    }

    @Deprecated
    public synchronized HyperSecRestClient getHyperSecRestClient() {
        if (hyperSecRestClient == null) {
            hyperSecRestClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, null, deviceRegistration).build()).build().create(HyperSecRestClient.class);
        }
        return hyperSecRestClient;
    }

    public synchronized OAuth2Client getOAuthClient() {
        if (oAuth2Client == null) {
            OkHttpClient.Builder clientBuilder = okHttpClientNoInterceptors()
                    // Just UA header and logging interceptors for this client
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request request = chain.request().newBuilder()
                                    .addHeader("User-Agent", userAgentProvider.get())
                                    .build();
                            return chain.proceed(request);
                        }
                    })
                    .addInterceptor(getResponseBodyLoggingInterceptor());

            for (LoggingInterceptor interceptor : getLoggingInterceptors(getLogLevel())) {
                clientBuilder.addNetworkInterceptor(interceptor);
            }

            oAuth2Client = retrofit(clientBuilder.build())
                    .baseUrl(urlProvider.getOauth2Url())
                    .build()
                    .create(OAuth2Client.class);
        }
        return oAuth2Client;
    }


    public synchronized UserClient getUserClient() {
        if (userClient == null) {
            userClient = retrofit(Uri.parse(urlProvider.getUsersApiUrl()), deviceRegistration).baseUrl(urlProvider.getUsersApiUrl()).build().create(UserClient.class);
        }
        return userClient;
    }

    public synchronized WhiteboardPersistenceClient getWhiteboardPersistenceClient() {
        if (whiteboardPersistenceClient == null) {
            Uri url = deviceRegistration.getBoardServiceUrl();
            if (url == null) {
                ln.w("Device data reported null whiteboard service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            whiteboardPersistenceClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, url, deviceRegistration).build()).baseUrl(url.toString()).build().create(WhiteboardPersistenceClient.class);
        }

        return whiteboardPersistenceClient;
    }

    public synchronized FlagClient getFlagClient() {
        if (flagClient == null) {
            Uri url = deviceRegistration.getUserAppsServiceUrl();
            if (url == null) {
                ln.w("Device data reported null conversation service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            flagClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(FlagClient.class);
        }
        return flagClient;
    }

    public synchronized ConversationClient getConversationClient() {
        if (conversationClient == null) {
            Uri url = deviceRegistration.getConversationServiceUrl();
            if (url == null) {
                ln.w("Device data reported null conversation service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            conversationClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(ConversationClient.class);
        }
        return conversationClient;
    }

    public synchronized RetentionClient getRetentionClient() {
        if (retentionClient == null) {
            Uri url = deviceRegistration.getRetentionServiceUrl();
            if (url == null) {
                ln.e("Device data reported null retention service url");
                return null;
            }
            url = Uri.withAppendedPath(url, "");
            retentionClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(RetentionClient.class);
        }
        return retentionClient;
    }

    public synchronized RoomServiceClient getRoomServiceClient() {
        if (roomServiceClient == null) {
            Uri url = deviceRegistration.getRoomServiceUrl();
            if (url == null) {
                ln.w("Device data reported null room service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            roomServiceClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(RoomServiceClient.class);
        }
        return roomServiceClient;
    }

    public synchronized RoomEmulatorServiceClient getRoomEmulatorServiceClient() {
        if (roomServiceClient == null) {
            Uri url = deviceRegistration.getRoomServiceUrl();
            if (url == null) {
                ln.w("Device data reported null room service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            roomEmulatorServiceClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(RoomEmulatorServiceClient.class);
        }
        return roomEmulatorServiceClient;
    }

    public synchronized PresenceServiceClient getPresenceClient() {
        if (apheleiaClient == null) {
            Uri url = deviceRegistration.getPresenceServiceUrl();
            if (url == null) {
                ln.w("Device data reported null conversation service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            apheleiaClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(PresenceServiceClient.class);
        }
        return apheleiaClient;
    }

    public synchronized JanusClient getJanusClient() {
        if (janusClient == null) {
            Uri url = deviceRegistration.getJanusServiceUrl();
            if (url == null) {
                ln.w("Device data reported null janus service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            janusClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(JanusClient.class);
        }
        return janusClient;
    }

    public synchronized RegionClient getRegionClient() {
        if (regionClient == null) {
            Uri url = Uri.parse(urlProvider.getRegionUrl()).buildUpon().appendPath("").build();
            regionClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(RegionClient.class);
        }
        return regionClient;
    }

    public synchronized FeatureClient getFeatureClient() {
        if (featureClient == null) {
            Uri url = deviceRegistration.getFeatureServiceUrl();
            if (url == null) {
                ln.w("Device data reported null feature service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            featureClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(FeatureClient.class);
        }
        return featureClient;
    }

    public synchronized LyraClient getLyraClient() {
        if (lyraClient == null) {
            Uri url = deviceRegistration.getLyraServiceUrl();
            if (url == null) {
                ln.w("Device data reported null Lyra service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            lyraClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, url, deviceRegistration).build()).baseUrl(url.toString()).build().create(LyraClient.class);
        }

        return lyraClient;
    }

    public synchronized MetricsClient getMetricsClient() {
        if (metricsClient == null) {
            Uri url = deviceRegistration.getMetricsServiceUrl();

            if (url == null) {
                ln.w("Device data reported null metrics service url");
                throw new NotAuthenticatedException();
            }

            url = Uri.withAppendedPath(url, "");
            metricsClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(MetricsClient.class);

        }
        return metricsClient;
    }

    public synchronized MetricsPreloginClient getMetricsPreloginClient() {
        if (metricsPreloginClient == null) {
            Uri url = Uri.parse(urlProvider.getMetricsApiUrl());

            url = Uri.withAppendedPath(url, "");
            metricsPreloginClient = retrofitPrelogin(deviceRegistration)
                    .baseUrl(url.toString())
                    .build()
                    .create(MetricsPreloginClient.class);

        }
        return metricsPreloginClient;
    }

    //
    //TODO migrate from BaseApiClientProvider
    // These getters are ready but the underlying Client classes need migration.

    public synchronized LocusClient getLocusClient() {
        if (locusClient == null) {
            Uri url = deviceRegistration.getLocusServiceUrl();
            if (url == null) {
                ln.w("Device data reported null locus service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            locusClient = retrofit(okHttpClient(getLocusLogLevel(), url, deviceRegistration).build())
                    .baseUrl(url.toString())
                    .callbackExecutor(callbackExecutor)
                    .build()
                    .create(LocusClient.class);
        }
        return locusClient;
    }

    public synchronized CalendarServiceClient getCalendarServiceClient() {
        if (calendarServiceClient == null) {
            Uri url = deviceRegistration.getCalendarServiceUrl();
            if (url == null) {
                ln.w("Device data reported null calendar service url.");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            calendarServiceClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(CalendarServiceClient.class);
        }
        return calendarServiceClient;
    }

    public synchronized AdminClient getAdminClient() {
        if (adminClient == null) {
            Uri adminUri = deviceRegistration.getAdminServiceUrl();
            if (adminUri == null) {
                ln.w("null admin service uri");
                throw new NotAuthenticatedException();
            }
            adminUri = Uri.withAppendedPath(adminUri, "");
            adminClient = retrofit(adminUri, deviceRegistration).baseUrl(adminUri.toString()).build().create(AdminClient.class);
        }
        return adminClient;
    }

    public synchronized SecRestClient getSecurityClient() {
        if (securityClient == null) {
            Uri securityServiceUrl = deviceRegistration.getEncryptionServiceUrl();
            if (securityServiceUrl == null) {
                ln.w("Device data reported null SecREST service url");
                throw new NotAuthenticatedException();
            }
            securityServiceUrl = Uri.withAppendedPath(securityServiceUrl, "");
            securityClient = retrofit(securityServiceUrl, deviceRegistration).baseUrl(securityServiceUrl.toString()).build().create(SecRestClient.class);
        }
        return securityClient;
    }

    public synchronized WdmClient getWdmClient() {
        if (wdmClient == null) {
            Uri wdmUrl;

            if (!TextUtils.isEmpty(settings.getCustomWdmUrl())) {
                wdmUrl = Uri.parse(settings.getCustomWdmUrl());
                deviceRegistration.buildConfigWhitelist(wdmUrl);
            } else {
                wdmUrl = Uri.parse(urlProvider.getServiceApiUrl());
            }

            wdmUrl = Uri.withAppendedPath(wdmUrl, "");
            wdmClient = retrofit(wdmUrl, deviceRegistration).baseUrl(wdmUrl.toString()).build().create(WdmClient.class);
        }
        return wdmClient;
    }

    public synchronized HypermediaLocusClient getHypermediaLocusClient() {
        if (hypermediaLocusClient == null) {
            hypermediaLocusClient = retrofit(okHttpClient(getLocusLogLevel(), deviceRegistration.getLocusServiceUrl(), deviceRegistration).build())
                    .callbackExecutor(callbackExecutor)
                    .build().create(HypermediaLocusClient.class);
        }

        return hypermediaLocusClient;
    }

    public synchronized LinusClient getLinusClient() {
        if (linusClient == null) {
            linusClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, null, deviceRegistration).build())
                    .callbackExecutor(callbackExecutor)
                    .build().create(LinusClient.class);
        }

        return linusClient;
    }

    public synchronized SearchClient getSearchClient() {
        if (searchClient == null) {
            Uri searchClientUri = deviceRegistration.getSearchServiceUrl();
            if (searchClientUri == null) {
                ln.w("Device data reported null argonaut service url");
                throw new NotAuthenticatedException();
            }

            searchClientUri = Uri.withAppendedPath(searchClientUri, "");
            searchClient = retrofit(searchClientUri, deviceRegistration).baseUrl(searchClientUri.toString()).build().create(SearchClient.class);
        }
        return searchClient;
    }

    public synchronized CalliopeClient getCalliopeClient() {
        if (calliopeClient == null) {
            Uri calliopeClientUri = deviceRegistration.getCalliopeDiscoveryServiceUrl();
            if (calliopeClientUri == null) {
                ln.w("Device data reported null calliope service url");
                throw new NotAuthenticatedException();
            }
            calliopeClientUri = Uri.withAppendedPath(calliopeClientUri, "");
            calliopeClient = retrofit(calliopeClientUri, deviceRegistration).baseUrl(calliopeClientUri.toString()).build().create(CalliopeClient.class);
        }
        return calliopeClient;
    }

    public synchronized WhistlerTestClient getWhistlerTestClient() {
        if (whistlerTestClient == null) {

            Uri url = WhistlerTestClient.URL;
            // There is no corresponding service in the service collection
            url = Uri.withAppendedPath(url, "");
            whistlerTestClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(WhistlerTestClient.class);
        }

        return whistlerTestClient;
    }

    public synchronized HecateClient getHecateClient() {
        if (hecateClient == null) {
            Uri url = deviceRegistration.getHecateServiceUrl();
            if (url == null) {
                ln.w("Device data reported null hecate service url");
                throw new NotAuthenticatedException();
            }

            url = Uri.withAppendedPath(url, "");
            hecateClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(HecateClient.class);
        }

        return hecateClient;
    }

    public synchronized LyraProximityServiceClient getLyraProximityServiceClient() {
        if (lyraProximityServiceClient == null) {
            Uri url = deviceRegistration.getProximityServiceUrl();
            if (url == null) {
                ln.e("Lyra proximity service url is null");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            lyraProximityServiceClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, url, deviceRegistration).build()).baseUrl(url.toString()).build().create(LyraProximityServiceClient.class);
        }
        return lyraProximityServiceClient;
    }

    public static Converter<ResponseBody, ErrorDetail> getErrorDetailConverter() {
        if (errorDetailConverter == null) {
            errorDetailConverter = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create(new Gson()))
                    .baseUrl(DEFAULT_BASE_URL)
                    .build()
                    .responseBodyConverter(ErrorDetail.class, new Annotation[0]);
        }
        return errorDetailConverter;
    }

    //Used for testing
    public boolean getIsNetworkConnected() {
        throw new RuntimeException("Not Implemented");
    }

    // Used for testing
    public void setServerReturnCode(int statusCode) {
        throw new RuntimeException("Not Implemented");
    }

    // Used for testing
    public int getServerReturnCode() {
        throw new RuntimeException("Not Implemented");
    }

    // Used for testing
    public void resetServiceClient() {
        wdmClient = null;
    }
}
