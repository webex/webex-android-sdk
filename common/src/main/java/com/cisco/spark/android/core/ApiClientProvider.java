package com.cisco.spark.android.core;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.BuildConfig;
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
import com.cisco.spark.android.client.LocusClient;
import com.cisco.spark.android.client.LyraClient;
import com.cisco.spark.android.client.MetricsClient;
import com.cisco.spark.android.client.MetricsPreloginClient;
import com.cisco.spark.android.client.PresenceServiceClient;
import com.cisco.spark.android.client.RegionClient;
import com.cisco.spark.android.client.RoomEmulatorServiceClient;
import com.cisco.spark.android.client.RoomServiceClient;
import com.cisco.spark.android.client.SearchClient;
import com.cisco.spark.android.client.SecRestClient;
import com.cisco.spark.android.client.StickiesServiceClient;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.client.UserClient;
import com.cisco.spark.android.client.WebExFilesClient;
import com.cisco.spark.android.client.WhistlerTestClient;
import com.cisco.spark.android.client.WhiteboardPersistenceClient;
import com.cisco.spark.android.flag.FlagClient;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.WdmClient;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;
import com.jakewharton.retrofit.Ok3Client;

import java.io.IOException;

import javax.inject.Provider;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit2.Call;
import retrofit2.Callback;

