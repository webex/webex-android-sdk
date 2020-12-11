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
