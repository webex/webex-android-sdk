# Change Log
All notable changes to this project will be documented in this file.


#### 3.0.0
- `3.0.0` Releases - [3.0.0](#300)

#### 2.8.0 Releases
- `2.8.0` Releases - [2.8.0](#280)

#### 2.7.0 Releases
- `2.7.0` Releases - [2.7.0](#270)

#### 2.6.0 Releases
- `2.6.0` Releases - [2.6.0](#260)

#### 2.5.0 Releases
- `2.5.0` Releases - [2.5.0](#250)

#### 2.4.0 Releases
- `2.4.0` Releases - [2.4.0](#240)

#### 2.3.0 Releases
- `2.3.0` Releases - [2.3.0](#230)

#### 2.1.1 Releases
- `2.1.1` Releases - [2.1.1](#211)

#### 2.1.0 Releases
- `2.1.0` Releases - [2.1.0](#210)

#### 2.0.0 Releases
- `2.0.0` Releases - [2.0.0](#200)

#### 1.4.0 Releases
- `1.4.0` Releases - [1.4.0](#140)

#### 1.3.0 Releases
- `1.3.0` Releases - [1.3.0](#130)

#### 0.2.0 Releases
- `0.2.0` Releases - [0.2.0](#020)


---
## [3.0.0](https://github.com/webex/webex-android-sdk/releases/tag/3.0.0)
Released on **24 May, 2021**.

**NOTE: SDK-v3 is built in Kotlin language.**
#### Added
- Ability to make calls via CUCM.
- Receive push notification for incoming CUCM calls.
- `WebexUCLoginDelegate` interface to receive webex CUCM login events.
- `Call.startAssociatedCall(dialNumber: String, associationType: CallAssociationType, audioCall: Boolean, callback: CompletionHandler<Call>)` for CUCM calls
- `Call.transferCall(toCallId: String)` for CUCM calls
- `Call.mergeCalls(targetCallId: String)` for CUCM calls
- `Call.holdCall(putOnHold: Boolean)` for CUCM calls
- `Call.isOnHold()` for CUCM calls
- `Call.isCUCMCall()` to check if call is CUCM
- `Call.canShare()` to check if the call has permission to share the screen
- `Call.getTitle()` to get the title of the call
- `Call.muteParticipantAudio(participantId: String, doMute: Boolean)` to mute particular participant
- `Call.muteAllParticipantAudio(doMute: Boolean)` to mute all other participants who are on call, also un-mutes the others if isMuted is `true`
- `Call.isGroupCall()` to check if the call is Space call
- `Phone.getCallHistory` to retrieve the collection of spaces which contains call history of One to One Spaces as well as Group type Spaces
- `CallObserver.OnInfoChanged` - A callback whenever a call information is changed for example - a participant is added or removed from call or mute status is changed
- `CallObserver.CallEnded` - This event is fired when the resources of the call object gets cleared after disconnection.
- `CallSchedule.getId()` to get meeting ID of the scheduled call.
- `CallSchedule.getMeetingLink()` to get meeting link of the scheduled call.
- `CallSchedule.getSubject()` to get the subject of the scheduled call.
- `NotificationCallType` enum to check if call type is Webex or CUCM
- `CallAssociationType` enum for call transfer and merge
- `Webex.getCallIdByNotificationId(notificationId: String, callType: NotificationCallType)` to get the actual call id of the call based on callType.
- `Webex.spaces.filter(query: String, handler: CompletionHandler<List<Space>>)` to search the people by email id or by name.
- `Webex.getlogFileUri(incudeLastRunLog: Boolean)` to collect logs of SDK for dev support.
- `Webex.base64Encode(type: ResourceType, resource: String, handler: CompletionHandler<String>)` to encode UUID as Base64
- `Webex.base64Decode(encodedResource: String): Resource` to decode Base64 to Resource
- `Webex.initialize(handler: CompletionHandler<Void>)` to check if user is already logged in
- `Webex.isUCLoggedIn()` for CUCM
- `Webex.getUCServerConnectionStatus()` for CUCM
- `Webex.setUCDomainServerUrl(ucDomain: String, serverUrl: String)` for CUCM
- `Webex.enableConsoleLogger(enable: Boolean)` to enable/disable console logging
- `Message.isContentDecrypted()` to check if message is decrypted
- `getConversationId()`, `getMessageId()`, `getContentIndex()` in `RemoteFile`

#### Updated
- `OAuthWebViewAuthenticator` takes email as a constructor parameter
- Support of completion handler in `JWTAuthenticator.authorize(String jwt, CompletionHandler handler)`
- Support of completion handler in `MessageClient.markAsRead(String spaceId, String messageId, CompletionHandler handler)`
- In `Message.Mention`, two new fields `start` and `end` are introduced to indicate the start and end index of mention in text message.
- Support of completion handler in `setRemoteVideoRenderMode` in `Call`

#### Removed
- SSO Authenticator
- Webex.runInBackground()
- OAuthAuthenticator
- `refreshToken` api is removed from `OAuthWebViewAuthenticator`
- `afterAssociated` api
- `update` api removed from `Message`
- `Phone.register()` and `Phone.deregister()` apis are removed

---
## [2.8.0](https://github.com/webex/webex-android-sdk/releases/tag/2.8.0)
Released on 2021-04-30.
#### Added
- Support Multi-stream feature in group calls.
- Support message edit.
- Expose some service urls.
- Allow preview during dialling.
- Support for meeting scheduled from Webex Meetings.
- Increase the meeting number to 11 digit number.
- Add a new API `Phone.enableBackgroundConnection(boolean)` to keep receive events when app is in background.
- Add a new API `Phone.enableAskingReadPhoneStatePermission(boolean enable)` enable or disable ask for read phone state permission.
- Removed the API `CallMembership.getEmail()` due to privacy protection reasons.
- Add a new API `CallMembership.getDisplayName()` to get the display name of the person.

#### Updated
- Upgrade min sdk version to 24.
- Fixed occasionally encryption failures when trying to send messages.
- Fixed screen sharing doesn't work if targetSDK >= 29.
- Fixed LocalLeft event occasionally triggered incorrect.
- Fixed MediaOption.setPin() doesn't appear to work.

---
## [2.7.0](https://github.com/webex/webex-android-sdk/releases/tag/2.7.0)
Released on 2020-12-14.
#### Added
- Support to notify a space call status through SpaceObserver
- Support to notify muted by host during a space call.
- Support to enable audio Background Noise Removal(BNR), and switch between HP(High Performance) and LP(Low Power) mode.
- Not sending sensitive headers for unknown site.
- Add a new API `Phone.setAdvancedSetting(new ShareMaxCaptureFPS(Int))` to change the max capture fps when screen sharing.
- Add a new API `Call.switchAudioOutput(AudioOutputMode audioOutputMode)` to switch the audio play output mode during a call.

#### Updated
- Fixed users required to activate H.264 multiple times.
- Fixed SpaceClient.listWithActiveCalls() cannot show spaces cross-cluster.

## [2.6.0](https://github.com/webex/webex-android-sdk/releases/tag/2.6.0)
Released on 2020-09-28.
#### Added
- Support for incoming call notifications for scheduled sapce call.
- Support for being notified of the end of a space call.
- Support to join password-protected meetings.
- Support receiving annotation from the Webex Teams Client.
- Add a new API `Call.setVideoLayout(VideoLayout)` to change the video layout during a call.
- Add a new API `Call.setRemoteVideoRenderMode(VideoRenderMode)` to specify how the remote video adjusts its content to be render in a view.
- Add a new API `Phone.setAdvancedSetting(new VideoMaxTxFPS(Int))` to change the max sending fps for video.
- Add a new API `Phone.setAdvancedSetting(new VideoEnableCamera2(Boolean))` to enable(disable) android.hardware.camera2.
- Add a new API `Phone.enableBackgroundStream(boolean)` to let control whether the app can continue video streaming when app in background.
- Add a new API `SpaceClient.listWithActiveCalls` to get a list of spaces that have ongoing call.
- Add a new API `Message.isAllMentioned` to check if the message mentioned everyone in space.
- Add a new API `Message.getMentions` to get all people mentioned in the message.
- Popup H.264 license warning by default when first call.

#### Updated
- Improved video and audio quality
- API enhancements to improve bandwidth control.
- Fixed crash when behind the proxy.
- Fixed receiving calls failing after declining a space call.
- Fixed self view is rotated 90° if the call is started in landscape mode.
- Fixed previous messages are not listed.
- Fixed volume up/down key cannot control call volume directly.
- Fixed user in EMEAR org cannot message and call the user in US org.
- Fixed could not get thumbnail of the WORD, POWERPOINT, EXCEL and PDF file in the message.
- Fixed undesired OnConnectEvent callback is received twice.
- Fixed local and remote views are stuck/frozen.
- Fixed black screen when enable hardware codec.
- Fixed NullPointerException when post messages.
- Fixed cannot list messages in space for some case.
- Fixed annotations not received by some users in a space call.
- Fixed hangup fail when the the app is switched between the foreground and the background.
- Fixed SSLHandshake on Android 7.
- Fixed video isn't going full screen.


## [2.5.0](https://github.com/webex/webex-android-sdk/releases/tag/2.5.0)
Released on 2020-04-01.
#### Added
- Support to send/receive the threaded messaging.
- Support compose and render the active speaker video with other attendee video and all the names in one single view.
- Support single, filmstrip and grid layouts for the composed video view.

#### Updated
- Improve dependencies tree.
- Fixed no video if set screenShare view to null.
- Fixed crashes when posting file in which name starts with "#" sign.
- Fixed lock the audio playback when play through bluetooth headset.

## [2.4.0](https://github.com/webex/webex-android-sdk/releases/tag/2.4.0)
Released on 2020-01-15.
#### Added
- Support to join the meeting where lobby is enabled.
- Support to let-in waiting people from lobby to the meeting.

#### Updated
- Fixed users' audio cannot be heard mute/unmute.
- Fixed remote video black screen in PiP mode.

## [2.3.0](https://github.com/webex/webex-android-sdk/releases/tag/2.3.0)
Released on 2019-09-30.
#### Added
- Add API to receive membership created/deleted/updated/seen events.
- Add API to receive room created/updated events.
- Add API to get a space's last activity status.
- Add API to get a list of all space's last activity status.
- Add API to get a list of memberships's read status in a space.
- Add API to get space meeting details.
- Add API to send read receipts for message.
- Add API to get the token expiration date for JWTAuthenticator.

#### Updated
- Update to Dagger 2 dependency.
- Reduce latency when list messages.
- Fixed message list result include the message as "before" query parameter.
- Fixed audio in meeting being faint or nonexistant.
- Fixed already calling error when same cases.
- Removed notification when download file.

## [2.1.1](https://github.com/webex/webex-android-sdk/releases/tag/2.1.1)
Released on 2019-07-24.
#### Added
- Support 64bits.
- Support Google hardware media codec for video.
- Add API to list person by person IDs and Org ID.
- Add API to get the person's last activity.
- Add API to get the person's presence status.

#### Updated
- Fixed call disconnected after waiting in lobby.

## [2.1.0](https://github.com/webex/webex-android-sdk/releases/tag/2.1.0)
Released on 2019-06-09.
#### Updated
- Fixed download remote file issue.
- Fixed sending file issue.
- Fixed event returned after posting a file.
- Improve APIs of message.
- Improve API docs.

## [2.0.0](https://github.com/webex/webex-android-sdk/releases/tag/2.0.0)
Released on 2018-10-31.
#### Added
- SDK rebranding.
- Support multi stream in space call.
- Add active speaker related API and event.

#### Updated
- Upgrade to latest media engine.
- Rename room to space.
- Update PersonId in CallMemberShip to be the same as participant's id.
- Fixed sending message error if login with different user account.

## [1.4.0](https://github.com/ciscospark/spark-android-sdk/releases/tag/1.4.0)
Released on 2018-08-23.

#### Added
- Support screen sharing for both sending and receiving.
- A new API to refresh token for authentication.
- Two properties in Membership: personDisplayName, personOrgId.
- Support real time message receiving.
- Support message end to end encription.
- A few new APIs to do message/file end to end encryption, Mention in message, upload and download encrypted files.
- Five properties in Person: nickName, firstName, lastName, orgId, type.
- Three functions to create/update/delete a person for organization's administrator.
- Support room list ordered by either room ID, lastactivity time or creation time.
- A new property in TeamMembership: personOrgId.
- Two new parameters to update webhook : status and secret.

#### Updated
- Fixed sometimes cannot receive callback when hangup a call.
- Fixed video call has bad video quality with Vuzix M300 smart glasses.
- Fixed the order of redirectUri and scope are reversed in OAuthWebViewAuthenticator.

## [1.3.0](https://github.com/ciscospark/spark-android-sdk/releases/tag/1.3.0)
Released on 2018-01-12.

#### Added
- Receive and display content-sharing stream
- Support room calling/multi-party calling
- Support Single-Sign-On authentication
- Set the maximum bandwidth for Audio/Video/Content Sharing

#### Updated
- Fixed always receiving incoming room call even if there is nobody in the meeting room
- Fixed unstable call state caused by race condition in call control events
- Fixed random crash when logout
- Updated the license by adding a term for H264 codec, and adding a new license file for "Cisco API" used in the project.

#### Removed
The following exclude is no longer needed in the packagingOptions (unless RxJava2 or its related library is involved in developers's app):

    packagingOptions {
        exclude 'META-INF/rxjava.properties'
    }

## [0.2.0](https://github.com/ciscospark/spark-android-sdk/releases/tag/0.2.0)
Released on 2017-11-30.

#### Added
- Initial release of Cisco Spark SDK for Android.