public class ApiClientProvider extends BaseApiClientProvider {

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
    private JanusClient janusClient;
    private FeatureClient featureClient;
    private LyraClient lyraClient;
    private WdmClient wdmClient;
    private LocusClient locusClient;
    private CalendarServiceClient calendarServiceClient;
    private HypermediaLocusClient hypermediaLocusClient;
    private SecRestClient securityClient;
    private AdminClient adminClient;
    private StickiesServiceClient stickiesServiceClient;
    private WhistlerTestClient whistlerTestClient;
    private SearchClient searchClient;
    private CalliopeClient calliopeClient;
    private HecateClient hecateClient;


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
        if (authenticatedUserProvider.isAuthenticated()) {
            AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUser();
            if (user != null && user.getOAuth2Tokens() != null) {
                return user.getOAuth2Tokens().shouldRefreshNow();
            }
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

            OkHttpClient client = okHttpClientNoInterceptors()
                    .addInterceptor(interceptor)
                    .addInterceptor(getTokenRefreshInterceptor())
                    .addNetworkInterceptor(getAuthInterceptor(deviceRegistration))
                    .addNetworkInterceptor(getLoggingInterceptor(getLogLevel()))
                    .build();

            webExFilesClient = retrofit(client).build().create(WebExFilesClient.class);
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
            aclClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, deviceRegistration).build()).baseUrl(url.toString()).build().create(AclClient.class);
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

        OkHttpClient client = okHttpClientNoInterceptors()
                .addInterceptor(interceptor)
                .addInterceptor(getTokenRefreshInterceptor())
                .addNetworkInterceptor(getAuthInterceptor(deviceRegistration))
                .addNetworkInterceptor(getLoggingInterceptor(LoggingInterceptor.Level.REQUEST_BODY))
                .build();

        return retrofit(client).baseUrl(urlString).build().create(AclClient.class);
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
            avatarClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, deviceRegistration).build()).baseUrl(url.toString()).build().create(AvatarClient.class);
        }
        return avatarClient;
    }

    @Deprecated
    public synchronized HyperSecRestClient getHyperSecRestClient() {
        if (hyperSecRestClient == null) {
            hyperSecRestClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, deviceRegistration).build()).build().create(HyperSecRestClient.class);
        }
        return hyperSecRestClient;
    }

    public synchronized OAuth2Client getOAuthClient() {
        if (oAuth2Client == null) {
            OkHttpClient client = okHttpClientNoInterceptors()
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
                    .addNetworkInterceptor(getLoggingInterceptor(getLogLevel()))
                    .addInterceptor(getResponseBodyLoggingInterceptor())
                    .build();

            oAuth2Client = retrofit(client)
                    .baseUrl(urlProvider.getOauth2Url())
                    .build()
                    .create(OAuth2Client.class);
        }
        return oAuth2Client;
    }


    public synchronized UserClient getUserClient() {
        if (userClient == null) {
            userClient = retrofit(deviceRegistration).baseUrl(BuildConfig.USERS_API_URL).build().create(UserClient.class);
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
            whiteboardPersistenceClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, deviceRegistration).build()).baseUrl(url.toString()).build().create(WhiteboardPersistenceClient.class);
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
            flagClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(FlagClient.class);
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
            conversationClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(ConversationClient.class);
        }
        return conversationClient;
    }

    public synchronized RoomServiceClient getRoomServiceClient() {
        if (roomServiceClient == null) {
            Uri url = deviceRegistration.getRoomServiceUrl();
            if (url == null) {
                ln.w("Device data reported null room service url");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            roomServiceClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(RoomServiceClient.class);
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
            roomEmulatorServiceClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(RoomEmulatorServiceClient.class);
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
            apheleiaClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(PresenceServiceClient.class);
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
            janusClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(JanusClient.class);
        }
        return janusClient;
    }

    public synchronized RegionClient getRegionClient() {
        if (regionClient == null) {
            Uri url = Uri.parse(urlProvider.getRegionUrl()).buildUpon().appendPath("").build();
            regionClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(RegionClient.class);
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
            featureClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(FeatureClient.class);
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
            lyraClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, deviceRegistration).build()).baseUrl(url.toString()).build().create(LyraClient.class);
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
            metricsClient = retrofit(deviceRegistration).baseUrl(url.toString()).build().create(MetricsClient.class);

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
            locusClient = retrofit(deviceRegistration)
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
            RestAdapter restAdapter = buildStandardRestAdapter(url.toString(), deviceRegistration);
            calendarServiceClient = restAdapter.create(CalendarServiceClient.class);
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
            RestAdapter restAdapter = buildStandardRestAdapter(adminUri.toString(), deviceRegistration);
            adminClient = restAdapter.create(AdminClient.class);
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
            RestAdapter restAdapter = buildStandardRestAdapter(securityServiceUrl.toString(), deviceRegistration);
            securityClient = restAdapter.create(SecRestClient.class);
        }
        return securityClient;
    }

    public synchronized WdmClient getWdmClient() {
        if (wdmClient == null) {
            String wdmUrl;

            if (!TextUtils.isEmpty(settings.getCustomWdmUrl())) {
                wdmUrl = settings.getCustomWdmUrl();
                deviceRegistration.whitelist(Uri.parse(wdmUrl));
            } else {
                wdmUrl = urlProvider.getServiceApiUrl();
            }

            RestAdapter restAdapter = createRestAdapterBuilder()
                    .setConverter(new GsonConverter(gson))
                    .setClient(new Ok3Client(buildOkHttpClient(deviceRegistration)))
                    .setEndpoint(wdmUrl)
                    .build();
            wdmClient = restAdapter.create(WdmClient.class);
        }
        return wdmClient;
    }

    public synchronized HypermediaLocusClient getHypermediaLocusClient() {
        if (hypermediaLocusClient == null) {
            hypermediaLocusClient = retrofit(okHttpClient(LoggingInterceptor.Level.HEADERS, deviceRegistration).build())
                    .callbackExecutor(callbackExecutor)
                    .build().create(HypermediaLocusClient.class);
        }

        return hypermediaLocusClient;
    }

    public synchronized StickiesServiceClient getStickiesClient() {
        if (stickiesServiceClient == null) {

            Uri url = deviceRegistration.getStickiesServiceUrl();
            if (url == null) {
                ln.w("Device data reported null Stickies service url");
                throw new NotAuthenticatedException();
            }
            RestAdapter restAdapter = buildStandardRestAdapter(url.toString(), deviceRegistration);
            stickiesServiceClient = restAdapter.create(StickiesServiceClient.class);
        }
        return stickiesServiceClient;
    }

    public synchronized SearchClient getSearchClient() {
        if (searchClient == null) {
            Uri searchClientUri = deviceRegistration.getSearchServiceUrl();
            if (searchClientUri == null) {
                ln.w("Device data reported null argonaut service url");
                throw new NotAuthenticatedException();
            }
            RestAdapter restAdapter = buildStandardRestAdapter(searchClientUri.toString(), deviceRegistration);
            searchClient = restAdapter.create(SearchClient.class);
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
            RestAdapter restAdapter = buildStandardRestAdapter(calliopeClientUri.toString(), deviceRegistration);
            calliopeClient = restAdapter.create(CalliopeClient.class);
        }
        return calliopeClient;
    }

    public synchronized WhistlerTestClient getWhistlerTestClient() {
        if (whistlerTestClient == null) {

            final String url = WhistlerTestClient.URL;

            RestAdapter restAdapter = buildStandardRestAdapter(url, deviceRegistration);
            whistlerTestClient = restAdapter.create(WhistlerTestClient.class);
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
            RestAdapter restAdapter = buildStandardRestAdapter(url.toString(), deviceRegistration);
            hecateClient = restAdapter.create(HecateClient.class);
        }

        return hecateClient;
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
