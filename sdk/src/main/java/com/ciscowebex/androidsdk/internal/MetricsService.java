package com.ciscowebex.androidsdk.internal;

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.collection.Maps;

import java.util.List;
import java.util.Map;

public class MetricsService {

    private Authenticator authenticator;

    public MetricsService(@NonNull Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void post(Device device, List<Map<String, String>> metrics) {
        Service.Metrics.homed(device).post(Maps.makeMap("metrics", metrics)).to("metrics")
                .auth(authenticator)
                .queue(Queue.main)
                .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                .async((Closure<Void>) data -> Ln.e("" + data));
    }

}
