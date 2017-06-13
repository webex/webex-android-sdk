package com.cisco.spark.android.core;

import android.content.Context;
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
import com.jakewharton.retrofit.Ok3Client;

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
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class BaseApiClientProvider {
    // used as a placeholder for clients that use absolute @Url parameters
    protected static final String DEFAULT_BASE_URL = "https://cisco.com/";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String TRACKING_ID_HEADER = "TrackingID";
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
                        .header("User-Agent", userAgentProvider.get())
                        .header(TRACKING_ID_HEADER, trackingIdGenerator.nextTrackingId());

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

                if (deviceRegistration != null && !deviceRegistration.sendAuthToHost(request.url().host())) {
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
        return new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Request.Builder requestBuilder = chain.request().newBuilder()
                        .addHeader("X-Prelogin-UserId", settings.getPreloginUserId());

                return chain.proceed(requestBuilder.build());
            }
        };
    }

    protected RestAdapter buildStandardRestAdapter(final String url, DeviceRegistration deviceRegistration) {
        return buildStandardRestAdapter(url, okHttpClient(getLogLevel(), deviceRegistration).build());
    }

    protected RestAdapter buildStandardRestAdapter(final String url, OkHttpClient client) {
        return buildStandardRestAdapter(url, client, gson);
    }

    protected RestAdapter buildStandardRestAdapter(final String url, OkHttpClient client, Gson gson) {
        return createRestAdapterBuilder()
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError cause) {
                        if (cause.getKind() == RetrofitError.Kind.NETWORK) {
                            ln.d(cause);
                        } else {
                            Response response = cause.getResponse();
                            if (response != null && response.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                ln.w(false, cause, "Received %d - %s", response.getStatus(), response.getReason());
                            } else if (response != null && response.getStatus() == HttpURLConnection.HTTP_MOVED_TEMP) {
                                ln.d("Received 307 - Temporary redirect");
                            } else if (response != null && response.getStatus() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                                ln.d("Received 304 - Not Modified");
                            } else {
                                ln.w(false, cause);
                            }
                        }
                        return cause;
                    }
                })
                .setClient(new Ok3Client(client))
                .setConverter(new GsonConverter(gson))
                .setEndpoint(url)
                .setExecutors(
                        Executors.newCachedThreadPool(getThreadFactory("Async HTTP")),
                        Executors.newCachedThreadPool(getThreadFactory("Async HTTP Callback")))
                .build();
    }

    protected Retrofit.Builder retrofitPrelogin(DeviceRegistration deviceRegistration) {
        OkHttpClient.Builder okHttpClientBuilder = okHttpClientNoInterceptors()
                .addNetworkInterceptor(getCommonHeadersInterceptor(deviceRegistration))
                .addNetworkInterceptor(getPreAuthUserIdInterceptor())
                .addNetworkInterceptor(getResponseBodyLoggingInterceptor())
                .addNetworkInterceptor(getLoggingInterceptor(getLogLevel()));

        return retrofit(okHttpClientBuilder.build());
    }

    protected Retrofit.Builder retrofit(DeviceRegistration deviceRegistration) {
        return retrofit(okHttpClient(getLogLevel(), deviceRegistration).build());
    }

    protected Retrofit.Builder retrofit(OkHttpClient okHttpClient) {
        clients.add(new WeakReference<>(okHttpClient));
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(DEFAULT_BASE_URL)
                .client(okHttpClient);
    }

    protected OkHttpClient buildOkHttpClient(DeviceRegistration deviceRegistration) {
        return okHttpClient(getLogLevel(), deviceRegistration).build();
    }

    protected OkHttpClient.Builder okHttpClient(LoggingInterceptor.Level logLevel, DeviceRegistration deviceRegistration) {
        OkHttpClient.Builder ret = okHttpClientNoInterceptors()
                .addInterceptor(getCommonHeadersInterceptor(deviceRegistration))
                .addInterceptor(getHttp308RedirectInterceptor())
                .addInterceptor(getTokenRefreshInterceptor())
                .addNetworkInterceptor(getAuthInterceptor(deviceRegistration))
                .addNetworkInterceptor(getLoggingInterceptor(logLevel));

        List<Interceptor> customInterceptors = getCustomInterceptors();
        if (customInterceptors != null) {
            for (Interceptor interceptor : customInterceptors) {
                ret.addInterceptor(interceptor);
            }
        }

        if (Ln.isVerboseEnabled())
            ret.addInterceptor(getResponseBodyLoggingInterceptor());

        ret.dispatcher(new Dispatcher((ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR));

        return ret;
    }

    protected OkHttpClient.Builder okHttpClientNoInterceptors() {
        OkHttpClient.Builder builder = okHttpClientBuilderProvider.get();
        return builder
                .proxy(getProxy())
                .certificatePinner(certificatePinner.getCertificatePinner())
                .sslSocketFactory(buildSSLContext().getSocketFactory())
                .connectionPool(getConnectionPool())
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS);
    }

    protected LoggingInterceptor getLoggingInterceptor(LoggingInterceptor.Level logLevel) {
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor(new LoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                ln.d("[HTTP] " + message);
            }
        });
        loggingInterceptor.setLevel(logLevel);
        return loggingInterceptor;
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

    protected RestAdapter.Builder createRestAdapterBuilder() {
        return new RestAdapter.Builder();
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
}
