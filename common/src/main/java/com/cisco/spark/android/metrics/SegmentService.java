package com.cisco.spark.android.metrics;


import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.provisioning.model.UserActivationResponse;
import com.cisco.spark.android.util.MimeUtils;
import com.github.benoitdion.ln.Ln;
import com.segment.analytics.Analytics;
import com.segment.analytics.ConnectionFactory;
import com.segment.analytics.Middleware;
import com.segment.analytics.Properties;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

import static com.cisco.spark.android.core.BaseApiClientProvider.TRACKING_ID_HEADER;

public class SegmentService {
    public final static String PRODUCTION_WRITE_KEY = "TLoj5LC3y5H0mrCTnEeLAPblgKoRUD0T";
    public final static String TEST_WRITE_KEY = "80N9WgSoCNq8GLnyQdK1WlvoayODNktb";

    private final static int DEFAULT_FLUSH_INTERVAL_SECS = 15;

    // Event Types
    public static final String ONBOARDING_VIEWED_GET_STARTED = "Ob Viewed Get Started";
    public static final String ONBOARDING_VISITED_TOS = "Ob Visited Terms Of Service";
    public static final String ONBOARDING_VISITED_PRIVACY = "Ob Visited Privacy Statement";
    public static final String ONBOARDING_VISITED_NOTICES = "Ob Visited Notices & Disclaimers";
    public static final String ONBOARDING_VISITED_LEARN = "Ob Visited Learn More About Spark";
    public static final String ONBOARDING_VISITED_CONTACT_SUPPORT = " Ob Visited Contact Support";
    public static final String ONBOARDING_VIEWED_ENTER_EMAIL = " Ob Viewed Enter Email";
    public static final String ONBOARDING_ENTERED_EMAIL = " Ob Entered Email";
    public static final String ONBOARDING_VIEWED_CHECK_YOUR_EMAIL = " Ob Viewed Check Your Email";
    public static final String ONBOARDING_OPENED_EMAIL = " Ob Opened Native Email App";
    public static final String ONBOARDING_RESENT_EMAIL = " Ob Resent Email";
    public static final String ONBOARDING_VIEWED_CREATE_PASSWORD = " Ob Viewed Create Password";
    public static final String ONBOARDING_VIEWED_PASSWORD_VALIDATION = " Ob Viewed Password Validation";
    public static final String ONBOARDING_CREATED_PASSWORD = " Ob Created Password";
    public static final String ONBOARDING_VIEWED_CREATE_PROFILE = " Ob Viewed Create Profile";
    public static final String ONBOARDING_VIEWED_ENTER_NAME = " Ob Viewed Enter Name";
    public static final String ONBOARDING_UPLOADED_AVATAR = " Ob Uploaded Avatar";
    public static final String ONBOARDING_ENTERED_NAME = " Ob Entered Name";

    public final static String STARTED_APP_EVENT = "Started App";
    public final static String APPLICATION_BACKGROUNDED_EVENT = "Application Backgrounded";
    public final static String APPLICATION_FOREGROUNDED_EVENT = "Application Foregrounded";
    public final static String READ_MESSAGES_EVENT = "Read Messages";
    public final static String VIEWED_SPACE_EVENT = "Viewed Space";
    public final static String POSTED_MESSAGE_EVENT = "Posted Message";
    public final static String JOINED_CALL_EVENT = "Joined Call";
    public final static String CREATED_SPACE_EVENT = "Created Space";
    public final static String CREATED_TEAM_EVENT = "Created Team";
    public final static String ADDED_USER_TO_SPACE_EVENT = "Added User To Space";
    public final static String ADDED_USER_TO_TEAM_EVENT = "Added User To Team";
    public final static String INVITED_NEW_USER_EVENT = "Invited New User";


