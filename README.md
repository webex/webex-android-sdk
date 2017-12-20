# Cisco Spark Android SDK

[![Travis CI](https://travis-ci.org/ciscospark/spark-android-sdk.svg)](https://travis-ci.org/ciscospark/spark-android-sdk)
[![license](https://img.shields.io/github/license/ciscospark/spark-android-sdk.svg)](https://github.com/ciscospark/spark-android-sdk/blob/master/LICENSE)

> The Cisco Spark™ Android SDK

The Cisco Spark Android SDK makes it easy to integrate secure and convenient Cisco Spark messaging and calling features in your Android apps.

This SDK is built with **Android SDK Tools 25** and requires **Android API Level 21** or later.

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Contribute](#contribute)
- [License](#license)

## Install

Assuming you already have an Android project, e.g. _MySparkApp_, for your Android app, here are the steps to integrate the Cisco Spark Android SDK into your project using [Gradle](https://gradle.org):

1. Add the following repository to your top-level `build.gradle` file:

    ```groovy
    allprojects {
        repositories {
            jcenter()
            maven {
                url 'https://devhub.cisco.com/artifactory/sparksdk/'
            }
        }
    }
    ```

2. Add the `spark-android-sdk` library as a dependency for your app in the `build.gradle` file:

    ```groovy
    dependencies { 
        compile('com.ciscospark:androidsdk:0.2.0@aar', {
            transitive = true
        })
    }
    ```

3. Enable [multidex](https://developer.android.com/studio/build/multidex.html) support for your app:

    ```groovy
    android {
        defaultConfig {
            multiDexEnabled true
        }
    }
    ```
    
4. Exclude rxjava.properties in your packagingOptions :

    ```groovy
    packagingOptions {
        exclude 'META-INF/rxjava.properties'
    }
    ```

## Usage

To use the SDK, you will need Cisco Spark integration credentials. If you do not already have a Cisco Spark account, visit the [Cisco Spark for Developers portal](https://developer.ciscospark.com/) to create your account and [register an integration](https://developer.ciscospark.com/authentication.html#registering-your-integration). Your app will need to authenticate users via an [OAuth](https://oauth.net/) grant flow for existing Cisco Spark users or a [JSON Web Token](https://jwt.io/) for guest users without a Cisco Spark account.

See the [Android SDK area](https://developer.ciscospark.com/sdk-for-android.html) of the Cisco Spark for Developers site for more information about this SDK.

### Examples

Here are some examples of how to use the Android SDK in your app.

1. Create a new `Spark` instance using Spark ID authentication ([OAuth](https://oauth.net/)-based):

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

2. Create a new `Spark` instance using Guest ID authentication ([JWT](https://jwt.io/)-based):

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

4. Create a new Cisco Spark space, add users to the space, and send a message:

    ```java
    // Create a Cisco Spark space:

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
    
    // Add a user to a space:

    spark.memberships().create(roomId, null, "person@example.com", true, new CompletionHandler<Membership>() {
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

    // Send a message to a space:

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
    spark.phone().dial("person@example.com", MediaOption.audioVideo(local, remote), new CompletionHandler<Call>() {
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

## Contribute

Pull requests welcome. To suggest changes to the SDK, please fork this repository and submit a pull request with your changes. Your request will be reviewed by one of the project maintainers.

## License

&copy; 2016-2017 Cisco Systems, Inc. and/or its affiliates. All Rights Reserved.

See [LICENSE](https://github.com/ciscospark/spark-android-sdk/blob/master/LICENSE) for details.
