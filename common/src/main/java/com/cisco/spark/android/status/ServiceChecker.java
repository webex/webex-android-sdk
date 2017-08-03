package com.cisco.spark.android.status;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.events.ServiceReachabilityEvent;
import com.cisco.spark.android.util.SchedulerProvider;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Response;
import rx.Observable;

import static com.github.benoitdion.ln.Ln.d;
import static com.github.benoitdion.ln.Ln.e;

public class ServiceChecker {

    private static final int NUM_SERVICES = ServiceName.values().length;

    private ApiClientProvider mApiClientProvider;
    private SchedulerProvider mSchedulerProvider;
    private EventBus mBus;

    private boolean mCheckInProgress;
    private Semaphore mServicesPinged;

    private final List<Service> mUnreachableServices;
    private final List<Service> mUnreachableServicesPingPending;

    private final Object mLock;

    public ServiceChecker(ApiClientProvider apiClientProvider, SchedulerProvider schedulerProvider, EventBus bus) {
        mApiClientProvider = apiClientProvider;
        mSchedulerProvider = schedulerProvider;
        mBus = bus;
        mUnreachableServicesPingPending = new ArrayList<>();
        mUnreachableServices = new ArrayList<>();
        mLock = new Object();
    }

    public void checkServices() {
        synchronized (mLock) {
            if (mCheckInProgress) {
                return;
            }
            mCheckInProgress = true;
        }
        synchronized (mUnreachableServicesPingPending) {
            mUnreachableServicesPingPending.clear();
        }

        mServicesPinged = new Semaphore(-NUM_SERVICES + 1);

        for (ServiceName s : ServiceName.values()) {
            ping(s);
        }

        Observable.just(1).subscribeOn(mSchedulerProvider.newThread()).subscribe(t-> {
            try {
                // wait for all the ping requests to have completed
                boolean permitAquired = mServicesPinged.tryAcquire(1, 100, TimeUnit.SECONDS);
                // permitAcquired false means some ping never returned, so assume services are not responding
                synchronized (mUnreachableServicesPingPending) {
                    updateUnreachableServices(mUnreachableServicesPingPending);
                    if (!mUnreachableServicesPingPending.isEmpty() || !permitAquired) {
                        mBus.post(new ServiceReachabilityEvent(false));
                    } else {
                        mBus.post(new ServiceReachabilityEvent(true));
                    }
                }
            } catch (InterruptedException e) {
                e(e);
            }
            synchronized (mLock) {
                mCheckInProgress = false;
            }
        });
    }

    public List<Service> getUnreachableServices() {
        synchronized (mUnreachableServices) {
            return mUnreachableServices;
        }
    }

    public boolean isExperiencingServiceIssues() {
        synchronized (mUnreachableServices) {
            return !mUnreachableServices.isEmpty();
        }
    }

    private void updateUnreachableServices(List<Service> unreachableServices) {
        synchronized (mUnreachableServices) {
            mUnreachableServices.clear();
            mUnreachableServices.addAll(unreachableServices);
        }
    }

    private Observable<HealthCheckResponse> getPingClient(ServiceName serviceName) {
        switch (serviceName) {
            case WDM_SERVICE:
                return mApiClientProvider.getWdmClient().ping();
            case ENCRYPTION_SERVICE:
                return mApiClientProvider.getSecurityClient().ping();
            case SEARCH_SERVICE:
                return mApiClientProvider.getSearchClient().ping();
            case HECATE_SERVICE:
                return mApiClientProvider.getHecateClient().ping();
            case CALLIOPE_SERVICE:
                return mApiClientProvider.getCalliopeClient().ping();
            case CALENDAR_SERVICE:
                return mApiClientProvider.getCalendarServiceClient().ping();
            case ADMIN_SERVICE:
                return mApiClientProvider.getAdminClient().ping();
        }
        return null;
    }

    private Observable<Response<HealthCheckResponse>> getRetrofit2PingClient(ServiceName serviceName) {
        switch (serviceName) {
            case ACL_SERVICE:
                return mApiClientProvider.getAclClient().ping();
            case LYRA_SERVICE:
                return mApiClientProvider.getLyraClient().ping();
            case AVATAR_SERVICE:
                return mApiClientProvider.getAvatarClient().ping();
            case CONVERSATION_SERVICE:
                return mApiClientProvider.getConversationClient().ping();
            case FEATURE_SERVICE:
                return mApiClientProvider.getFeatureClient().ping();
            case JANUS_SERVICE:
                return mApiClientProvider.getJanusClient().ping();
            case METRICS_SERVICE:
                return mApiClientProvider.getMetricsClient().ping();
            case WHITEBOARD_PERSISTENT_SERVICE:
                return mApiClientProvider.getWhiteboardPersistenceClient().ping();
            case USER_SERVICE:
                return mApiClientProvider.getUserClient().ping();
            case LOCUS_SERVICE:
                return mApiClientProvider.getLocusClient().ping();
            case PRESENCE_SERVICE:
                return mApiClientProvider.getPresenceClient().ping();
        }
        return null;
    }