    // Event Properties
    private final static String NETWORK_WAS_SUCCESFUL_PROPERTY = "network.wasSuccessful";
    private final static String MESSAGE_WORD_COUNT_PROPERTY = "message.wordCount";
    private final static String MESSAGE_MENTION_COUNT_PROPERTY = "message.mentionCount";
    private final static String SPACE_IS_TEAM_PROPERTY = "space.isTeam";
    private final static String SPACE_MEMBER_COUNT_PROPERTY = "space.memberCount";
    private final static String SPACE_VIEW_SOURCE_PROPERTY = "source";
    private final static String TEAM_SPACE_COUNT_PROPERTY = "team.spaceCount";
    private final static String TEAM_MEMBER_COUNT_PROPERTY = "team.memberCount";
    private final static String CALL_SOURCE_PROPERTY = "source";
    private final static String REFERRER_URL_PROPERTY = "referrerUrl";


    private final String writeKey;
    private final Context context;
    private final OAuth2 oAuth2;
    private final ApiClientProvider apiClientProvider;
    private final TrackingIdGenerator trackingIdGenerator;
    private final UrlProvider urlProvider;
    private final Middleware middleware;

    private static Analytics analytics;


    public SegmentService(String writeKey, Context context, ApiClientProvider apiClientProvider, UrlProvider urlProvider, OAuth2 oAuth2,
                          TrackingIdGenerator trackingIdGenerator, Middleware middleware) {
        this.writeKey = writeKey;
        this.context = context;
        this.apiClientProvider = apiClientProvider;
        this.urlProvider = urlProvider;
        this.oAuth2 = oAuth2;
        this.trackingIdGenerator = trackingIdGenerator;
        this.middleware = middleware;
    }


    /**
     * Initialize Segment.  Note that requests to Segment are proxied through spark metrics service.
     */
    public void initialize(int flushIntervalSeconds) {
        Ln.i("SegmentService.initialize(), analytics = " + analytics);

        if (analytics != null) {
            return;
        }

        Analytics.Builder analyticsBuilder = new Analytics.Builder(context, writeKey)
                .trackApplicationLifecycleEvents()
                .recordScreenViews()
                .flushInterval(flushIntervalSeconds, TimeUnit.SECONDS)
                .connectionFactory(getSegmentProxyConnectionFactory());

        if (middleware != null) {
            analyticsBuilder.middleware(middleware);
        }

        analytics = analyticsBuilder.build();
    }

    public void initialize() {
        initialize(DEFAULT_FLUSH_INTERVAL_SECS);
    }

    public void identify(String userId) {
        if (analytics != null) {
            analytics.identify(userId);
        }
    }


    /**
     * Copy the segment authorization into the Proxy-Authorization field
     */
    protected void addSegmentProxyHeaders(HttpURLConnection connection) {
        connection.addRequestProperty("Segment-Auth", connection.getRequestProperty("Authorization"));
        connection.setRequestProperty(TRACKING_ID_HEADER, trackingIdGenerator.nextTrackingId());
    }


    protected ConnectionFactory getSegmentProxyConnectionFactory() {
        return segmentProxyConnectionFactory;
    }

    public void setSegmentProxyConnectionFactory(ConnectionFactory segmentProxyConnectionFactory) {
        this.segmentProxyConnectionFactory = segmentProxyConnectionFactory;
    }

    private ConnectionFactory segmentProxyConnectionFactory = new ConnectionFactory() {
        @Override
        protected HttpURLConnection openConnection(String url) throws IOException {
            String path = Uri.parse(url).getPath();
            String segmentMetricsUrl = urlProvider.getMetricsApiUrl() + "segmentmetrics" + path;
            Ln.d("SegmentService: openConnection, metricsUrl = " + segmentMetricsUrl);
            return super.openConnection(segmentMetricsUrl);
        }

        @Override
        public HttpURLConnection upload(String writeKey) throws IOException {
            Ln.d("SegmentService: upload");
            HttpURLConnection connection = super.upload(writeKey);
            addSegmentProxyHeaders(connection);
            return connection;
        }

        @Override
        public HttpURLConnection attribution(String writeKey) throws IOException {
            Ln.d("SegmentService: attribution");
            HttpURLConnection connection = super.attribution(writeKey);
            addSegmentProxyHeaders(connection);
            return connection;
        }
    };


