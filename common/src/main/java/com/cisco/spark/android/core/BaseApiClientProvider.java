package com.cisco.spark.android.core;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.DiagnosticModeChangedEvent;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.net.ssl.SSLContext;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class BaseApiClientProvider {
    // used as a placeholder for clients that use absolute @Url parameters
    protected static final String DEFAULT_BASE_URL = "https://cisco.com/";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TRACKING_ID_HEADER = "TrackingID";
    private static final String CISCO_DEVICE_URL_HEADER = "Cisco-Device-URL";
    private static final String LOG_LEVEL_TOKEN_HEADER = "LogLevelToken";
    private static final String ACCEPT_LANGUAGE = "Accept-Language";
    private static final Set<String> SENSITIVE_HEADERS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(AUTHORIZATION_HEADER, TRACKING_ID_HEADER, CISCO_DEVICE_URL_HEADER, LOG_LEVEL_TOKEN_HEADER)));

    private String verboseLoggingToken = null;

    public static final int HTTP_PERMANENT_REDIRECT = 308;

    protected final UserAgentProvider userAgentProvider;
    protected final TrackingIdGenerator trackingIdGenerator;
    protected final Gson gson;
    protected final Settings settings;
    protected final Provider<OkHttpClient.Builder> okHttpClientBuilderProvider;
    private final Lazy<OperationQueue> operationQueue;
    protected final SquaredCertificatePinner certificatePinner;
    private final NaturalLog ln;
    private ConnectionPool connectionPool;
    private int id = 0;
    protected Executor callbackExecutor = Executors.newCachedThreadPool(getThreadFactory("Async HTTP Callback"));


    public BaseApiClientProvider(UserAgentProvider userAgentProvider,
                                 TrackingIdGenerator trackingIdGenerator,
                                 Gson gson,
                                 EventBus bus, Settings settings, Context context,
                                 Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientProvider,
                                 Lazy<OperationQueue> operationQueue, SquaredCertificatePinner certificatePinner) {
        this.userAgentProvider = userAgentProvider;
        this.trackingIdGenerator = trackingIdGenerator;
        this.gson = gson;
        this.settings = settings;
        this.okHttpClientBuilderProvider = okHttpClientProvider;
        this.operationQueue = operationQueue;
        this.certificatePinner = certificatePinner;
        this.ln = Ln.get(lnContext, "Api");
        bus.register(this);
    }

    protected LoggingInterceptor.Level getLogLevel() {
        return Ln.isDebugEnabled() ? LoggingInterceptor.Level.REQUEST_BODY : LoggingInterceptor.Level.BASIC;
    }

    protected LoggingInterceptor.Level getLocusLogLevel() {
        return Ln.isDebugEnabled() ? LoggingInterceptor.Level.REQUEST_BODY : LoggingInterceptor.Level.HEADERS;
    }

    protected abstract boolean shouldRefreshTokensNow();
    protected abstract String getAuthHeader();

    private ThreadFactory getThreadFactory(final String name) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread ret = new Thread(runnable);
                ret.setName(name + " " + id++);
                ret.setDaemon(true);
                return ret;
            }
        };
    }

    protected Interceptor getCommonHeadersInterceptor(final DeviceRegistration deviceRegistration) {
        return new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Request.Builder requestBuilder = chain.request().newBuilder()
                        .addHeader("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header(TRACKING_ID_HEADER, trackingIdGenerator.nextTrackingId());

                addHeaderWithDefaultValue(requestBuilder, "User-Agent", userAgentProvider.get(), UserAgentProvider.APP_NAME);

                if (deviceRegistration != null && deviceRegistration.getUrl() != null) {
                    requestBuilder.header(CISCO_DEVICE_URL_HEADER, deviceRegistration.getUrl().toString());
                }
                // Avoid race where the token gets reset to null on the event bus.
                String localToken = verboseLoggingToken;
                if (localToken != null)
                    requestBuilder.header(LOG_LEVEL_TOKEN_HEADER, localToken);
                requestBuilder.addHeader(ACCEPT_LANGUAGE, String.format("%s-%s", Locale.getDefault().getLanguage(), Locale.getDefault().getCountry()));
                return chain.proceed(requestBuilder.build());
            }
        };
    }

    private void addHeaderWithDefaultValue(okhttp3.Request.Builder builder, String headerName, String headerValue, String defaultValue) {
        try {
            builder.header(headerName, headerValue);
        } catch (IllegalArgumentException e) {
            Ln.w(e, "Falling back to using a default value for the header: " + headerName);
            builder.header(headerName, defaultValue);
        }
    }

    protected Interceptor getHttp308RedirectInterceptor() {
        return new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                okhttp3.Response response = chain.proceed(request);
                if (response.code() == HTTP_PERMANENT_REDIRECT) {
                    request = request.newBuilder()
                            .url(response.header("Location"))
                            .build();

                    Ln.i("Handling 308 redirect, url = " + request.url());

                    response = chain.proceed(request);
                }

                return response;
            }
        };
    }

    protected Interceptor getTokenRefreshInterceptor() {
        return new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                okhttp3.Response response = chain.proceed(request);

                if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Ln.i("Got 401, Refreshing Token " + request.url());

                    Operation refreshTokenOperation = operationQueue.get().refreshToken("failed request");
                    if (refreshTokenOperation != null) {
                        refreshTokenOperation.waitForTerminalState(20000);
                        if (refreshTokenOperation.getState() == ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED) {
                            response.body().close();
                            response = chain.proceed(request);
                        }
                    }
                } else {
                    if (shouldRefreshTokensNow())
                        operationQueue.get().refreshToken("preemptive");
                }

                return response;
            }
        };
    }

    protected Interceptor getAuthInterceptor(final DeviceRegistration deviceRegistration) {
        return new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Request request = chain.request();
                okhttp3.Request.Builder builder = request.newBuilder();

                if (deviceRegistration != null && !deviceRegistration.sendAuthToHost(request.url())) {
                    ln.d("Not sending sensitive headers for url: %s", request.url());
                    for (String sensitiveHeader : SENSITIVE_HEADERS) {
                        builder.removeHeader(sensitiveHeader);
                    }
                } else {
                    // host gets auth
                    if (TextUtils.isEmpty(request.header(AUTHORIZATION_HEADER))) {
                        String token = getAuthHeader();
                        if (token != null)
                            builder.header(AUTHORIZATION_HEADER, token).build();
                    }
                }

                return chain.proceed(builder.build());
            }
        };
    }

    protected Interceptor getHAIntercepter(Uri url, DeviceRegistration deviceRegistration) {
        return new HAInterceptor(url, deviceRegistration == null ? null : deviceRegistration.getServiceHostMap());
    }

    protected Interceptor getResponseBodyLoggingInterceptor() {
        return new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Response response = chain.proceed(chain.request());
                if (Ln.isVerboseEnabled() && LoggingInterceptor.isLoggableContentType(response.header("Content-Type"))) {
                    String responseBody = LoggingInterceptor.getBodyLog(response.body());
                    if (!TextUtils.isEmpty(responseBody))
                        Ln.v("[HTTP] " + responseBody.getBytes().length + "-byte Response Body: " + responseBody);
                }
                return response;
            }
        };
    }

    protected Interceptor getPreAuthUserIdInterceptor() {
        return chain -> {
            Request.Builder requestBuilder = chain.request().newBuilder()
                    .addHeader("X-Prelogin-UserId", settings.getPreloginUserId());

            return chain.proceed(requestBuilder.build());
        };
    }

    protected Retrofit.Builder retrofitPrelogin(DeviceRegistration deviceRegistration) {
        OkHttpClient.Builder okHttpClientBuilder = okHttpClientNoInterceptors()
                .addNetworkInterceptor(getCommonHeadersInterceptor(deviceRegistration))
                .addNetworkInterceptor(getPreAuthUserIdInterceptor())
                .addNetworkInterceptor(getResponseBodyLoggingInterceptor());

        for (LoggingInterceptor interceptor : getLoggingInterceptors(getLogLevel())) {
            okHttpClientBuilder.addNetworkInterceptor(interceptor);
        }

        return retrofit(okHttpClientBuilder.build());
    }

    protected Retrofit.Builder retrofit(Uri baseUrl, DeviceRegistration deviceRegistration) {
        return retrofit(okHttpClient(getLogLevel(), baseUrl, deviceRegistration).build());
    }

    protected Retrofit.Builder retrofit(OkHttpClient okHttpClient) {
        clients.add(new WeakReference<>(okHttpClient));
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(DEFAULT_BASE_URL)
                .client(okHttpClient);
    }

    public OkHttpClient buildOkHttpClient(Uri baseUrl, DeviceRegistration deviceRegistration) {
        return buildOkHttpClient(baseUrl, deviceRegistration, new Dispatcher((ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR));
    }

    public OkHttpClient buildOkHttpClient(Uri baseUrl, DeviceRegistration deviceRegistration, Dispatcher dispatcher) {
        return okHttpClient(getLogLevel(), baseUrl, deviceRegistration, dispatcher).build();
    }

    protected OkHttpClient.Builder okHttpClient(LoggingInterceptor.Level logLevel, Uri baseUrl, DeviceRegistration deviceRegistration) {
        return okHttpClient(logLevel, baseUrl, deviceRegistration, new Dispatcher((ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR));
    }

    protected OkHttpClient.Builder okHttpClient(LoggingInterceptor.Level logLevel, Uri baseUrl, DeviceRegistration deviceRegistration, Dispatcher customDispatcher) {
        OkHttpClient.Builder ret = okHttpClientNoInterceptors()
                .addInterceptor(getCommonHeadersInterceptor(deviceRegistration))
                .addInterceptor(getHttp308RedirectInterceptor())
                .addInterceptor(getTokenRefreshInterceptor())
                .addInterceptor(getHAIntercepter(baseUrl, deviceRegistration))
                .addInterceptor(getAuthInterceptor(deviceRegistration));

        for (LoggingInterceptor interceptor : getLoggingInterceptors(logLevel)) {
            ret.addInterceptor(interceptor);
        }

        List<Interceptor> customInterceptors = getCustomInterceptors();
        if (customInterceptors != null) {
            for (Interceptor interceptor : customInterceptors) {
                ret.addInterceptor(interceptor);
            }
        }

        if (Ln.isVerboseEnabled())
            ret.addInterceptor(getResponseBodyLoggingInterceptor());

        if (customDispatcher != null) {
            ret.dispatcher(customDispatcher);
        }

        return ret;
    }

    protected OkHttpClient.Builder okHttpClientNoInterceptors() {
        OkHttpClient.Builder builder = okHttpClientBuilderProvider.get();
        builder
                .proxy(getProxy())
                .certificatePinner(certificatePinner.getCertificatePinner())
                .sslSocketFactory(buildSSLContext().getSocketFactory())
                .connectionPool(getConnectionPool())
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS);
        if (getProxyAuthenticator() != null) {
            builder.proxyAuthenticator(getProxyAuthenticator());
        }
        return builder;
    }

    protected List<LoggingInterceptor> getLoggingInterceptors(LoggingInterceptor.Level logLevel) {
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor(new LoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                ln.d("[HTTP] " + message);
            }
        });
        loggingInterceptor.setLevel(logLevel);
        return Collections.singletonList(loggingInterceptor);
    }

    ConnectionPool getConnectionPool() {
        if (connectionPool == null)
            connectionPool = new ConnectionPool(5, 30, TimeUnit.SECONDS);

        return connectionPool;
    }

    public SSLContext buildSSLContext() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(); // The system has no TLS. Just give up.
        }
        return sslContext;
    }

    public Socket buildSSLSocket() {
        try {
            return buildSSLContext().getSocketFactory().createSocket();
        } catch (IOException e) {
            ln.e(e);
            return null;
        }
    }

    @Nullable
    protected List<Interceptor> getCustomInterceptors() {
        return null;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(DiagnosticModeChangedEvent event) {
        verboseLoggingToken = event.getVerboseLoggingToken();
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
    List<WeakReference<OkHttpClient>> clients = new ArrayList<>();
    public boolean allClientsIdle() {
        for (WeakReference<OkHttpClient> clientRef : clients) {
            OkHttpClient client = clientRef.get();
            if (client != null) {
                if (client.dispatcher().runningCallsCount() + client.dispatcher().queuedCallsCount() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    protected Proxy getProxy() {
        return null;
    }

    protected Authenticator getProxyAuthenticator() {
        return null;
    }
}
