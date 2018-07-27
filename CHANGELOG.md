# Change Log
All notable changes to this project will be documented in this file.

#### 1.3.0 Releases
- `1.3.0` Releases - [1.3.0](#130)

#### 0.2.0 Releases

- `0.2.0` Releases - [0.2.0](#020)

---
## [1.3.0](https://github.com/webex/webex-android-sdk/releases/tag/1.3.0)
Released on 2018-01-12.

#### Added
- Receive and display content-sharing stream
- Support space calling/multi-party calling
- Support Single-Sign-On authentication
- Set the maximum bandwidth for Audio/Video/Content Sharing

#### Updated
- Fixed always receiving incoming space call even if there is nobody in the meeting space
- Fixed unstable call state caused by race condition in call control events
- Fixed random crash when logout
- Updated the license by adding a term for H264 codec, and adding a new license file for "Cisco API" used in the project.

#### Removed
The following exclude is no longer needed in the packagingOptions (unless RxJava2 or its related library is involved in developers's app):

    packagingOptions {
        exclude 'META-INF/rxjava.properties'
    }

## [0.2.0](https://github.com/webex/webex-android-sdk/releases/tag/0.2.0)
Released on 2017-11-30.

#### Added
- Initial release of Cisco Webex SDK for Android.