    public void reportMetric(String eventName, Properties properties) {
        if (analytics != null) {
            analytics.track(eventName, properties);
        }
    }

    public void reportMetric(String eventName) {
        reportMetric(eventName, new Properties());
    }


    public static class PropertiesBuilder {
        private Properties segmentMetricProperties;

        public PropertiesBuilder() {
            segmentMetricProperties = new Properties();
        }

        public PropertiesBuilder setNetworkResponse(Response response) {
            boolean networkSuccessful = !(response == null || response.code() >= 400);
            segmentMetricProperties.putValue(NETWORK_WAS_SUCCESFUL_PROPERTY, networkSuccessful);
            return this;
        }

        public PropertiesBuilder setReferrerUrl(String referrerUrl) {
            segmentMetricProperties.putValue(REFERRER_URL_PROPERTY, referrerUrl);
            return this;
        }

        public PropertiesBuilder setSpaceMemberCount(int spaceMemberCount) {
            segmentMetricProperties.putValue(SPACE_MEMBER_COUNT_PROPERTY, spaceMemberCount);
            return this;
        }

        public PropertiesBuilder setSpaceIsTeam(boolean spaceIsTeam) {
            segmentMetricProperties.putValue(SPACE_IS_TEAM_PROPERTY, spaceIsTeam);
            return this;
        }

        public PropertiesBuilder setSpaceViewSource(String spaceViewSource) {
            segmentMetricProperties.putValue(SPACE_VIEW_SOURCE_PROPERTY, spaceViewSource);
            return this;
        }

        public PropertiesBuilder setTeamSpaceCount(int teamSpaceCount) {
            segmentMetricProperties.putValue(TEAM_SPACE_COUNT_PROPERTY, teamSpaceCount);
            return this;
        }

        public PropertiesBuilder setTeamMemberCount(int teamMemberCount) {
            segmentMetricProperties.putValue(TEAM_MEMBER_COUNT_PROPERTY, teamMemberCount);
            return this;
        }

        public PropertiesBuilder setMessageWordCount(int messageWordCount) {
            segmentMetricProperties.putValue(MESSAGE_WORD_COUNT_PROPERTY, messageWordCount);
            return this;
        }

        public PropertiesBuilder setMessageMentionCount(int messageMentionCount) {
            segmentMetricProperties.putValue(MESSAGE_MENTION_COUNT_PROPERTY, messageMentionCount);
            return this;
        }

        public PropertiesBuilder setCallSource(String callSource) {
            segmentMetricProperties.putValue(CALL_SOURCE_PROPERTY, callSource);
            return this;
        }

        public PropertiesBuilder setUserActivationProperties(UserActivationResponse response) {
            if (response != null) {
                segmentMetricProperties.putValue(ClientMetricTag.METRIC_TAG_USER_CREATED.getTagName(), response.isUserCreated());
                segmentMetricProperties.putValue(ClientMetricTag.METRIC_TAG_DIR_SYNC.getTagName(), response.isDirSync());
                segmentMetricProperties.putValue(ClientMetricTag.METRIC_TAG_SSO.getTagName(), response.isSSO());
                segmentMetricProperties.putValue(ClientMetricTag.METRIC_TAG_HAS_PASSWORD.getTagName(), response.hasPassword());
                segmentMetricProperties.putValue(ClientMetricTag.METRIC_TAG_VERIFICATION_EMAIL_TRIGGERED.getTagName(), response.isVerificationEmailTriggered());
            }
            return this;
        }

        public PropertiesBuilder setFileTraits(File file) {
            if (file != null) {
                segmentMetricProperties.putValue(ClientMetricField.METRIC_FIELD_MIME_TYPE.getFieldName(), MimeUtils.getMimeType(file.getAbsolutePath()));
                segmentMetricProperties.putValue(ClientMetricField.METRIC_FIELD_FILE_SIZE.getFieldName(), file.length());
            }
            return this;
        }


        public Properties build() {
            return segmentMetricProperties;
        }
    }

}
