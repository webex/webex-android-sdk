package com.cisco.spark.android.util;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.mercury.MercuryClient;
import com.github.benoitdion.ln.Ln;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import rx.Observable;
import rx.Subscription;

public class DiagnosticManager {
    private EventBus bus;
    private String verboseLoggingToken, pin;
    private ApiClientProvider apiClientProvider;
    private Subscription timeoutSubscription;

    public DiagnosticManager(EventBus bus, ApiClientProvider apiClientProvider) {
        this.bus = bus;
        this.apiClientProvider = apiClientProvider;
    }

    private static String randomPin() {
        final String corpus = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < 4; i++) {
            int j = Math.abs(random.nextInt()) % corpus.length();
            sb.append(corpus.charAt(j));
        }
        return sb.toString();
    }

    /**
     * Turn on dynamic logging with the specified logging token, and a timeout at the
     * specified timestamp in the future.  Recommendation is 30 minutes.
     * see: http://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Dynamic-Logging
     * This client will then get verbose logging in logstash.
     * see: http://sqbu-github.cisco.com/WebExSquared/platform/blob/master/services/logstash.md
     */
    public void enableVerboseLogging() {
        final String localPin = randomPin();
        apiClientProvider.getConversationClient().getDynamicLoggingToken(localPin).enqueue(new retrofit2.Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, retrofit2.Response<HashMap<String, String>> response) {
                if (response.isSuccessful()) {
                    HashMap<String, String> loggingPayload = response.body();
                    String token = loggingPayload.get("logToken");
                    if (token != null) {
                        receivedToken(localPin, token);
                    }
                } else {
                    Ln.w("Failed enabling verbose logging: " + LoggingUtils.toString(response));
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Ln.w(t, "Failed enabling verbose logging");
            }
        });
    }

    public void disableVerboseLogging() {
        Subscription localSubscription = timeoutSubscription;
        if (localSubscription != null) localSubscription.unsubscribe();
        timeoutSubscription = null;
        pin = verboseLoggingToken = null;
        bus.post(new DiagnosticModeChangedEvent(pin, verboseLoggingToken));
        bus.post(new ResetEvent(MercuryClient.WebSocketStatusCodes.CLOSE_UNKNOWN));
        Ln.i("stopped diagnostic mode");
    }

    public void toggleVerboseLogging() {
        if (verboseLoggingToken != null) {
            disableVerboseLogging();
        } else {
            enableVerboseLogging();
        }
    }

    public String getVerboseLoggingToken() {
        return verboseLoggingToken;
    }

    public String getPin() {
        return pin;
    }

    private void receivedToken(final String localPin, final String logToken) {
        this.verboseLoggingToken = logToken;
        this.pin = localPin;
        bus.post(new DiagnosticModeChangedEvent(pin, verboseLoggingToken));
        bus.post(new ResetEvent(MercuryClient.WebSocketStatusCodes.CLOSE_UNKNOWN)); // Restart Mercury and other components after verbose logging is enabled.
        Ln.i("started diagnostic mode with PIN: " + localPin);
        // start a timer to turn off verbose logging for this client in 30 minutes
        timeoutSubscription = Observable.timer(30, TimeUnit.MINUTES).subscribe(new ObserverAdapter<Long>() {
            @Override
            public void onCompleted() {
                disableVerboseLogging();
            }
        });
    }
}
