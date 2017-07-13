/*
 * Copyright (c) 2016-2017 Cisco Systems Inc
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

package com.ciscospark.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.ciscospark.SparkError;
import com.ciscospark.auth.AuthorizeListener;
import com.ciscospark.auth.OAuthWebViewStrategy;

public class MainActivity extends Activity implements AuthorizeListener {

    static final String TAG = MainActivity.class.getName();

    Button btn;
    WebView webView;
    String clientId = "Cc580d5219555f0df8b03d99f3e020381eae4eee0bad1501ad187480db311cce4";
    String clientSec = "d4e9385b2e5828eef376077995080ea4aa42b5c92f1b6af8f3a59fc6a4e79f6a";
    String redirect = "AndroidDemoApp://response";
    String scope = "spark:all spark:kms";
    String email = "";
    OAuthWebViewStrategy strategy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webview);

        strategy = new OAuthWebViewStrategy(clientId, clientSec, redirect, scope, email, webView);

        webView.loadUrl("https://developer.ciscospark.com/");
        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn.setVisibility(View.INVISIBLE);
                strategy.authorize(MainActivity.this);
            }
        });
    }

    @Override
    public void onSuccess() {
        OAuth2AccessToken token = strategy.getToken();
        Log.d(TAG, "success: " + token.getAccessToken());
        String html = "<html><body><b>Access token:</b> " + token.getAccessToken() + "</body></html>";
        webView.loadData(html, "text/html", null);
        btn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFailed(SparkError<AuthError> E) {
        btn.setVisibility(View.VISIBLE);
        Log.d(TAG, "failed " + E.toString());
    }
}
