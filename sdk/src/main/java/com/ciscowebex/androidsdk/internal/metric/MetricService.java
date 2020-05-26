package com.ciscowebex.androidsdk.internal.metric;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.model.GenericMetricModel;
import com.ciscowebex.androidsdk.internal.model.GenericMetricsModel;
import com.ciscowebex.androidsdk.internal.queue.BackgroundQueue;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.collection.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MetricService {

    private final Queue queue = new BackgroundQueue();

    private PhoneImpl phone;

    private GenericMetricsModel metrics = new GenericMetricsModel();

    public MetricService(PhoneImpl phone) {
        this.phone = phone;
    }

    public void post(GenericMetricModel metric) {
        metrics.addMetric(metric);
        queue.run(() -> {
            if (metrics.metricsSize() > 0) {
                Service.Metrics.post(new GenericMetricsModel(metrics.popMetrics(100))).to("clientmetrics")
                        .auth(phone.getAuthenticator())
                        .device(phone.getDevice())
                        .queue(queue)
                        .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                        .async((Closure<Void>) data -> Ln.d("" + data));
            }
        });
    }

    public void post(Map<String, String> metric) {
        queue.run(() -> Service.Metrics.post(Maps.makeMap("metrics", Collections.singletonList(metric))).to("metrics")
                .auth(phone.getAuthenticator())
                .device(phone.getDevice())
                .queue(queue)
                .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                .async((Closure<Void>) data -> Ln.e("" + data)));

    }

    public void post(List<Map<String, String>> metrics) {
        queue.run(() -> Service.Metrics.post(Maps.makeMap("metrics", metrics)).to("metrics")
                .auth(phone.getAuthenticator())
                .device(phone.getDevice())
                .queue(queue)
                .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                .async((Closure<Void>) data -> Ln.e("" + data)));

    }
}