    private boolean isRetrofit2Client(ServiceName service) {
        switch (service) {
            case ACL_SERVICE:
            case LYRA_SERVICE:
            case AVATAR_SERVICE:
            case CONVERSATION_SERVICE:
            case FEATURE_SERVICE:
            case JANUS_SERVICE:
            case METRICS_SERVICE:
            case WHITEBOARD_PERSISTENT_SERVICE:
            case USER_SERVICE:
            case LOCUS_SERVICE:
            case PRESENCE_SERVICE:
                return true;
            case WDM_SERVICE:
            case ENCRYPTION_SERVICE:
            case SEARCH_SERVICE:
            case HECATE_SERVICE:
            case CALLIOPE_SERVICE:
            case CALENDAR_SERVICE:
            case ADMIN_SERVICE:
                return false;
        }
        Ln.e("ServiceChecker: service %s unknown.", service);
        return false;
    }

    private void healthCheck(HealthCheckResponse response) {
        Service s = new Service(response.getServiceName());
        if (HealthCheckResponse.ServiceState.FAULT.equals(response.getServiceState())) {
            d("ServiceChecker: service %s in a faulty state.", response.getServiceName());
            s.setState(HealthCheckResponse.ServiceState.FAULT);
        }
        if (response.getUpstreamServices() != null) {
            checkUpstreamServices(response.getUpstreamServices(), s);
        }
        if (s.getState() != HealthCheckResponse.ServiceState.ONLINE || !s.getFaultyUpstreamServices().isEmpty()) {
            synchronized (mUnreachableServicesPingPending) {
                mUnreachableServicesPingPending.add(s);
            }
        }
    }

    private void checkUpstreamServices(List<UpstreamService> upstreamServices, Service service) {
        for (UpstreamService us : upstreamServices) {
            if (HealthCheckResponse.ServiceState.FAULT.equals(us.getServiceState())) {
                d("ServiceChecker: upstreamService %s in a faulty state.", us.getServiceName());
                service.addFaultyUpstreamServices(us.getServiceName());
            }
        }
    }

    private void pingRetrofit2(ServiceName serviceName) {
        Observable<Response<HealthCheckResponse>> observable = getRetrofit2PingClient(serviceName);

        if (observable == null) {
            Ln.e("ServiceChecker: No health check registered for %s", serviceName.name());
            return;
        }
        observable.subscribeOn(mSchedulerProvider.newThread())
                .timeout(10, TimeUnit.SECONDS)
                .subscribe(response -> {
                    if (response.code() != 200) {
                        d("ServiceChecker: %s can't be reached at the moment, response is %d", serviceName.name(), response.code());
                        synchronized (mUnreachableServicesPingPending) {
                            mUnreachableServicesPingPending.add(new Service(serviceName.name(), HealthCheckResponse.ServiceState.UNREACHABLE));
                        }
                    } else if (response.body() != null) {
                        healthCheck(response.body());
                    }
                    mServicesPinged.release();
                }, throwable -> {
                    d("ServiceChecker: %s can't be reached at the moment%s", serviceName.name(), (throwable.getMessage() != null ? (", failed with error " + throwable.getMessage()) : "."));
                    synchronized (mUnreachableServicesPingPending) {
                        mUnreachableServicesPingPending.add(new Service(serviceName.name(), HealthCheckResponse.ServiceState.UNREACHABLE));
                    }
                    mServicesPinged.release();
                    });
    }

    private void ping(ServiceName serviceName) {

        if (isRetrofit2Client(serviceName)) {
            pingRetrofit2(serviceName);
            return;
        }

        Observable<HealthCheckResponse> observable = getPingClient(serviceName);

        if (observable == null) {
            Ln.e("ServiceChecker: No health check registered for %s", serviceName.name());
            return;
        }

        observable.subscribeOn(mSchedulerProvider.newThread())
                .timeout(10, TimeUnit.SECONDS)
                .subscribe(response -> {
                    if (response != null) {
                        healthCheck(response);
                    }
                    mServicesPinged.release();
                }, throwable -> {
                    d("ServiceChecker: %s can't be reached at the moment.", serviceName.name());
                    synchronized (mUnreachableServicesPingPending) {
                        mUnreachableServicesPingPending.add(new Service(serviceName.name(), HealthCheckResponse.ServiceState.UNREACHABLE));
                    }
                    mServicesPinged.release();
                });
    }

    public enum ServiceName {
        ACL_SERVICE,
        LYRA_SERVICE,
        AVATAR_SERVICE,
        ADMIN_SERVICE,
        CALENDAR_SERVICE,
        CALLIOPE_SERVICE,
        CONVERSATION_SERVICE,
        FEATURE_SERVICE,
        HECATE_SERVICE,
        JANUS_SERVICE,
        LOCUS_SERVICE,
        METRICS_SERVICE,
        SEARCH_SERVICE,
        WHITEBOARD_PERSISTENT_SERVICE,
        USER_SERVICE,
        WDM_SERVICE,
        ENCRYPTION_SERVICE,
        PRESENCE_SERVICE,
    }

    public static class Service {
        private HealthCheckResponse.ServiceState state = HealthCheckResponse.ServiceState.ONLINE;
        private String name;
        private List<String> upstreamServices = new ArrayList<>();


        public Service(String name) {
            this.name = name;
        }

        public Service(String name, HealthCheckResponse.ServiceState state) {
            this.name = name;
            this.state = state;
        }

        public void setName(ServiceName name) {
            this.name = name.name();
        }

        public HealthCheckResponse.ServiceState getState() {
            return state;
        }

        public void setState(HealthCheckResponse.ServiceState status) {
            this.state = status;
        }

        public List<String> getFaultyUpstreamServices() {
            return upstreamServices;
        }

        public void addFaultyUpstreamServices(String upstreamService) {
            this.upstreamServices.add(upstreamService);
        }

    }
}
