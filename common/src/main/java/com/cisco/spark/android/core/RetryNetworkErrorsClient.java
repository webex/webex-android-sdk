package com.cisco.spark.android.core;

import com.github.benoitdion.ln.Ln;
import retrofit.client.Client;
import retrofit.client.Request;
import retrofit.client.Response;

import java.io.EOFException;
import java.io.IOException;
import java.net.UnknownHostException;

public class RetryNetworkErrorsClient implements Client {
    private final Client delegate;

    public RetryNetworkErrorsClient(Client delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response execute(Request request) throws IOException {
        int retryIn = 2000;

        for (int i = 0; i < 5; i++) {
            try {
                return delegate.execute(request);
            } catch (UnknownHostException ex) {
                Ln.e(ex, "Trapped UnknownHostException, retrying");
            } catch (EOFException ex) {
                Ln.e(ex, "Trapped EOFException, retrying");
            }
            try {
                Thread.sleep(retryIn);
            } catch (InterruptedException e) {
            }
            retryIn *= 2;
        }
        return delegate.execute(request);
    }
}
