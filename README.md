# Cisco Webex Android SDK

[![Travis CI](https://travis-ci.org/webex/webex-android-sdk.svg)](https://travis-ci.org/webex/webex-android-sdk)
[![license](https://img.shields.io/github/license/webex/webex-android-sdk.svg)](https://github.com/webex/webex-android-sdk/blob/master/LICENSE)

> The Cisco Webex™ Android SDK

The Cisco Webex Android SDK makes it easy to integrate secure and convenient Cisco Webex messaging and calling features in your Android apps.

This SDK is built with **Android SDK Tools 27** and requires **Android API Level 21** or later.

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Migrating from Cisco Spark Android SDK](#migrating-from-cisco-spark-android-sdk)
- [Contribute](#contribute)
- [License](#license)

## Install

Assuming you already have an Android project, e.g. _MyWebexApp_, for your Android app, here are the steps to integrate the Cisco Webex Android SDK into your project using [Gradle](https://gradle.org):

1. Add the following repository to your top-level `build.gradle` file:

    ```groovy
    allprojects {
        repositories {
            jcenter()
            maven {
                url 'https://devhub.cisco.com/artifactory/webexsdk/'
            }
        }
    }
    ```

2. Add the `webex-android-sdk` library as a dependency for your app in the `build.gradle` file:

    ```groovy
    dependencies { 
        compile('com.ciscowebex:androidsdk:2.4.0@aar', {
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

To use the SDK, you will need Cisco Webex integration credentials. If you do not already have a Cisco Webex account, visit the [Cisco Webex for Developers portal](https://developer.webex.com/) to create your account and [register an integration](https://developer.webex.com/authentication.html#registering-your-integration). Your app will need to authenticate users via an [OAuth](https://oauth.net/) grant flow for existing Cisco Webex users or a [JSON Web Token](https://jwt.io/) for guest users without a Cisco Webex account.

See the [Android SDK area](https://developer.webex.com/sdk-for-android.html) of the Cisco Webex for Developers site for more information about this SDK.

### Examples

Here are some examples of how to use the Android SDK in your app.

1. Create a new `Webex` instance using Webex ID authentication ([OAuth](https://oauth.net/)-based):

    ```java
    String clientId = "YOUR_CLIENT_ID";
    String clientSecret = "YOUR_CLIENT_SECRET";
    String scope = "spark:all";
    String redirectUri = "Webexdemoapp://response";

    OAuthWebViewAuthenticator authenticator = new OAuthWebViewAuthenticator(clientId, clientSecret, scope, redirectUri);
    Webex webex = new Webex(activity.getApplication(), authenticator)
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

2. Create a new `Webex` instance using Guest ID authentication ([JWT](https://jwt.io/)-based):

    ```java
    JWTAuthenticator authenticator = new JWTAuthenticator();
    Webex webex = new Webex(activity.getApplication(), authenticator);
    if (!authenticator.isAuthorized()) {
        authenticator.authorize(myJwt);
    }
    ```

3. Register the device to Webex platform:

    ```java
    webex.phone().register(new CompletionHandler<Void>() {
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

4. Create a new Cisco Webex space, add users to the space, and send a message:

    ```java
    // Create a Cisco Webex space:

    webex.spaces().create("Hello World", null, new CompletionHandler<Space>() {
        @Override
        public void onComplete(Result<Space> result) {
            if (result.isSuccessful()) {
                Space space = result.getData();
            }
            else {
                WebexError error = result.getError();
            }
        }
    });
    
    // Add a user to a space:

    webex.memberships().create(spaceId, null, "person@example.com", true, new CompletionHandler<Membership>() {
        @Override
        public void onComplete(Result<Membership> result) {
            if (result.isSuccessful()) {
                Membership membership = result.getData();
            }
            else {
                WebexError error = result.getError();
            }
        }
    });

    // Send a message to a space:

    webex.messages().postToSpace(spaceId, Message.Text.html("<strong>Hello</strong>", "Hello"), null, null, new CompletionHandler<Message>() {
        @Override
        public void onComplete(Result<Message> result) {
            if (result.isSuccessful()) {
                Message message = result.getData();
            }
            else {
                WebexError error = result.getError();
            }
        }
    });
    ```

5. Make an outgoing call:

    ```java
    webex.phone().dial("person@example.com", MediaOption.audioVideo(local, remote), new CompletionHandler<Call>() {
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
                    
                    @Override
                    public void onWaiting(Call call, Call.WaitReason waitReason) {
        
                    }
                });
            }
            else {
                WebexError error = result.getError();
            }
        }
    });
    ```

6. Receive a call:

    ```java
    webex.phone().setIncomingCallListener(new Phone.IncomingCallListener() {
        @Override
        public void onIncomingCall(Call call) {
            call.answer(MediaOption.audioVideo(local, remote), new CompletionHandler<Void>() {
                @Override
                public void onComplete(Result<Void> result) {
                    if (result.isSuccessful()) {
                        // success
                    }
                    else {
                        WebexError error = result.getError();
                    }
                }
            });
        }
    });
    ```
7. Make an space call:

    ```java
    webex.phone().dial(spaceId, MediaOption.audioVideoSharing(new Pair<>(localView,remoteView),shareView), new CompletionHandler<Call>() {
        @Override
        public void onComplete(Result<Call> result) {
            Call call = result.getData();
            if (call != null) {
                call.setObserver(new CallObserver() {
                    @Override
                    public void onConnected(Call call) {

                    }

                 	//...

                    @Override
                    public void onCallMembershipChanged(CallMembershipChangedEvent callMembershipChangeEvent) {
                        CallMembership membership = callMembershipChangeEvent.getCallMembership();
                        if (callMembershipChangeEvent instanceof MembershipJoinedEvent) {

                        } else if (callMembershipChangeEvent instanceof MembershipLeftEvent) {

                        } else if (callMembershipChangeEvent instanceof MembershipDeclinedEvent) {

                        } else if (callMembershipChangeEvent instanceof MembershipSendingVideoEvent) {

                        } else if (callMembershipChangeEvent instanceof MembershipSendingAudioEvent) {

                        } else if (callMembershipChangeEvent instanceof MembershipSendingSharingEvent) {

                        }
                    }
                });
            }
            else {
                WebexError error = result.getError();
            }
        }
    });
    ```
    
8. Receive screen share:

    ```java
    webex.phone().dial(spaceId, MediaOption.audioVideoSharing(new Pair<>(localView,remoteView),shareView), new CompletionHandler<Call>() {
        @Override
        public void onComplete(Result<Call> result) {
            Call call = result.getData();
            if (call != null) {
                call.setObserver(new CallObserver() {
                    @Override
                    public void onConnected(Call call) {

                    }

                 	//...

                    @Override
                    public void onMediaChanged(MediaChangedEvent mediaChangedEvent) {
                        if (mediaChangedEvent instanceof RemoteSendingSharingEvent) {
                            if (((RemoteSendingSharingEvent) mediaChangedEvent).isSending()) {
                                mediaChangedEvent.getCall().setSharingRenderView(shareView);
                            } else if (!((RemoteSendingSharingEvent) mediaChangedEvent).isSending()) {
                                mediaChangedEvent.getCall().setSharingRenderView(null);
                            }
                        }
                    }
                });
            }
            else {
                WebexError error = result.getError();
            }
        }
    });
    
    ```
9. Start/stop sharing screen:

    ```java
    activeCall.startSharing(r -> Ln.d("startSharing result: " + r));
    boolean isSharing = activeCall.isSendingSharing();
    activeCall.stopSharing(r -> Ln.d("stopSharing result: " + r));
    ```

10. Post a message

    ```java
    webex.message().postToPerson(
        EmailAddress.fromString("bob@example.com"), 
        Message.Text.markdown("**Hello**", "<strong>Hello</strong>", "Hello"), 
        files,
        new CompletionHandler<Message>() {
            @Override
            public void onComplete(Result<Message> result) {
                if (result.isSuccessful()) {
                    // message sent success
                    ...
                } else {
                    // message sent failed
                    ...
                }
            }
        }));
    ```

11. Receive a message

    ```java
    webex.message().setMessageObserver(
        new MessageObserver() {
            void onEvent(MessageEvent event) {
                if (event instanceof MessageReceived) {
                    Message message = event.getMessage();
                    // new message arrived
                } else if (event instanceof MessageDeleted) {
                    // message deleted
                }
            }
        }
    );
    ```
12. Send Read Receipts

    ```java
    //Mark all exist messages in space as read
    webex.message().markAsRead(spaceId);

    //Mark exist messages before pointed message(include) in space as read
    webex.message().markAsRead(spaceId, messageId);
    ```

13. Multi-Stream to receive more video streams 

    ```java
    activeCall.setMultiStreamObserver(new MultiStreamObserver() {
        @Override
        public void onAuxStreamChanged(AuxStreamChangedEvent event) {
            if (event instanceof MultiStreamObserver.AuxStreamOpenedEvent) {
                if ((MultiStreamObserver.AuxStreamOpenedEvent)event.isSuccessful()) {
                    // success to open a stream
                    ...
                } else {
                    // fail to open a stream
                    ...
                }
            } else if (event instanceof MultiStreamObserver.AuxStreamClosedEvent) {
                if ((MultiStreamObserver.AuxStreamClosedEvent)event.isSuccessful()) {
                    // success to close a stream
                    ...
                } else {
                    // fail to close a stream
                    ...
                }
            } else if (event instanceof MultiStreamObserver.AuxStreamSendingVideoEvent) {
                ...
            } else if (event instanceof MultiStreamObserver.AuxStreamPersonChangedEvent) {
                ...
            } else if (event instanceof MultiStreamObserver.AuxStreamSizeChangedEvent) {
                ...
            }
        }

        @Override
        public View onAuxStreamAvailable() {
            // should return a MediaRenderView for rendering 
            return mediaRenderView;
        }

        @Override
        public View onAuxStreamUnavailable() {
	    // should return a MediaRenderView to stop rendering or return null to let SDK handle it
            return null;
        }
    });
    ```
14. Set MembershipObserver to receive Membership events 

    ```java
    webex.memberships().setMembershipObserver(new MembershipObserver() {
            @Override
            public void onEvent(MembershipEvent event) {
                //The WebexEventPayload.
                WebexEvent.Payload payload = event.getPayload();
                ...
                if (event instanceof MembershipCreated) {
                    //The event when a new membership has added to a space.
                    ...
                } else if (event instanceof MembershipDeleted) {
                    //The event when a membership has removed from a space.
                    ...
                } else if (event instanceof MembershipUpdated) {
                    //The event when a membership moderator status changed
                    ...
                }else if (event instanceof MembershipSeen){
                    //The event when a user has sent a read receipt
                    ...
                }
            }
        });
    ```
15. Set SpaceObserver to receive Space events 

    ```java
    webex.spaces().setSpaceObserver(new SpaceObserver() {
            @Override
            public void onEvent(SpaceEvent event) {
                //The WebexEventPayload.
                WebexEvent.Payload payload = event.getPayload();
                
                if (event instanceof SpaceCreated){
                    //The event when a new space was created
                    ...
                }else if (event instanceof SpaceUpdated){
                    //The event when a space was changed (usually a rename)
                    ...
                }
            }
        });
    ```
16. Get space meeting details

    ```java
    webex.spaces().getMeeting(spaceId, new CompletionHandler<SpaceMeeting>() {
            @Override
            public void onComplete(Result<SpaceMeeting> result) {
                if (result.isSuccessful()){
                    SpaceMeeting spaceMeeting = result.getData();
                    ...
                }
            }
        });
    ```
17. Get read status of a space

    ```java
    webex.spaces().getWithReadStatus(spaceId, new CompletionHandler<SpaceReadStatus>() {
            @Override
            public void onComplete(Result<SpaceReadStatus> result) {
                if (result.isSuccessful()){
                    SpaceReadStatus spaceReadStatus = result.getData();
                    ...
                }
            }
        });
    ```
## Migrating from Cisco Spark Android SDK

The purpose of this guide is to help you to migrate from Cisco Spark Android SDK to Cisco Webex Android SDK.

### Install

Assuming you already have an Android project with Spark Android SDK integrated. For your Android app, here are the steps to migrate to use Webex Android SDK:

1. Change the maven repository url in your top-level `build.gradle` file:

    ```groovy
    allprojects {
        repositories {
            jcenter()
            maven {
                // url 'https://devhub.cisco.com/artifactory/sparksdk/'
                url 'https://devhub.cisco.com/artifactory/webexsdk/'
            }
        }
    }
    ```

2. Update the library dependency for your app in the `build.gradle` file:

    ```groovy
    dependencies { 
        // compile('com.ciscospark:androidsdk:1.4.0@aar', {
        //     transitive = true
        // })
        compile('com.ciscowebex:androidsdk:2.3.0@aar', {
            transitive = true
        })
    }
    ```

### Usage

Here are API changes list from Spark Android SDK to Webex Android SDK.

| Description | Spark Android SDK | Webex Android SDK |
| :----:| :----: | :----:
| Package name | com.ciscospark.androidsdk | com.ciscowebex.androidsdk
| Create a new instance | Spark spark = new Spark(application, authenticator) | Webex webex = new Webex(application, authenticator)
| Get error response | SparkError error = result.getError() | WebexError error = result.getError()
| Rename Room to Space | spark.rooms().get(roomId, CompletionHandler< Room > handler) | webex.spaces().get(spaceId, CompletionHandler< Space > handler)

## Contribute

Pull requests welcome. To suggest changes to the SDK, please fork this repository and submit a pull request with your changes. Your request will be reviewed by one of the project maintainers.

## License

&copy; 2016-2020 Cisco Systems, Inc. and/or its affiliates. All Rights Reserved.

See [LICENSE](https://github.com/webex/webex-android-sdk/blob/master/LICENSE) for details.
