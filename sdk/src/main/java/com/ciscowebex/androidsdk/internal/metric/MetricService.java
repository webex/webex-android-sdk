/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
                GenericMetricsModel models = new GenericMetricsModel(metrics.popMetrics(100));
                Service.Metrics.homed(phone.getDevice()).post(models).to("clientmetrics")
                        .auth(phone.getAuthenticator())
                        .queue(queue)
                        .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                        .async((Closure<Void>) data -> Ln.d("" + data));
            }
        });
    }

    public void post(Map<String, String> metric) {
        queue.run(() -> Service.Metrics.homed(phone.getDevice()).post(Maps.makeMap("metrics", Collections.singletonList(metric))).to("metrics")
                .auth(phone.getAuthenticator())
                .queue(queue)
                .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                .async((Closure<Void>) data -> Ln.e("" + data)));

    }

    public void post(List<Map<String, String>> metrics) {
        queue.run(() -> Service.Metrics.homed(phone.getDevice()).post(Maps.makeMap("metrics", metrics)).to("metrics")
                .auth(phone.getAuthenticator())
                .queue(queue)
                .error((CompletionHandler<Void>) result -> Ln.e("" + result.getError()))
                .async((Closure<Void>) data -> Ln.e("" + data)));

    }
}
