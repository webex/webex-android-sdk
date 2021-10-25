# Cisco Webex Android SDK

> The Cisco Webex™ Android SDK Version 3.2.0

The Cisco Webex Android SDK makes it easy to integrate secure and convenient Cisco Webex messaging and calling features in your Android apps.

This SDK is built with **Android SDK Tools 29** and requires **Android API Level 24** or later.

## Table of Contents

- [New Integration](#new-integration)
- [Advantages](#advantages)
- [Notes](#notes)
- [Integration](#integration)
  - [Option 1](#option-1)
  - [Option 2](#option-2)
- [Usage](#usage)
- [Examples](#examples)
- [Multi Stream](#multi-stream)
- [CUCM](#cucm)
- [Virtual Background](#virtual-background)
- [Calendar Meetings](#calendar-meetings)
- [Migration Guide](#migration-guide)
- [Sample App](#sample-app)
- [API Reference](#api-reference)
- [FedRAMP Testing Guide](#fedramp-testing-guide)
- [License](#license)

## New Integration
For creating a new app integration, new client id generation, etc. visit [App Registration For Mobile SDK v3](https://github.com/webex/webex-android-sdk/wiki/App-Registration-for-Mobile-SDK-v3-)

## Advantages
* Unified feature set: Meeting, Messaging, CUCM calling.
* Greater feature velocity and in parity with the Webex mobile app.
* Easier for developers community: SQLite is bundled for automatic data caching.
* Greater quality as it is built on a more robust infrastructure.

## Notes
* Integrations created earlier will not work with v3 because they are not entitled to the scopes required by v3. You can either raise a support request to enable these scopes for your appId or you could create a new Integration that's meant to be used for v3. This does not affect Guest Issuer JWT token-based sign-in.
* FedRAMP(
Federal Risk and Authorization Management Program) support from 3.1 onwards.
* Currently all resource ids that are exposed from the SDK are barebones GUIDs. You cannot directly use these ids to make calls to [webexapis.com](webexapis.com). You'll need to call `Webex.base64Encode(type: ResourceType, resource: String, handler: CompletionHandler<String>)` to get a base64 encoded resource. However, you're free to interchange between base64 encoded resource ids and barebones GUID while providing them as input to the SDK APIs.
* You can add `android:extractNativeLibs="true"` inside your `<application>` tag in your Manifest file to reduce the generated apk size.
* You can split the application APKs based on architecture for individual distribution. To get details of each architecture library and sample application sizes please visit [here](https://github.com/webex/webex-android-sdk/wiki/Android-SDK-v3---Library-and-Sample-application-sizes)
* The `Webex.initialize` method should be called before invoking any other api.

## Integration

### Option 1
1. Put AAR file in libs folder of your Android project
2. Open the project level Gradle file and add the following lines under the repositories tag, which is nested under allprojects.

      ```
      allprojects {
        repositories {
            flatDir { dirs 'aars'} //add this line
        }
      }
      ```
3. Add the following dependency in module level Gradle file and press sync-now
   ```
   implementation files('libs/WebexSDK.aar')
   ```
### Option 2

   1. Add the following repository to your top-level `build.gradle` file:
        ```
        allprojects {
            repositories {
                maven {
                    url 'https://devhub.cisco.com/artifactory/webexsdk/'
                }
            }
        }
        ```
  2. Add the `webex-android-sdk` library as a dependency for your app in the `build.gradle` file:

        ```
        dependencies {
            implementation 'com.ciscowebex:androidsdk:3.1.0@aar'
        }
        ```
## Usage

To use the SDK, you will need Cisco Webex integration credentials. If you do not already have a Cisco Webex account, visit the [Cisco Webex for Developers portal](https://developer.webex.com/) to create your account and [register an integration](https://developer.webex.com/docs/integrations#registering-your-integration). Your app will need to authenticate users via an [OAuth](https://oauth.net/) grant flow for existing Cisco Webex users or a [JSON Web Token](https://jwt.io/) for guest users without a Cisco Webex account.

See the [Android SDK area](https://developer.webex.com/docs/sdks/android) of the Cisco Webex for Developers site for more information about this SDK.

### Examples

Here are some examples of how to use the Android SDK in your app.

1. Create a new `Webex` instance using Webex ID authentication ([OAuth](https://oauth.net/)-based):

    ```kotlin
    val clientId: String = "YOUR_CLIENT_ID"
    val clientSecret: String = "YOUR_CLIENT_SECRET"
    val redirectUri: String = "https://webexdemoapp.com"
    val scope: String = "spark:all"
    val email = "EMAIL_ID_OF_END_USER" // Get email id from end user

    val authenticator: OAuthWebViewAuthenticator = OAuthWebViewAuthenticator(clientId, clientSecret, scope, redirectUri, email)
    val webex = Webex(application, authenticator)
    webex.enableConsoleLogger(true)
    webex.setLogLevel(LogLevel.VERBOSE) // Highly recommended to make this end-user configurable incase you need to get detailed logs.

    webex.initialize(CompletionHandler { result ->
        if (result.error == null) {
            //already authorised
        } else {
            authenticator.authorize(loginWebview, CompletionHandler { result ->
                    if (result.error != null) {
                        //Handle the error
                    }else{
                        //Authorization successful
                    }
                })
        }
    })
    ```

2. Create a new `Webex` instance using JWT authentication

    ```kotlin
    val token: String = "jwt_token"
    val authenticator: JWTAuthenticator = JWTAuthenticator()
    val webex = Webex(application, authenticator)
    webex.initialize(CompletionHandler { result ->
        if (result.error == null) {
            //already authorised
        } else {
            authenticator.authorize(token, CompletionHandler { result ->
                    if (result.error != null) {
                        //Handle the error
                    }else{
                        //Authorization successful
                    }
                })
        }
    })
    ```

3. Create a new `Webex` instance using access token authentication

   ```kotlin
    val token: String = "<your-access-token>"
    val expiryInSeconds = 60      //expiry time in seconds
    val authenticator: TokenAuthenticator = TokenAuthenticator()
    val webex = Webex(application, authenticator)
    webex.initialize(CompletionHandler { result ->
        if (result.error == null) {
            //already authorised
        } else {
            authenticator.authorize(token, expiryInSeconds, CompletionHandler { result ->
                    if (result.error != null) {
                        //Handle the error
                    }else{
                        //Authorization successful
                    }
                })
        }
    })
    ```

4. Create a new Cisco Webex space, add users to the space, and send a message:

    ```kotlin
    // Create a Cisco Webex space:
    webex.spaces.create("Hello World", null, CompletionHandler<Space?> { result ->
        if (result.isSuccessful) {
            val space = result.data
        } else {
            val error= result.error
        }
    })

    // Add a user to a space:
    webex.memberships.create("spaceId", null, "person@example.com", true, CompletionHandler<Membership?> { result ->
        if (result.isSuccessful) {
            val space = result.data
        } else {
            val error= result.error
        }
    })

    // Send a message to a space:
    webex.messages.postToSpace("spaceId", Message.Text.plain("Hello"), null, null, CompletionHandler<Message> { result ->
        if(result != null && result.isSuccessful){
            val message = result.data
        }
    })
    ```

5. Make an outgoing call:

    ```kotlin
    webex.phone.dial("person@example.com", MediaOption.audioVideo(local, remote), CompletionHandler {
        val call = it.data
        call?.setObserver(object : CallObserver {
            override fun onConnected(call: Call?) {
                super.onConnected(call)
            }

            override fun onDisconnected(event: CallDisconnectedEvent?) {
                super.onDisconnected(event)
            }

            override fun onFailed(call: Call?) {
                super.onFailed(call)
            }
        })
    })
    ```

6. Receive a call:

    ```kotlin
    webex.phone.setIncomingCallListener(object : Phone.IncomingCallListener {
        override fun onIncomingCall(call: Call?) {
            call?.answer(MediaOption.audioOnly(), CompletionHandler {
                if (it.isSuccessful){
                    // ...
                }
            })
        }
    })
    ```

7. Make a space call:

    ```kotlin
    webex.phone().dial(spaceId, MediaOption.audioVideoSharing(Pair(localView,remoteView), screenShareView), CompletionHandler { result ->
        if (result.isSuccessful) {
            result.data?.let { _call ->
                // Space call connected. Set observer to listen for call events
                call.setObserver(object : CallObserver {
                    override fun onConnected(call: Call?) {
                    }

                    override fun onRinging(call: Call?) {
                    }

                    override fun onWaiting(call: Call?, reason: Call.WaitReason?) {
                    }

                    override fun onDisconnected(event: CallObserver.CallDisconnectedEvent?) {
                    }

                    override fun onInfoChanged(call: Call?) {
                    }

                    override fun onMediaChanged(event: CallObserver.MediaChangedEvent?) {
                    }

                    override fun onCallMembershipChanged(event: CallObserver.CallMembershipChangedEvent?) {
                    }

                    override fun onScheduleChanged(call: Call?) {
                    }
                })
            }
        } else {
            result.error?.let { errorCode ->
                // Error in space call
            }
        }
    });
    ```

8. Screen sharing:

    ```kotlin
    webex.phone.dial("spaceId", MediaOption.audioVideoSharing(Pair(localView, remoteView), screenShareView), CompletionHandler {
        if(it.isSuccessful){
            val call = it.data
            call?.setObserver(object :CallObserver{
                override fun onConnected(call: Call?) {
                    super.onConnected(call)
                }

                // ...

                override fun onMediaChanged(event: CallObserver.MediaChangedEvent?) {
                    event?.let { _event ->
                        val _call = _event.getCall()
                        when (_event) {
                            is CallObserver.RemoteSendingSharingEvent -> {
                                if (_event.isSending()) {
                                    _call?.setSharingRenderView(screenShareView)
                                } else {
                                    _call??.setSharingRenderView(null)
                                }
                            }
                        }
                    }
                }
            })
        } else {
            val error = it.error
        }
    })
    ```
9.  Start/stop sharing screen:

    ```kotlin
    call.startSharing(CompletionHandler {
       if (it.isSuccessful){
          // ...
       }
    })
    call.stopSharing(CompletionHandler {
       if (it.isSuccessful){
          // ...
       }
    })
    ```

10. Post a message

    ```kotlin
    webex.messages.post(targetId, Message.draft(Message.Text.markdown("**Hello**", null, null)).addAttachments(localFile), CompletionHandler { result ->
        if (result.isSuccessful) {
            //message sent success
        } else {
            val error = result.error
            //message sent failed
        }
    })
    ```

11. Post a threaded message

    ```kotlin
    webex.messages.post(targetId, Message.draft(Message.Text.markdown("**Hello**", null, null))
    .addAttachments(localFile)
    .setParent(parentMessage),
    CompletionHandler { result ->
        if (result.isSuccessful) {
            //message sent success
        } else {
            val error = result.error
            //message sent failed
        }
    })
    ```

12. Set MessageObserver to receive messaging events
    ```kotlin
    webex.messages.setMessageObserver(object : MessageObserver {
        override fun onEvent(event: MessageObserver.MessageEvent) {
            when (event) {
                is MessageObserver.MessageReceived -> {
                    val message = event.getMessage()
                    if (message?.getParentId() != null) {
                        // Threaded message
                    }
                }
                is MessageObserver.MessageDeleted -> {
                    // message deleted
                }
                is MessageObserver.MessageFileThumbnailsUpdated -> {
                    // thumbnails updated for files
                }
                is MessageObserver.MessageEdited -> {
                    // message edited successfully. event.getMessage() returns the edited message.
                }
            }
        }
    })
    ```

13. Send Read Receipts

    ```kotlin
     //Mark all existing messages in space as read
     webex.messages.markAsRead(spaceId)

     //Mark existing message before pointed message(include) in space as read
     webex.message.markAsRead(spaceId, messageId)

     //Mark existing message before pointed message(include) in space as read with a completion handler
     webex.message.markAsRead(spaceId, messageId, CompletionHandler { result ->
        if (result.isSuccessful) {
            // Success
        } else {
            // Failure
        }
    })
    ```

14. Get read status of a space

    ```kotlin
    webex.spaces.getWithReadStatus(spaceId, CompletionHandler { result ->
        if (result.isSuccessful) {
            //show the data
        } else {
            //handle error
        }
    })
    ```

15. Set MembershipObserver to receive Membership events

    ```kotlin
    webex.memberships.setMembershipObserver(object : MembershipObserver {
        override fun onEvent(event: MembershipObserver.MembershipEvent?) {
            when (event) {
                is MembershipObserver.MembershipCreated -> {
                    //The event when a new membership has added to a space.
                    ...
                }
                is MembershipObserver.MembershipUpdated -> {
                    //The event when a membership moderator status changed
                    ...
                }
                is MembershipObserver.MembershipDeleted -> {
                    //The event when a membership has been removed from a space.
                    ...
                }
                is MembershipObserver.MembershipMessageSeen -> {
                    //The event when a user has sent a read receipt
                    ...
                }
            }
        }
    })
    ```

16. Set SpaceObserver to receive Space events

    ```kotlin
    webex.spaces.setSpaceObserver(object : SpaceObserver {
        override fun onEvent(event: SpaceObserver.SpaceEvent) {
            when (event) {
                is SpaceObserver.SpaceCallStarted -> {
                    //The event when a space call was started
                    ...
                }
                is SpaceObserver.SpaceCallEnded -> {
                    //The event when a space call has ended
                    ...
                }
                is SpaceObserver.SpaceCreated -> {
                    //The event when a new space was created
                    ...
                }
                is SpaceObserver.SpaceUpdated -> {
                    //The event when a space was changed (usually a rename)
                    ...
                }
            }
        }
    })
    ```

17. Get space meeting details

    ```kotlin
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

18. Get read status of a space

    ```kotlin
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

19. Join password-protected meetings

    ```kotlin
    mediaOption.setModerator(isModerator: Boolean)
    mediaOption.setPin(pin: String)
    ```

20. Change the composite video layout during a call

    ```kotlin
    activeCall.setCompositedVideoLayout(layout: MediaOption.CompositedVideoLayout)
    ```

21. Specify how the remote video adjusts its content to be rendered in a view

    ```kotlin
    activeCall.setRemoteVideoRenderMode(mode);
    ```
    Use a completion handler to get the result of success or failure.
    ```
    activeCall..setRemoteVideoRenderMode(mode, CompletionHandler {
        it.let {
            if (it.isSuccessful) {
                // callback returned success
            } else {
                // callback returned failure
            }
        }
    })
    ```
22. Change the max sending fps for video

    ```kotlin
    webex.phone.setAdvancedSetting(AdvancedSetting.VideoMaxTxFPS(value: Int) as AdvancedSetting<*>)
    ```
23. Enable(disable) android.hardware.camera2

    ```kotlin
    webex.phone.setAdvancedSetting(AdvancedSetting.VideoEnableCamera2(value: Boolean) as AdvancedSetting<*>)
    ```
24. Whether the app can continue video streaming when the app is in background

    ```kotlin
    webex.phone.enableBackgroundStream(enable: Boolean)
    ```
25. Get a list of spaces that have an ongoing call

    ```kotlin
    webex.spaces.listWithActiveCalls(CompletionHandler { result ->
        if (result.isSuccessful) {
            // callback returned success, result.data gives data if any
        } else {
            // callback returned failure
        }
    })
    ```
26. Check if the message mentioned everyone in space

    ```kotlin
    message.isAllMentioned()
    ```
27. Get all people mentioned in the message

    ```kotlin
    message.getMentions()
    ```
28. Change the max capture fps when screen sharing

    ```kotlin
    webex.phone.setAdvancedSetting(AdvancedSetting.ShareMaxCaptureFPS(value: Int) as AdvancedSetting<*>)
    ```
29. Switch the audio play output mode during a call

    ```kotlin
    activeCall.switchAudioOutput(mode: Call.AudioOutputMode);
    ```

30. Enable/Disable Background Noise Removal(BNR)

    ```kotlin
    webex.phone.enableAudioBNR(enable: Boolean)
    ```
31. Set Background Noise Removal(BNR) mode

    ```kotlin
    webex.phone.setAudioBNRMode(mode: Phone.AudioBRNMode)
    ```
32. Edit a message

    ```kotlin
    webex.messages.edit(originalMessage, messageText, mentions, CompletionHandler { result ->
        if (result.isSuccessful) {
            // message edit success
            val editedMessage = result.data
        } else {
            // message edit failure
        }
    })
    ```
33. Enable/Disable background connection

    ```kotlin
    webex.phone.enableBackgroundConnection(enable: Boolean)
    ```

34. Enable/Disable console logging

    ```kotlin
    webex.enableConsoleLogger(enable: Boolean)
    ```

35. Set log level of logging

    ```kotlin
    webex.setLogLevel(logLevel: LogLevel)
    ```


## Multi Stream
For multistream related APIs see [Multi Stream v3](https://github.com/webex/webex-android-sdk/wiki/Multi-Stream-v3-)

## CUCM
For CUCM related APIs see [CUCM Usage Guide v3](https://github.com/webex/webex-android-sdk/wiki/CUCM-Usage-Guide-v3)

## Virtual Background
For virtual background related APIs see [Virtual Background](https://github.com/webex/webex-android-sdk/wiki/Virtual-Background)

## Calendar Meetings
For Calendar Meetings related APIs see [Calendar Meetings](https://github.com/webex/webex-android-sdk/wiki/Calendar-Meetings-APIs)

## Migration Guide
The migration guide is meant to help developers port their code from SDK-v2 to SDK-v3. See [Migration Guide For v2 to v3](https://github.com/webex/webex-android-sdk/wiki/Migration-Guide-for-v2-to-v3)

## Sample App
The sample app demonstrates the common usage of SDK-v3. You can view the demo app [Source Code](https://github.com/webex/webex-android-sdk-example)

## API Reference
For a complete reference to all supported APIs, please visit [Webex Android SDK API docs](https://webex.github.io/webex-android-sdk/).

## FedRAMP Testing Guide
For complete testing guide, please visit [FedRAMP Testing Guide](https://github.com/webex/webex-android-sdk/wiki/Android-SDK---FedRAMP-Environment)

## License

All contents are licensed under the Cisco EULA

See [License](LICENSE.txt) for details.
