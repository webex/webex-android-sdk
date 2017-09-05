# spark-android-sdk

Spark SDK for android.

## Usage

### 1. Add the following repository to your Top-level build.gradle file

```
allprojects {
    repositories {
        jcenter()
        maven {
            url 'http://engci-maven.cisco.com/artifactory/webex-cca-thirdparty'
        }
        maven {
            url 'http://engci-maven.cisco.com/artifactory/webex-cca-release'
        }
        maven {
            url 'http://engci-maven.cisco.com/artifactory/webex-cca-group'
        }
        maven {
            url 'http://engci-maven.cisco.com/artifactory/androidwb-group/'
        }
        maven {
            url 'http://10.140.253.42:10081/nexus/content/repositories/thirdparty/'
        }
    }
}
```

### 2. Add spark-android-sdk library in your App build.gradle dependency

```
dependencies { compile 'com.ciscospark:android-sdk:0.0.1' }
```

### 3. Enable multiDex in your App

```
android {
    defaultConfig {
        multiDexEnabled true
    }
}
```

### 4. Make sure your minSdkVersion is up to 21, Android 5.0

```
android {
    defaultConfig {
        minSdkVersion 21
    }
}
```

### 5. User SparkApplication

There're two ways of using SparkApplication

1. Direct assign in AndroidManifest.xml

```
<application
        android:name="com.ciscospark.core.SparkApplication"
        android:theme="@style/AppTheme">
        <activity android:name=".MyActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
</application>
```

2. Inherit SparkApplication

Create a java class extends SparkApplication

```
import com.ciscospark.core.SparkApplication;

public class MyApplication extends SparkApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // your code here
    }

}
```

Assign it in AndroidManifest.xml

```
<application
        android:name=".MyApplication"
        android:theme="@style/AppTheme">
</application>
```



## Build
