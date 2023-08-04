# Change Log
All notable changes to this project will be documented in this file.

#### 3.9.1.3 Releases
- `3.9.1.3` Releases - [3.9.1.3](#3913)

#### 3.9.1 Releases
- `3.9.1` Releases - [3.9.1](#391)

#### 3.9.0 Releases
- `3.9.0` Releases - [3.9.0](#390)

#### 3.8.0 Releases
- `3.8.0` Releases - [3.8.0](#380)

#### 3.7.0 Releases
- `3.7.0` Releases - [3.7.0](#370)

#### 3.6.0 Releases
- `3.6.0` Releases - [3.6.0](#360)

#### 3.5.0 Releases
- `3.5.0` Releases - [3.5.0](#350)

#### 3.4.0 Releases
- `3.4.0` Releases - [3.4.0](#340)

#### 3.3.0 Releases
- `3.3.0` Releases - [3.3.0](#330)

#### 3.2.1 Releases
- `3.2.1` Releases - [3.2.1](#321)

#### 3.2.0 Releases
- `3.2.0` Releases - [3.2.0](#320)

#### 3.1.0 Releases
- `3.1.0` Releases - [3.1.0](#310)

#### 3.0.0 Releases
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


## [3.9.1.3]
Released on **4 August, 2023**.
### Added
- New API added `Phone.dialPhoneNumber(dialString: String, option: MediaOption, callback: CompletionHandler<Call>)` to dial only phone numbers.
- New API `CallHistoryRecord.isPhoneNumber()` to denotes if call record was a phone number.

## [3.9.1](https://github.com/webex/webex-android-sdk/releases/tag/3.9.1)
Released on **19 June, 2023**.
### Added
- New API added `Call.getExternalTrackingId()` to get the external tracking id for corresponding call. Applicable only for WxC calls.
- Supports Webex Calling NewCall notifications payload through webhook.

### Updated
- FIXED: Self video turning off in case of poor uplink event.
- Fixed: Crash in `Webex.signOut` API

## [3.9.0](https://github.com/webex/webex-android-sdk/releases/tag/3.9.0)
Released on **05 June, 2023**.
### Added
- New SDK variant Webex-Wxc released, a light weight SDK for WebexCalling which can be created as an Android Dynamic module.
- New Callback added `CallObserver.onStartRinging(call:Call?, ringerType: Call.RingerType)`, when a ringer has to be started.
- New Callback added `CallObserver.onStopRinging(call:Call?, ringerType: Call.RingerType)`, when a ringer has to be stopped.
- New Class added `AdvancedSettings.EnablePhotoCapture` to enable capturing photo.
- New Class added `ShareConfig` a defined data type to represent the share screen config.
- New Enum added `Call.RingerType`  for a ringerType to denote the type of tone to be played/stopped
- New Enum added `CallMembership.DeviceType` for device types
- New Enum added `Call.ShareOptimizeType` to represent the OptimiseType for share screen
- New Data added `Person.encodedId` to get base64 encoded ID of the person
- New API added `CallMembership.getDeviceType()` to get device type joined by this CallMembership.
- New API added `CallMembership.getPairedMemberships()` to get all memberships joined using deviceType "Room".
- New API added `Call.isVideoEnabled()` to indicate whether video calling is enabled for the user in Control hub.
- New API added `Call.getShareConfig()` to get the share screen optimisation type of the call.
- New Feature added to support multiple active calls.
- New Feature added to support end-to-end encrypted meetings
- Metrics added to monitor SDK performance, track login flows & diagnostics events like token related errors.
- New API added `Call.enableReceivingNoiseRemoval(enable: Boolean, callback: CompletionHandler<ReceivingNoiseRemovalEnableResult>)` to enable or disable receiving noise removal functionality for incoming PSTN calls.
- New API added `Call.getReceivingNoiseInfo()` to get the info object which contains information on Receiving noise removal state.

### Updated
- Screen sharing now have optimisation options as part of share config in `startSharing()` API
- Now FedRamp can be enabled through authenticators
- Now `CallObserver.RemoteCancel` event  will be fired when host ends meeting for all or kicked by host.
- FIXED: Webex calling failures for certificate issues
- FIXED: `PersonClient.getMe()` api returns multiple callbacks for errors
- FIXED: `MessagesUpdated` event getting called for internal provisional messages.
- FIXED: `MessagesUpdated` event not getting called with decrypted content in some cases, after list message API was called.
- FIXED: Crash when setting multiple call observers from different threads.

## [3.8.0](https://github.com/webex/webex-android-sdk/releases/tag/3.8.0)
Released on **24 January, 2023**.
#### Added
- New SDK variant `WebexSDK-Meeting`, a light weight meeting-only SDK(doesn’t include calling).
- New API `setCallServiceCredential(username: String, password: String)` to  set username and password for authentication with calling service.
- New API `onUCLoginFailed(failureReason: UCLoginFailureReason)` to notify app whenever CUCM server login or Webex Calling login fails.
- New API `Call.isWebexCallingOrWebexForBroadworks()` to denote if this call is Webex or Broadworks call.
- New API `Call.directTransferCall(toPhoneNumber: String, callback: CompletionHandler<DirectTransferResult>` to transfer the active call to the given number.
- New API `Call.switchToVideoCall(callId: String, callback: CompletionHandler<SwitchToAudioVideoCallResult>)` to switch the current Webex Calling call to video call.
- New API `Call.switchToAudioCall(callId: String, callback: CompletionHandler<SwitchToAudioVideoCallResult>)` to switch the current Webex Calling call to audio call.
- New API `CallHistoryRecord.isMissedCall()` to denotes if call record was a missed call.
- New API `Phone.processPushNotification(msg : String, handler: CompletionHandler<PushNotificationResult>)` to process the payload received in FCM service for Webex calling.
- New API `Phone.setPushTokens(bundleId : String, deviceId : String, voipToken : String)` to set params required for Push notifications of Webex calling.
- New API `Phone.getCallingType()` to get the type of Calling supported for logged in user.
- New API `Phone.buildNotificationPayload(notification: Map<String, String>, notificationId: String)` to build the payload from FCM notification.
- New API `Phone.connectPhoneServices(callback: CompletionHandler<PhoneConnectionResult>)` to login into phone services for CallingType.WebexCalling.
- New API `Phone.disconnectPhoneServices(callback: CompletionHandler<PhoneConnectionResult>)` to log out from phone services for CallingType.WebexCalling.
- New API `SpaceClient.isSpacesSyncCompleted()` to denote if space sync is completed or not.
- New Callback `SpaceClient.setOnInitialSpacesSyncCompletedListener(handler: CompletionHandler<Void>)` to set a listener to receive completion status for initial database sync.
- New Callback `SpaceClient.setOnSpaceSyncingStatusChangedListener(handler: CompletionHandler<Boolean>)` to set a listener to receive sync status for space changes.
- Added new Enum `UCLoginFailureReason` to indicate the failure reason for CUCM login or WebexCalling login
- Added new Enum `CallingType` to represent calling service type of logged-in user
- Added new Enum `PhoneConnectionResult` to indicate the result of call connection towards phone services.
- Added new Enum `SwitchToAudioVideoCallResult` to indicate the result of switching call, from audio to video  or vice versa.

#### Updated
- Maven arifact id changed from `androidsdk` to `webexsdk`
- FIXED: Space load issue and app crash on first install of KitchenSink.

#### Deprecated
- Deprecated API `setCUCMCredential(username: String, password: String)` instead use `setCallServiceCredential(username: String, password: String)`

## [3.7.0](https://github.com/webex/webex-android-sdk/releases/tag/3.7.0)
Released on **30 September, 2022**.
#### Added
- New case `forbidden` in enum `CreatePersonError`, `UpdatePersonError`
- Three new cases `INVALID_PASSWORD`, `CAPTCHA_REQUIRED`, `INVALID_PASSWORD_WITH_CAPTCHA` to enum `ErrorCode`
- New interface `Captcha` to represent the Captcha object.
- New interface `Breakout` A data type to represent the breakout.
- New interface `BreakoutSession` A data type to represent the breakout session.
- New API `Phone.isRestrictedNetwork(): Boolean` to check whether the device is in a restricted network.
- New API `Space.isExternallyOwned(): Boolean` to check whether space is owned by external org.
- New API `Phone.enableStreams(enable : Boolean)` to enable or disable all media streams on active calls.
- New API `Call.getCorrelationId(): String` to get the correlationId for that particular call.
- New API `MediaOption.setCaptchaId(captchaId: String)` to set unique id for the captcha.
- New API `MediaOption.getCaptchaId(): String` to get unique id for the captcha.
- New API `MediaOption.setCaptchaCode(captchaCode: String)` to set captcha verification code to be entered by user.
- New API `MediaOption.getCaptchaCode(): String` to get the captcha verification code.
- New callback `Call.joinBreakoutSession(session: BreakoutSession)` to join the Breakout Session manually by passing the `BreakoutSession` if host has enabled allow join session later.
- New callback `Call.returnToMainSession()` to return to main session.
- New callback `CallObserver.onBreakoutError(error: BreakoutSession.BreakoutSessionError)` to notify when any breakout api returns error.
- New callback `CallObserver.onBreakoutUpdated(breakout: Breakout)` to notify when Breakout is updated.
- New callback `CallObserver.onBroadcastMessageReceivedFromHost(message: String)` to notify when host broadcast the message to the session.
- New callback `CallObserver.onHostAskingReturnToMainSession()` to notify when host is asking participants to return to main meeting.
- New callback `CallObserver.onJoinableSessionUpdated(breakoutSessions: List<BreakoutSession>)` to notify when list of join breakout session changes.
- New callback `CallObserver.onJoinedSessionUpdated(breakoutSession: BreakoutSession)` to notify when joined Breakout session is updated.
- New callback `CallObserver.onReturnedToMainSession()` to notify when returned to main session.
- New callback `CallObserver.onSessionClosing()` to notify when Breakout session is closing.
- New callback `CallObserver.onSessionEnabled()` to notify when Breakout session is enabled.
- New callback `CallObserver.onSessionJoined(breakoutSession: BreakoutSession)` to notify when Breakout session is joined.
- New callback `CallObserver.onSessionStarted(breakout: Breakout)` to notify when Breakout session is started.
- New callback `Phone.refreshMeetingCaptcha(handler: CompletionHandler<Captcha>)` to refresh the Captcha object to get a new Captcha code.
- New callback `setOnRestrictedNetworkStatusChange(handler: CompletionHandler<Boolean>)` to monitor restricted network status changes.

#### Updated
- `Roles`,`licenses` and `siteUrls` fields are added to the `Person` class.
- Fixed - Crash while doing multiparty calls in Android SDK 3.5
- Fixed - Grid view Auxstream video hanging when navigating to another screen and back.
---
## [3.6.0](https://github.com/webex/webex-android-sdk/releases/tag/3.6.0)
Released on **24 August, 2022**.
#### Added
- `call.setMediaStreamCategoryC(participantId: String, quality: MediaStreamQuality)` to pin the participant's stream with the specified params if it does not already exist. Otherwise, update the pinned participant's stream with the specified params.
- `call.removeMediaStreamCategoryC(participantId: String)` to remove the pinning of a participant's stream.
- `Webex.startUCServices()` to start login process of CUCM
- `Webex.retryUCSSOLogin()` in case UC sso login expires or requires a retry.
- `Webex.forceRegisterPhoneServices()` to handle `RegisteredElsewhere` error.
- New class `CallHistoryRecord`.
- `webex.ucCancelSSOLogin()` which cancels currents SSO authentication for CUCM login.
- New callback `showUCSSOBrowser()` to notify when user needs to show SSO Browser.
- New callback `hideUCSSOBrowser()` to notify when user needs to hide SSO Browser.
- New callback `onUCSSOLoginFailed(failureReason: UCSSOFailureReason)` to notify app when SSO login fails.
- New Enum `UCSSOFailureReason` to indicate CUCM SSO Login failure reason.
#### Updated
- Fixed - Added Support for message with video and thumbnail
- Fixed - Post message api was returning the message object with mentions as empty array
- Fixed - List message api bug fixes to return correct data before a provided date or id and honouring max values
- Fixed - CUCM login for SSO authentication
- Fixed - CUCM call history
- Renamed callback `showUCSSOLoginView(ssoUrl: String)` to `loadUCSSOViewInBackground(ssoUrl: String)`
- `Phone.getCallHistory()` to return `CallHistoryRecord`s instead of `Spaces`

---
## [3.5.0](https://github.com/webex/webex-android-sdk/releases/tag/3.5.0)
Released on **7 June, 2022**.

#### Added
- `MessageObserver.MessageUpdated` event to get events when some existing messages are updated. 
- `Call.getMediaStreams(): List<MediaStream>` to get all opened media streams
- `Call.setMediaStreamCategoryA(duplicate: Boolean, quality: MediaStreamQuality)` to add the Active Speaker stream with the specified params if it does not already exist. Otherwise, update the Active Speaker stream with the specified params.
- `Call.setMediaStreamsCategoryB(numStreams: Int, quality: MediaStreamQuality)` to set the Category-B streams to specified parameters
- `Call.removeMediaStreamCategoryA()` to remove the active speaker stream
- `Call.removeMediaStreamsCategoryB()` to remove the Category-B streams
- `CallObserver.MediaStreamAvailabilityEvent` event to know when a media stream is available or unavailable
- `MediaStream` which represents a media stream instance
- `MediaStreamType` enum to represent the type of media stream
- `MediaStreamQuality` enum to represent the quality of a media stream
- `MediaStreamChangeEventType` enum to represent the change event type of a media stream.
- `MediaStreamChangeEventInfo` which represents media stream change event information.

#### Updated
- Fixed - Virtual Background showed wrong orientation when portrait images were used to add VBG
- Fixed - Virtual background was not user specific i.e. after logout another user can see previously selected backgrounds
- Fixed - `MessageClient.postToPerson` api issue when JWT user sends a message to a person with whom they have never interacted before.
- Support for 1080p video resolutions

---
## [3.4.0](https://github.com/webex/webex-android-sdk/releases/tag/3.4.0)
Released on **19 April, 2022**.

#### Added
- New enum `Call.MediaQualityInfo` to denote media quality
- New observer `CallObserver.onMediaQualityInfoChanged` to get  media quality events
- `Text.html(html: String): Text` to send html text
- `Text.markdown(markdown: String): Text` to send markdown text

#### Updated
- Fixed - Crash when remote user starts or stops sharing
- Fixed - Call pipeline improvement
- Fixed - List messages before messageId not returning messages
- Fixed - Text object type incorrect on received messages
- Fixed - Message sender details incorrect in integration usecase


#### Deprecated
- Sending multiple formats of text in the same message is not supported. Below text constuctors are deprecated
    - `Text.markdown(markdown: String, html: String?, plain: String?): Text`
    - `Text.html(html: String, plain: String?): Text`

---
## [3.3.0](https://github.com/webex/webex-android-sdk/releases/tag/3.3.0)
Released on **15 February, 2022**.

#### Added
- `Call.setCameraFocusAtPoint(pointX: Float, pointY: Float): Boolean` to set the camera focus at given coordinate.
- `Call.setCameraFlashMode(mode: FlashMode): Boolean` to set the camera flash mode.
- `Call.getCameraFlashMode(): FlashMode` to get the camera flash mode.
- `Call.setCameraTorchMode(mode: TorchMode): Boolean` to set the camera Torch mode.
- `Call.getCameraTorchMode(): TorchMode` to get the camera Torch mode.
- `Call.getCameraExposureDuration(): CameraExposureDuration` to get the exposure duration of the camera.
- `Call.getCameraExposureISO(): CameraExposureISO` to get the exposure ISO of the camera.
- `Call.getCameraExposureTargetBias(): CameraExposureTargetBias` to get the exposure target bias of the camera.
- `Call.setCameraCustomExposure(duration: Double, iso: Float): Boolean` to set the camera custom exposure value using camera exposure duration and ISO.
- `Call.setCameraAutoExposure(targetBias: Float): Boolean` to set the camera auto exposure value using camera exposure target bias.
- `Call.setVideoZoomFactor(factor: Float): Boolean` to set the Zoom IN/OUT factor for the local camera.
- `Call.getVideoZoomFactor(): Float` to get the local camera zoom factor.
- `Call.takePhoto(): Boolean` to takes a snapshot of the local video view.
- `CallObserver.onPhotoCaptured(imageData: ByteArray?)`  to notify app whenever a photo is captured.
- `Call.getWXA(): WXA` for Webex assistant and real time transcription controls.
- `Interface WXA` A data type to represent the WebEx Assistant.
- `class Transcription` A data type to represent a transcription object from the WebEx Assistant.

#### Updated
- Fixed - Camera Orientation with respect to device orientation.
- Fixed - Thumbnail for high resolution images not loading.
- Fixed - Decoding of special characters in urlencoded Guest issuer JWT token.
- Fixed - Made `exp` field as optional in Guest Issuer JWT
- Fixed - Callback not being fired for deleting self membership from space.
- Fixed - Fetching inter-cluster team memberships.
- Fixed - Virtual Background Image orientation.
- Fixed - DTMF API usage.

---
## [3.2.1](https://github.com/webex/webex-android-sdk/releases/tag/3.2.1)
Released on **30 November, 2021**.

#### Added
- `Call.forceSendingVideoLandscape(forceLandscape: Boolean, callback: CompletionHandler<Void>?)` to force landscape video transfer of local video view.
- `Call.getLocusURL()` returns the locus url of the call.

#### Updated
- Fixed - Video resume issue when phone unlocks.
- Fixed - Space title issue for first time login.
- Fixed - setReceivingAudio() API issue when remote participant is muted.
- Fixed - CUCM login callback issue for OAuthAuthenticator.
- Fixed - Crash fix when switching between meetings or space call

---
## [3.2.0](https://github.com/webex/webex-android-sdk/releases/tag/3.2.0)
Released on **25 October, 2021**.

#### Added
- `CalendarMeetingClient.list(fromDate : Date?, toDate : Date?, handler: CompletionHandler<List<CalendarMeeting>>)` - To get calendar meetings.
- `CalendarMeetingClient.getById(meetingId : String, handler: CompletionHandler<CalendarMeeting>)` - To get a calendar meeting by id.
- `CalendarMeetingClient.setObserver(observer: CalendarMeetingObserver?)` - To listen to calendar events (Added, Updated, Removed)
- Custom token authentication
- `TokenAuthenticator.authorize(accessToken: String, expiryInSeconds: Int?, handler: CompletionHandler<Void>)` - To set a custom token
- `TokenAuthenticator.setOnTokenExpiredListener(callback: CompletionHandler<Void>)` - Callback triggered when token has expired
- Custom/Blur Background for calls
- `Phone.isVirtualBackgroundSupported()` to check if virtual background is supported
- `Phone.fetchVirtualBackgrounds(handler: CompletionHandler<List<VirtualBackground>>)` to list virtual backgrounds
- `Phone.addVirtualBackground(image: LocalFile, handler: CompletionHandler<VirtualBackground>)` to add a virtual background
- `Phone.removeVirtualBackground(backgroundItem: VirtualBackground, handler: CompletionHandler<Boolean>)` to remove a virtual background
- `Phone.applyVirtualBackground(backgroundItem: VirtualBackground, mode:  VirtualBackgroundMode, handler: CompletionHandler<Boolean>)` to apply a virtual backround
- `Phone.setMaxVirtualBackgroundItems(limit: Int)` to limit the number of custom virtual backgrounds
- `Phone.getMaxVirtualBackgroundItems(): Int` to get the limit of number of custom virtual backgrounds
- `CallObserver.onCpuHitThreshold()` - Callback to notify developer when CPU threshold is reached

#### Resolved Bugs
- Dial callback not received.
- Meeting Signal after restart inconsistency.
- Calling Screen Infinite loading - wrong meeting Id dial.
- Re-login crash without restart of application.
- Meeting subject incorrect .
- Remote Video rendering issue when re-join meeting.
- Local video stopped after ending whatsapp/hangout call.
- Video surfaces crash on leaving meeting.
- HW Acceleration video resolution 720p.


---
## [3.1.0](https://github.com/webex/webex-android-sdk/releases/tag/3.1.0)
Released on **16 August, 2021**.

#### Added
- `Phone.getServiceUrl(serviceUrlType: ServiceUrlType)` - to expose service Urls.
- OAuthAuthenticator.
- `getAuthorizationUrl(handler: CompletionHandler<String?>)` in OAuthWebViewAuthenticator.
- `Call.isSpaceMeeting` to check space backed meeting type.
- `Call.isSelfCreator` to check self is the initiator of the call.
- `Call.hasAnyoneJoined` to check anyone joined the meeting, excluding self.
- `Call.isPmr` to check meeting is in PMR (personal meeting room).
- `Call.isMeeting` to check the call is meeting type.
- `Call.isScheduledMeeting` to check the call is schedule meeting type.
- FedRAMP app configuration support.

#### Updated
- `OAuthWebViewAuthenticator` takes scope as a constructor parameter

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
