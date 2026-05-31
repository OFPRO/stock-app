---
name: android-tooling
description: Set up and configure Android development environment — JDK, SDK, Gradle, ADB, emulators, and platform tools. Use when starting a new Android project or debugging toolchain issues.
---

# Android Tooling

## When to Use This Skill
- Setting up a new Android development environment from scratch
- Debugging Gradle, SDK, or JDK compatibility issues
- Configuring build variants, product flavors, or signing configs
- Setting up CI/CD tooling for Android builds
- Managing emulators, AVDs, and physical device connections

## Toolchain Overview
```
JDK 21          → Java compilation (required by AGP 8.x)
Android SDK     → Platforms + Build-tools + Platform-tools
Gradle + AGP    → Build system + Android Gradle Plugin
ADB             → Device/emulator communication
sdkmanager      → SDK component manager
avdmanager      → Android Virtual Device manager
```

## Environment Setup

### Verify Installation
```bash
java -version                          # JDK 21+
adb --version                          # Android Debug Bridge
sdkmanager --list --sdk_root=$ANDROID_HOME
adb devices -l                         # List connected devices/emulators
```

### SDK Components Required
```bash
sdkmanager "platforms;android-35" \
           "build-tools;35.0.0" \
           "platform-tools" \
           "emulator" \
           "system-images;android-35;google_apis;arm64-v8a"
```

### Create an Emulator (AVD)
```bash
avdmanager create avd -n Pixel_9_API_35 \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d pixel_9
emulator -avd Pixel_9_API_35 -no-snapshot -wipe-data
```

### Build Variants
```groovy
android {
  flavorDimensions "environment"
  productFlavors {
    dev  { dimension "environment"; applicationIdSuffix ".dev" }
    prod { dimension "environment" }
  }
  buildTypes {
    debug    { debuggable true }
    release  { minifyEnabled true; proguardFiles getDefaultProguardFile('proguard-android-optimize.txt') }
  }
}
```

### Gradle Tasks (Essential)
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release AAB/APK
./gradlew bundleDebug            # Build debug AAB
./gradlew test                   # Run unit tests
./gradlew connectedDebugAndroidTest  # Run instrumentation tests
./gradlew lint                   # Static analysis
./gradlew :app:dependencies      # Print dependency tree
```

## Debugging Common Issues

### Gradle / AGP Compatibility
| AGP Version | Min JDK | Max JDK | Gradle Min |
|-------------|---------|---------|------------|
| 8.5.x       | 17      | 21      | 8.7        |
| 8.4.x       | 17      | 21      | 8.6        |
| 8.3.x       | 17      | 20      | 8.4        |

### ADB Troubleshooting
```bash
adb kill-server && adb start-server  # Restart ADB
adb reboot                           # Reboot device
adb logcat -c                        # Clear logs
adb logcat *:W                       # Show only warnings+
adb shell dumpsys battery unplug     # Simulate unplugged
```

### Memory & Performance
```bash
./gradlew --build-cache --parallel --configure-on-demand assembleDebug
./gradlew build --scan                    # Build performance report
```
