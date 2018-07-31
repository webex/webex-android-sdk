package com.ciscowebex.androidsdk.utils.http;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by qimdeng on 4/4/18.
 */

public class ListenerCallback<T> implements Callback<T> {

	private boolean hasHandleUnauthError = false; // only handle unauth error once

	private ServiceBuilder.UnauthErrorListener _listener;
	public void setUnauthErrorListener(ServiceBuilder.UnauthErrorListener listener){
		_listener = listener;
	}

	@Override
	public void onResponse(Call<T> call, Response<T> response) {

	}

	@Override
	public void onFailure(Call<T> call, Throwable t) {

	}

	protected boolean checkUnauthError(Response response){
		if (response.code() == 401 && !hasHandleUnauthError && _listener != null) {
			hasHandleUnauthError = true;
			_listener.onUnauthError(response);
			return true;
		}
		return false;
	}
}