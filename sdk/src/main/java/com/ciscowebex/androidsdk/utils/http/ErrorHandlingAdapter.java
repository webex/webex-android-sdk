/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.utils.http;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * Custom CallAdapter which adapts the built-in CallImpl to {@link ErrorHandlingCall}
 * whose callback {@link ErrorHandlingCallback} has more network error reports methods.
 */
public class ErrorHandlingAdapter {
	/**
	 * A callback which offers granular callbacks for various conditions.
	 */
	public interface ErrorHandlingCallback<T> {
		/**
		 * Called for [200, 300) responses.
		 */
		void success(Response<T> response);

		/**
		 * Called for 401 responses.
		 */
		void unauthenticated(Response<?> response);

		/**
		 * Called for [400, 500) responses, except 401.
		 */
		void clientError(Response<?> response);

		/**
		 * Called for [500, 600) response.
		 */
		void serverError(Response<?> response);

		/**
		 * Called for network errors while making the call.
		 */
		void networkError(IOException e);

		/**
		 * Called for unexpected errors while making the call.
		 */
		void unexpectedError(Throwable t);
	}

	public interface ErrorHandlingCall<T> {
		void cancel();

		void enqueue(ErrorHandlingCallback<T> callback);

		ErrorHandlingCall<T> clone();
	}

	public static class ErrorHandlingCallAdapterFactory extends CallAdapter.Factory {
		@Override
		public CallAdapter get(Type returnType, Annotation[] annotations,
		                                             Retrofit retrofit) {
			if (getRawType(returnType) != ErrorHandlingCall.class) {
				return null;
			}
			if (!(returnType instanceof ParameterizedType)) {
				throw new IllegalStateException(
					"ErrorHandlingCall must have generic type (e.g., ErrorHandlingCall<ResponseBody>)");
			}
			final Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
			final Executor callbackExecutor;
			Executor retrofitExecutor = retrofit.callbackExecutor();
			if (retrofitExecutor == null) {
				callbackExecutor = command -> command.run();
			} else {
				callbackExecutor = retrofitExecutor;
			}
			return new CallAdapter() {
				@Override
				public Type responseType() {
					return responseType;
				}

				@Override
				public Object adapt(Call call) {
					return new ErrorHandlingCallAdapter<>(call, callbackExecutor);
				}
			};
		}
	}

	/**
	 * Adapts a CallImpl to {@link ErrorHandlingCall}.
	 */
	static class ErrorHandlingCallAdapter<T> implements ErrorHandlingCall<T> {
		private final Call<T> call;
		private final Executor callbackExecutor;

		ErrorHandlingCallAdapter(Call<T> call, Executor callbackExecutor) {
			this.call = call;
			this.callbackExecutor = callbackExecutor;
		}

		@Override
		public void cancel() {
			call.cancel();
		}

		@Override
		public void enqueue(final ErrorHandlingCallback<T> callback) {
			// check callback not null
			if (callback == null) throw new NullPointerException("callback == null");

			call.enqueue(new Callback<T>() {
				@Override
				public void onResponse(Call<T> call, Response<T> response) {
					callbackExecutor.execute(() -> {
						int code = response.code();
						if (code >= 200 && code < 300) {
							callback.success(response);
						} else if (code == 401) {
							callback.unauthenticated(response);
						} else if (code >= 400 && code < 500) {
							callback.clientError(response);
						} else if (code >= 500 && code < 600) {
							callback.serverError(response);
						} else {
							callback.unexpectedError(new RuntimeException("Unexpected response " + response));
						}
					});
				}

				@Override
				public void onFailure(Call<T> call, Throwable t) {
					callbackExecutor.execute(() -> {
						if (t instanceof IOException) {
							callback.networkError((IOException) t);
						} else {
							callback.unexpectedError(t);
						}
					});
				}
			});
		}

		@Override
		public ErrorHandlingCall<T> clone() {
			return new ErrorHandlingCallAdapter<>(call.clone(), callbackExecutor);
		}
	}
}