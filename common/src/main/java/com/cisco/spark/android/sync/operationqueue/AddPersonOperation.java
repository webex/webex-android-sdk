package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.SegmentService;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.value.ClientMetricNames;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncStateFailureReason;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.FileUtils;
import com.github.benoitdion.ln.Ln;
import com.segment.analytics.Properties;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class AddPersonOperation extends ActivityOperation {

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient EventBus bus;

    private Person person;
    private boolean isTeam;
    private boolean addedByEmail;

    public AddPersonOperation(Injector injector, String conversationId, Person person, boolean isTeam) {
        super(injector, conversationId);
        this.person = person;
        this.isTeam = isTeam;
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.add);
        activity.setObject(person);

        if (TextUtils.isEmpty(person.getUuid())) {
            try {
                User user = apiClientProvider.getUserClient().getUserID(apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), person.getEmail()).execute().body();
                person.setUuid(user.getUuid());
            } catch (Exception e) {
                addedByEmail = true;
                Ln.i("Failed getting uuid for added user. Adding by email address instead.");
            }
        }
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        KmsResourceObject kro = getKmsResourceObject();

        HashSet<String> email = new HashSet<>();
        email.add(person.getEmail());

        if (!keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            return SyncState.READY;
        }
        activity.setEncryptedKmsMessage(conversationProcessor.authorizeNewParticipantsUsingKmsMessagingApi(kro, person));

        Response<Activity> response = postActivity(activity);

        if (!response.isSuccessful()) {
            if (isTeam) {
                GenericMetric metric = GenericMetric.buildBehavioralMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM)
                        .withNetworkTraits(response);
                operationQueue.postGenericMetric(metric);
            }

            if (response.code() == 403 || response.code() == 400) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileUtils.streamCopy(response.errorBody().byteStream(), os);

                String body = new String(os.toByteArray());
                ErrorDetail errorDetail = gson.fromJson(body, ErrorDetail.class);
                ErrorDetail.CustomErrorCode errorCode = errorDetail.extractCustomErrorCode();

                if (isTeam) {
                    GenericMetric metric = GenericMetric.buildBehavioralMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM)
                            .withNetworkTraits(errorDetail);
                    operationQueue.postGenericMetric(metric);
                }

                switch (errorCode) {
                    case SideboardingExistingParticipant:
                        setErrorMessage("Add Participant - user already member of conversation");
                        break;
                    case SideboardingFailed:
                        setErrorMessage("Add Participant - Participant Not Found");
                        break;
                    case KmsMessageOperationFailed:
                        String message = CryptoUtils.getKmsErrorMessage(response, gson, keyManager.getSharedKeyAsJWK());
                        Ln.w(message);
                        setErrorMessage(message);
                        break;
                    case NewUserInDirSycnedOrg:
                    default:
                        setErrorMessage(errorDetail.getMessage());
                        Ln.w(errorDetail.getMessage());
                        break;
                }
                return SyncState.FAULTED;
            }

            return SyncState.READY;
        }

        person = (Person) response.body().getObject();

        if (isTeam) {
            GenericMetric metric = GenericMetric.buildBehavioralMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM)
                    .withNetworkTraits(response);
            operationQueue.postGenericMetric(metric);
        }

        postSegmentMetrics(response);

        return SyncState.SUCCEEDED;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.ADD_PARTICIPANT;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.ADD_PARTICIPANT)
            return false;

        AddPersonOperation newOp = (AddPersonOperation) newOperation;

        return TextUtils.equals(conversationId, newOp.conversationId)
                && newOp.person.equals(person);
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);
        if (getState().isTerminal())
            bus.post(new AddPersonOperationCompletedEvent(this));

        if (getState() == SyncState.FAULTED && getFailureReason() == SyncStateFailureReason.DEPENDENCY) {
            GenericMetric metric = GenericMetric.buildBehavioralMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM);
            operationQueue.postGenericMetric(metric);
        }
    }

    public Person getPerson() {
        return person;
    }

    protected KmsResourceObject getKmsResourceObject() {
        return ConversationContentProviderQueries.getKmsResourceObject(
                getContentResolver(), conversationId);
    }

    public static class AddPersonOperationCompletedEvent {
        public AddPersonOperation operation;

        private AddPersonOperationCompletedEvent(AddPersonOperation op) {
            this.operation = op;
        }
    }


    private void postSegmentMetrics(Response response) {
        String teamPrimaryConvId = ConversationContentProviderQueries.getTeamPrimaryConversationId(getContentResolver(), getConversationId());
        boolean isTeamConv = getConversationId().equals(teamPrimaryConvId);

        Properties segmentProperties = new SegmentService.PropertiesBuilder()
                .setNetworkResponse(response)
                .setSpaceIsTeam(!TextUtils.isEmpty(teamPrimaryConvId))
                .build();

        if (addedByEmail) {
            segmentService.reportMetric(SegmentService.INVITED_NEW_USER_EVENT, segmentProperties);
        } else {
            if (isTeamConv) {
                segmentService.reportMetric(SegmentService.ADDED_USER_TO_TEAM_EVENT, segmentProperties);
            } else {
                segmentService.reportMetric(SegmentService.ADDED_USER_TO_SPACE_EVENT, segmentProperties);
            }
        }
    }

}
