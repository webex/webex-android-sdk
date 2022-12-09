# Android Meeting SDK
AAR dependency can be downloaded from "Releases TAGS: 3.8.0-alpha" in right pane OR from [here](https://github.com/webex/webex-android-sdk/releases/tag/3.8.0-alpha)
## AAR Details

This branch covers the releases for **Meeting SDK**  
- This SDK supports Messaging and Meeting features
- It does not support CUCM Calling or Webex Calling
- More details about all features can be found [here](https://developer.webex.com/docs/sdks/android)

### Storage comparison:

||**3.7.0-Release** |**Meeting SDK** |
| :- | - | - |
|Storage snapshot |<img width="705" alt="Screenshot 2022-11-28 at 8 41 45 PM" src="https://user-images.githubusercontent.com/119413473/204521375-ca9b565c-22c0-4868-b023-306666a9b642.png">|<img width="708" alt="Screenshot 2022-11-28 at 8 36 34 PM" src="https://user-images.githubusercontent.com/119413473/204521557-79f32ebe-c886-40e7-a654-03f517527767.png">
|<p>Size on disk </p><p>(Universal) </p>|**214 MB** |**156 MB** |
|<p>Raw </p><p>Size          </p><p>(Universal) </p>|**212.8 MB** |**155.3 MB** |


|**Detailed AAR comparison** |
| - |
|<img width="1249" alt="Screenshot 2022-11-28 at 4 47 34 PM" src="https://user-images.githubusercontent.com/119413473/204522234-1f6db6dd-98b4-417c-bf6b-529fa81b773e.png">|

## When included in APP(APK details) 

**Meeting SDK** is tested with a sample application (without calling features), and we found **NO** impact on core features of SDK.
### How to add in APP
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
   implementation files('libs/WebexSDK-Meeting.aar')
   ```
### Storage analysis for apk
Below details can be used for reference (in the terms of storage), to see the impact of our new **Meeting SDK** on APP. 
||**Approx. Sizes (in MB)** |
| :- | - |
|APP bundle size (aab) |<img width="967" alt="Screenshot 2022-11-28 at 8 50 14 PM" src="https://user-images.githubusercontent.com/119413473/204522882-8586a662-b05c-41b6-b768-ad3ee623cbcd.png"><p>**158 MB** </p>|
|Universal apk size |<img width="962" alt="Screenshot 2022-11-28 at 8 52 09 PM" src="https://user-images.githubusercontent.com/119413473/204523253-0feb87f2-cf6d-4491-ab20-a2fa1e06c330.png"><p>**160 MB** </p>|
|arm64 apk size |<img width="965" alt="Screenshot 2022-11-28 at 8 57 01 PM" src="https://user-images.githubusercontent.com/119413473/204523366-d2e8d9e3-24c7-418c-95ad-5fa2b122fd96.png"><p>**43.5 MB** </p>|
|arm64 installation size |<img width="107" alt="image" src="https://user-images.githubusercontent.com/119413473/204523480-e0f179e7-0b52-4e6c-90c8-cea85953f1e1.png"><p>**160 MB** </p>|

## Documentation
- [Requirements & Feature List](https://developer.webex.com/docs/sdks/android)
- [Guides](https://github.com/webex/webex-android-sdk/wiki)
- [API Reference](https://webex.github.io/webex-android-sdk/)
- [Kitchen Sink Sample App](https://github.com/webex/webex-android-sdk-example)

## Support
- [Webex Developer Support ](https://developer.webex.com/support)
- Email: devsupport@webex.com

## License

All contents are licensed under the Cisco EULA

See [License](LICENSE) for details.
