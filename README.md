# Cisco Webex Android SDK

> The Cisco Webex™ Android SDK Version 3.0.0

The Cisco Webex Android SDK makes it easy to integrate secure and convenient Cisco Webex messaging and calling features in your Android apps.

This SDK is built with **Android SDK Tools 29** and requires **Android API Level 24** or later.

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Migrating from Cisco Spark Android SDK](#migrating-from-cisco-spark-android-sdk)
- [Contribute](#contribute)
- [License](#license)

## Install

Assuming you already have an Android project, e.g. _KitchenSinkApp_, for your Android app, here are the steps to integrate the Cisco Webex Android SDK into your project using [Gradle](https://gradle.org):

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
        compile('com.ciscowebex:androidsdk:3.0.0@aar', {
            transitive = true
        })
    }
    ```


## Usage

To use the SDK, you will need Cisco Webex integration credentials. If you do not already have a Cisco Webex account, visit the [Cisco Webex for Developers portal](https://developer.webex.com/) to create your account and [register an integration](https://developer.webex.com/docs/integrations#registering-your-integration). Your app will need to authenticate users via an [OAuth](https://oauth.net/) grant flow for existing Cisco Webex users or a [JSON Web Token](https://jwt.io/) for guest users without a Cisco Webex account.

See the [Android SDK area](https://developer.webex.com/docs/sdks/android) of the Cisco Webex for Developers site for more information about this SDK.

### Examples

Here are some examples of how to use the Android SDK in your app.

1. Create a new `Webex` instance using Webex ID authentication ([OAuth](https://oauth.net/)-based):

    ```kotlin
    String clientId = "YOUR_CLIENT_ID"
    String clientSecret = "YOUR_CLIENT_SECRET"
    String scope = "spark:all"
    String redirectUri = "squared://oauth2"

    OAuthWebViewAuthenticator authenticator = OAuthWebViewAuthenticator(clientId, clientSecret, scope, redirectUri)
    val webex = Webex.getInstance(application, authenticator)
    if(!authenticator.isAuthorized){
      authenticator.authorize(loginWebview, CompletionHandler { result ->
                  if (result.error != null) {
                      //Handle the error
                  }else{
                      //Authorization sucessful
                  }
              })
    }else{
      //already authorised
    }

    ```

2. Create a new Cisco Webex space, add users to the space, and send a message:

    ```kotlin
    // Create a Cisco Webex space:

    webex.spaces.create("Hello World", null, CompletionHandler<Space?> { result ->
            if (result.isSuccessful) {
                val space = result.data
            } else {
                val error= result.error
            }
        }

    // Add a user to a space:

    webex.memberships.create("spaceId", null, "person@example.com", true, CompletionHandler<Membership?> { result ->
            if (result.isSuccessful) {
                val space = result.data
            } else {
                val error= result.error
            }
        }

    // Send a message to a space:

    webex.messages.postToSpace("spaceId", Message.Text("Hello", "**Hello**", "<strong>Hello</strong>"), null, null, CompletionHandler<Message> { result ->
            if(result!=null && result.isSuccessful){
                val message = result.data
            }
        }
    ```

3. Make an outgoing call:

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

4. Receive a call:

    ```kotlin
    webexViewModel.webex.phone.setIncomingCallListener(object : Phone.IncomingCallListener {
            override fun onIncomingCall(call: Call?) {
                call?.answer(MediaOption.audioOnly(), CompletionHandler {
                    if(it.isSuccessful){
		        val answeredCall = it.data
		    }
                })
            }
        })
    ```

5. Receive screen share:

    ```kotlin
    webexViewModel.webex.phone.dial("spaceId", MediaOption.audioVideoSharing(Pair(localView, remoteView), shareView), CompletionHandler {
            if(it.isSuccessful){
                val call = it.data
                call?.setObserver(object :CallObserver{
                    override fun onConnected(call: Call?) {
                        super.onConnected(call)
                    }

                    override fun onTerminated(call: Call?) {
                        super.onTerminated(call)
                    }
                })
            } else {
                val error = it.error
            }
        })

    ```
6. Start/stop sharing screen:

    ```kotlin
    webex.phone.startShare("callid")
    webex.phone.stopShare("callid")
    ```

7. Post a message

    ```kotlin
    webex.messages.postToPerson(email, Message.Text("Hello", "**Hello**", "<strong>Hello</strong>"), null, CompletionHandler { result ->
	if (result.isSuccessful) {
	    //posting a message is successful
	} else {
	    val error = it.error
	    //handle the error
	}
    })
    ```

8. Send Read Receipts

    ```kotlin
    //Mark all exist messages in space as read
    webex.messages.markAsRead(spaceId)

    //Mark existing message before pointed message(include) in space as read
    webex.message.markAsRead(spaceId, messageId);
    ```

9. Get read status of a space

    ```kotlin
     webex.spaces.getWithReadStatus(spaceId, CompletionHandler { result ->
                if (result.isSuccessful) {
                    //show the data
                } else {
                    //handle error
                }
            })
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
        implementation('com.ciscowebex:androidsdk:3.0.0@aar', {
            transitive = true
        })
    }
    ```

### Usage

Here are API changes list from Spark Android SDK to Webex Android SDK.

| Description | Spark Android SDK | Webex Android SDK |
| :----:| :----: | :----:
| Package name | com.ciscospark.androidsdk | com.ciscowebex.androidsdk
| Create a new instance | Spark spark = new Spark(application, authenticator) | Webex webex = Webex.getInstance(application, authenticator)
| Get error response | SparkError error = result.getError() | WebexError error = result.getError()
| Rename Room to Space | spark.rooms().get(roomId, CompletionHandler< Room > handler) | webex.spaces().get(spaceId, CompletionHandler< Space > handler)

## Contribute

Pull requests welcome. To suggest changes to the SDK, please fork this repository and submit a pull request with your changes. Your request will be reviewed by one of the project maintainers.

## License

&copy; 2016-2020 Cisco Systems, Inc. and/or its affiliates. All Rights Reserved.

See [LICENSE](LICENSE) for details.
