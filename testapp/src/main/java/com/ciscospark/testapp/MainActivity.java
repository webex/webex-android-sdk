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
import android.widget.Toast;

import com.ciscospark.CompletionHandler;
import com.ciscospark.Spark;
import com.ciscospark.SparkError;
import com.ciscospark.auth.Authenticator;
import com.ciscospark.auth.JWTAuthenticator;
import com.ciscospark.phone.Phone;
import com.ciscospark.phone.RegisterListener;
import com.github.benoitdion.ln.Ln;

public class MainActivity extends Activity implements CompletionHandler<String> {

    static final String TAG = MainActivity.class.getName();

    Button btn;
    WebView webView;
    String clientId = "Cc580d5219555f0df8b03d99f3e020381eae4eee0bad1501ad187480db311cce4";
    String clientSec = "c87879c646f82b6d23a7a4c2f6eea1894234a53e013777e90bced91f22225317";
    String redirect = "AndroidDemoApp://response";
    String scope = "spark:all spark:kms";
    String email = "";
    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsIm5hbWUiOiJ1c2VyICMxIiwiaXNzIjoiY2Q1YzlhZjctOGVkMy00ZTE1LTk3MDUtMDI1ZWYzMGIxYjZhIn0.nQTlT_WwkHdWZTCNi4tVl2IA476nAWo34oxtuTlLSDk";
    //OAuthWebViewAuthenticator authenticator;
    Authenticator authenticator;
    Spark mSpark;
    Phone mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webview);

        //authenticator = new OAuthWebViewAuthenticator(clientId, clientSec, redirect, scope, email, webView);
        authenticator = new JWTAuthenticator(auth_token);
        mSpark = new Spark();
        mSpark.setAuthenticator(authenticator);

        //webView.loadUrl("https://developer.ciscospark.com/");
        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn.setVisibility(View.INVISIBLE);
                mSpark.authorize(MainActivity.this);
                //authenticator.authorize(MainActivity.this);
            }
        });
    }

    @Override
    public void onComplete(String auth_token) {
        Log.d(TAG, "success: " + auth_token);
        String html = "<html><body><b>Access token:</b> " + auth_token + "</body></html>";
        webView.loadData(html, "text/html", null);
        mPhone = mSpark.phone();
        mPhone.register(new RegisterListener() {
            @Override
            public void onSuccess() {
                Ln.d("register success");
                btn.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "register success", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailed(SparkError error) {
                Ln.d("register failed", error.toString());
                btn.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "register failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onError(SparkError error) {
        btn.setVisibility(View.VISIBLE);
        Log.d(TAG, "failed " + error.toString());
    }
}
