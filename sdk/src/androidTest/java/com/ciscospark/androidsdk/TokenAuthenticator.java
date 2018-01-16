package com.ciscospark.androidsdk;

import android.support.annotation.Nullable;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.SparkError;
import com.ciscospark.androidsdk.auth.Authenticator;

/**
 * Created with IntelliJ IDEA.
 * User: kt
 * Date: 2018/1/5
 * Time: 10:21
 */

public class TokenAuthenticator implements Authenticator {
	
	private String _token;
	
	public TokenAuthenticator(String token) {
		_token = token;
	}
	
	@Override
	public boolean isAuthorized() {
		return _token != null;
	}

	@Override
	public void deauthorize() {
		_token = null;
	}

	@Override
	public void getToken(CompletionHandler<String> handler) {
		handler.onComplete(new Result<String>() {
			@Override
			public boolean isSuccessful() {
				return true;
			}

			@Nullable
			@Override
			public SparkError getError() {
				return null;
			}

			@Nullable
			@Override
			public String getData() {
				return _token;
			}
		});
	}
}
