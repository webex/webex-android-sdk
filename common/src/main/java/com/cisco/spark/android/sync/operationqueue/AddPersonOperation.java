package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
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
                Ln.i("Failed getting uuid for added user. Adding by email address instead.");
            }
        }
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        KmsResourceObject kro = getKmsResourceObject();

        try {
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
                    GenericMetric metric = new GenericMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM);
                    metric.addNetworkFields(response);
                    operationQueue.postGenericMetric(metric);
                }

                return SyncState.READY;
            }

            person = (Person) response.body().getObject();

            if (isTeam) {
                GenericMetric metric = new GenericMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM);
                metric.addNetworkFields(response);
                operationQueue.postGenericMetric(metric);
            }

            return SyncState.SUCCEEDED;

        } catch (RetrofitError error) {
            if (error.getResponse() != null) {
                if (error.getResponse().getStatus() == 403 || error.getResponse().getStatus() == 400) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        FileUtils.streamCopy(error.getResponse().getBody().in(), os);
                    } catch (IOException e) {
                        Ln.w(e, "Failed reading response");
                        throw error;
                    }
                    String body = new String(os.toByteArray());
                    ErrorDetail errorDetail = gson.fromJson(body, ErrorDetail.class);
                    ErrorDetail.CustomErrorCode errorCode = ErrorDetail.CustomErrorCode.fromErrorCode(errorDetail.getErrorCode());

                    if (isTeam) {
                        GenericMetric metric = new GenericMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM);
                        metric.addNetworkFields(errorDetail);
                    }

                    switch (errorCode) {
                        case SideboardingExistingParticipant:
                            setErrorMessage("Add Participant - user already member of conversation");
                            return SyncState.FAULTED;
                        case SideboardingFailed:
                            setErrorMessage("Add Participant - Participant Not Found");
                            return SyncState.FAULTED;
                        case KmsMessageOperationFailed:
                            String message = CryptoUtils.getKmsErrorMessage(error, gson, keyManager.getSharedKeyAsJWK());
                            Ln.w(message);
                            setErrorMessage(message);
                            return SyncState.FAULTED;
                        default:
                            throw error;
                    }
                }
            } else {
                throw error;
            }
        } catch (Exception e) {
            Ln.w(e, "Failed adding user to room");
        }
        return SyncState.READY;
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
            GenericMetric metric = new GenericMetric(ClientMetricNames.ONBOARDING_INVITED_PEOPLE_TO_TEAM);
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
}
