# Cisco Spark Android SDK

[![license](https://img.shields.io/github/license/ciscospark/spark-android-sdk.svg)](https://github.com/ciscospark/spark-android-sdk/blob/master/LICENSE)

The Cisco Spark Android SDK makes it easy to integrate secure and convenient Cisco Spark messaging and calling features in your Android apps.

This SDK is built with **Android SDK Tools 25** and requires **Android API Level 21** or later.

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [License](#license)

## Install

Assuming you already have an Android project, e.g. _MySparkApp_, for your android app, here are the steps to integrate the Spark Android SDK into your project using [Gradle](https://gradle.org):

1. Add the following repository to your Top-level build.gradle file

    ```
    allprojects {
        repositories {
            mavenCentral()
        }
    }
    ```

2. Add spark-android-sdk library in your App build.gradle dependency

    ```
    dependencies { compile 'com.ciscospark:androidsdk:0.1.0' }
    ```

3. Enable multiDex in your App

    ```
    android {
        defaultConfig {
            multiDexEnabled true
        }
    }
    ```

## Usage

To use the SDK, you will need Cisco Spark integration credentials. If you do not already have a Cisco Spark account, visit [Spark for Developers](https://developer.ciscospark.com/) to create your account and [register your integration](https://developer.ciscospark.com/authentication.html#registering-your-integration). Your app will need to authenticate users via an [OAuth](https://oauth.net/) grant flow for existing Cisco Spark users or a [JSON Web Token](https://jwt.io/) for guest users without a Cisco Spark account.

See the [Android SDK area](https://developer.ciscospark.com/sdk-for-android.html) of the Spark for Developers site for more information about this SDK.

### Example

Here are some examples of how to use the Android SDK in your app.

1. Create the Spark instance using Spark ID authentication ([OAuth](https://oauth.net/)-based):

    ```java
    String clientId = "YOUR_CLIENT_ID";
    String clientSecret = "YOUR_CLIENT_SECRET";
    String scope = "spark:all";
    String redirectUri = "Sparkdemoapp://response";

    OAuthWebViewAuthenticator authenticator = new OAuthWebViewAuthenticator(clientId, clientSecret, scope, redirectUri);
    Spark spark = new Spark(activity.getApplication(), authenticator)
    if (!authenticator.isAuthorized()) {
        authenticator.authorize(webView, new CompletionHandler<Void>() {
            @Override
            public void onComplete(Result<Void> result) {
                if (!result.isSuccessful()) {
                    System.out.println("User not authorized");
                }
            }
        });
    }
    ```

2. Create the Spark instance with Guest ID authentication ([JWT](https://jwt.io/)-based):

    ```java
    JWTAuthenticator authenticator = new JWTAuthenticator();
    Spark spark = new Spark(activity.getApplication(), authenticator);
    if (!authenticator.isAuthorized()) {
        authenticator.authorize(myJwt);
    }
    ```

3. Register the device to send and receive calls:

    ```java
    spark.phone().register(new CompletionHandler<Void>() {
        @Override
        public void onComplete(Result<Void> result) {
            if (result.isSuccessful()) {
                // Device registered
            }
            else {
                // Device not registered, and calls will not be sent or received
            }
        }
    });
    ```

4. Use Spark service:

    ```java
    spark.rooms().create("Hello World", null, new CompletionHandler<Room>() {
        @Override
        public void onComplete(Result<Room> result) {
            if (result.isSuccessful()) {
                Room room = result.getData();
            }
            else {
                SparkError error = result.getError();
            }
        }
    });
    
    // ...

    spark.memberships().create(roomId, null, "people@example.com", true, new CompletionHandler<Membership>() {
        @Override
        public void onComplete(Result<Membership> result) {
            if (result.isSuccessful()) {
                Membership membership = result.getData();
            }
            else {
                SparkError error = result.getError();
            }
        }
    });

    // ...

    spark.messages().post(roomId, null, null, "Hello there", null, null, new CompletionHandler<Message>() {
        @Override
        public void onComplete(Result<Message> result) {
            if (result.isSuccessful()) {
                Message message = result.getData();
            }
            else {
                SparkError error = result.getError();
            }
        }
    });
    ```

5. Make an outgoing call:

    ```java
    spark.phone().dial("coworker@acm.com", MediaOption.audioVideo(local, remote), new CompletionHandler<Call>() {
        @Override
        public void onComplete(Result<Call> result) {
            Call call = result.getData();
            if (call != null) {
                call.setObserver(new CallObserver() {
                    @Override
                    public void onRinging(Call call) {
                        
                    }

                    @Override
                    public void onConnected(Call call) {

                    }

                    @Override
                    public void onDisconnected(CallDisconnectedEvent callDisconnectedEvent) {

                    }

                    @Override
                    public void onMediaChanged(MediaChangedEvent mediaChangedEvent) {

                    }
                });
            }
            else {
                SparkError error = result.getError();
            }
        }
    });
    ```

6. Receive a call:

    ```java
    spark.phone().setIncomingCallListener(new Phone.IncomingCallListener() {
        @Override
        public void onIncomingCall(Call call) {
            call.answer(MediaOption.audioVideo(local, remote), new CompletionHandler<Void>() {
                @Override
                public void onComplete(Result<Void> result) {
                    if (result.isSuccessful()) {
                        // success
                    }
                    else {
                        SparkError error = result.getError();
                    }
                }
            });
        }
    });
    ```

## License

&copy; 2016-2017 Cisco Systems, Inc. and/or its affiliates. All Rights Reserved.

See [LICENSE](https://github.com/ciscospark/spark-android-sdk/blob/master/LICENSE) for details.







