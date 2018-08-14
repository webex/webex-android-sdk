package com.ciscowebex.androidsdk.utils.http;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.benoitdion.ln.Ln;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Retries calls for 429 request error.
 */
public class RetryCallAdapterFactory extends CallAdapter.Factory {
	private final ScheduledExecutorService mExecutor;

	private RetryCallAdapterFactory() {
		mExecutor = Executors.newScheduledThreadPool(1);
	}

	public static RetryCallAdapterFactory create() {
		return new RetryCallAdapterFactory();
	}

	@Override
	public CallAdapter get(final Type returnType, Annotation[] annotations, Retrofit retrofit) {
		final CallAdapter delegate = retrofit.nextCallAdapter(this, returnType, annotations);
		return new CallAdapter() {
			@Override
			public Type responseType() {
				return delegate.responseType();
			}

			@Override
			public Object adapt(Call call) {
				return delegate.adapt(new RetryingCall<>(call, mExecutor, Integer.MAX_VALUE));
			}
		};
	}

	static final class RetryingCall<T> implements Call<T> {
		private final Call<T> mDelegate;
		private final ScheduledExecutorService mExecutor;
		private final int mMaxRetries;

		public RetryingCall(Call<T> delegate, ScheduledExecutorService executor, int maxRetries) {
			mDelegate = delegate;
			mExecutor = executor;
			mMaxRetries = maxRetries;
		}

		@Override
		public Response<T> execute() throws IOException {
			return mDelegate.execute();
		}

		@Override
		public void enqueue(Callback<T> callback) {
			mDelegate.enqueue(new RetryingCallback<>(mDelegate, callback, mExecutor, mMaxRetries));
		}

		@Override
		public boolean isExecuted() {
			return false;
		}

		@Override
		public void cancel() {
			mDelegate.cancel();
		}

		@Override
		public boolean isCanceled() {
			return false;
		}

		@SuppressWarnings("CloneDoesntCallSuperClone" /* Performing deep clone */)
		@Override
		public Call<T> clone() {
			return new RetryingCall<>(mDelegate.clone(), mExecutor, mMaxRetries);
		}

		@Override
		public Request request() {
			return null;
		}
	}

	// Exponential backoff approach from https://developers.google.com/drive/web/handle-errors
	static final class RetryingCallback<T> implements Callback<T> {
		private static final int DEFAULT_RETRY_AFTER = 60;
		private static final int MAX_RETRY_AFTER = 3600;
		private final int mMaxRetries;
		private final Call<T> mCall;
		private final Callback<T> mDelegate;
		private final ScheduledExecutorService mExecutor;
		private final int mRetries;

		RetryingCallback(Call<T> call, Callback<T> delegate, ScheduledExecutorService executor, int maxRetries) {
			this(call, delegate, executor, maxRetries, 0);
		}

		RetryingCallback(Call<T> call, Callback<T> delegate, ScheduledExecutorService executor, int maxRetries, int retries) {
			mCall = call;
			mDelegate = delegate;
			mExecutor = executor;
			mMaxRetries = maxRetries;
			mRetries = retries;
		}

		private void retryCall(int interval) {
			mExecutor.schedule(() -> {
                Ln.d("retryCall: " + (mRetries + 1));
                final Call<T> call = mCall.clone();
                call.enqueue(new RetryingCallback<>(call, mDelegate, mExecutor, mMaxRetries, mRetries + 1));
            }, interval, TimeUnit.SECONDS);
		}

		private int get429RetryAfterSeconds(final Response<T> response) {
			if (response == null || response.code() != 429) {
				return 0;
			}

			final String retryAfterHeader = response.headers().get("Retry-After");
			if (retryAfterHeader == null) {
				return DEFAULT_RETRY_AFTER;
			}

			final int retrySeconds;
			try {
				retrySeconds = Integer.parseInt(retryAfterHeader);
			} catch (final NumberFormatException e) {
				Ln.w("Failed parsing Retry-After header");
				return DEFAULT_RETRY_AFTER;
			}

			if (retrySeconds <= 0) {
				return DEFAULT_RETRY_AFTER;
			}

			return Math.min(retrySeconds, MAX_RETRY_AFTER);
		}

		@Override
		public void onResponse(Call<T> call, Response<T> response) {
			// Retry 429 request
			int interval = get429RetryAfterSeconds(response);
			Ln.d("onResponse: " + response.code() + "  retry interval: " + interval + "  retries: " + mRetries);
			if (interval > 0 && mRetries < mMaxRetries) {
				retryCall(interval);
			} else {
				mDelegate.onResponse(call, response);
			}
		}

		@Override
		public void onFailure(Call<T> call, Throwable t) {
			mDelegate.onFailure(call, t);
		}
	}
}